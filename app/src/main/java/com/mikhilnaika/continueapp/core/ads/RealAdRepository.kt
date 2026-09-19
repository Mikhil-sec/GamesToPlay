package com.mikhilnaika.continueapp.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.mikhilnaika.continueapp.BuildConfig
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import com.revenuecat.purchases.ads.rewardverification.VerifiedReward
import com.revenuecat.purchases.awaitPollRewardVerification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private class AdMobLoadedAd(
    val rewardedAd: RewardedAd,
    val placement: AdPlacement,
    val adUnitId: String,
    /** Null when RevenueCat couldn't issue a token; the ad still plays, it just can't verify. */
    val clientTransactionId: String?,
) : LoadedAd

/**
 * AdMob serves the ads; **RevenueCat verifies the rewards and records the revenue** —
 * docs/04-MONETIZATION.md §Layer 1.
 *
 * **Verification.** Before an ad is shown, RevenueCat issues a token for that exact impression
 * and it rides along as AdMob's server-side-verification custom data. When the viewer earns the
 * reward, AdMob calls RevenueCat's SSV webhook (configured per ad unit in the AdMob console),
 * RevenueCat checks it and applies the unit's reward rule from the dashboard — 1 COIN, or the
 * `pro` entitlement for 60 minutes — and the app polls for that verified result. The reward is
 * granted by RevenueCat's server, never by this client; the app only reads the outcome.
 *
 * **Tracking.** Load, impression, click and paid events go to RevenueCat's ad tracker, so ad
 * revenue lands in the same dashboard, per customer, as subscription revenue. That is the point
 * of the three-layer design: one LTV per player whether they paid with money or with attention.
 *
 * Both APIs are RevenueCat's experimental preview surface in `purchases` 10.12, hence the opt-in.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Singleton
class RealAdRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentManager: ConsentManager,
) : AdRepository {

    private val tracker get() = Purchases.sharedInstance.adTracker

    /**
     * The consent gate, and the only one.
     *
     * It sits here rather than at the call sites because a rule enforced at call sites is
     * enforced only at the call sites somebody remembered — the lesson this codebase learned
     * from a HAPTICS toggle that controlled nothing for weeks. Every ad surface, present and
     * future, comes through this method.
     *
     * Returning null on refusal is already a state every caller handles, since it is what a
     * no-fill looks like — and a no-fill is exactly what an un-consented ad request *is* from
     * the user's point of view: no ad, no reward, nothing else different.
     */
    override suspend fun load(placement: AdPlacement): LoadedAd? {
        if (!consentManager.canRequestAds()) return null
        val adUnitId = when (placement) {
            AdPlacement.COIN -> BuildConfig.ADMOB_UNIT_COIN
            AdPlacement.FREE_PLAY -> BuildConfig.ADMOB_UNIT_FREE_PLAY
        }
        return loadUnchecked(placement, adUnitId)
    }

    private suspend fun loadUnchecked(placement: AdPlacement, adUnitId: String): LoadedAd? =
        suspendCancellableCoroutine { cont ->
            RewardedAd.load(
                context,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        val impressionId = ad.responseInfo.responseId.orEmpty()
                        // The token binds this impression to this RevenueCat customer. Without
                        // it the ad still plays; the reward simply can't be verified.
                        val token = runCatching {
                            Purchases.sharedInstance.generateRewardVerificationToken(impressionId)
                        }.getOrNull()
                        if (token != null) {
                            ad.setServerSideVerificationOptions(
                                ServerSideVerificationOptions.Builder()
                                    .setUserId(token.appUserID)
                                    .setCustomData(token.customData)
                                    .build(),
                            )
                        }
                        ad.setOnPaidEventListener { value -> trackRevenue(ad, placement, adUnitId, value) }
                        track {
                            tracker.trackAdLoaded(
                                AdLoadedData(
                                    networkName = ad.networkName(),
                                    mediatorName = AdMediatorName.AD_MOB,
                                    adFormat = AdFormat.REWARDED,
                                    placement = placement.trackingName,
                                    adUnitId = adUnitId,
                                    impressionId = impressionId,
                                ),
                            )
                        }
                        cont.resume(AdMobLoadedAd(ad, placement, adUnitId, token?.clientTransactionId))
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        track {
                            tracker.trackAdFailedToLoad(
                                AdFailedToLoadData(
                                    mediatorName = AdMediatorName.AD_MOB,
                                    adFormat = AdFormat.REWARDED,
                                    placement = placement.trackingName,
                                    adUnitId = adUnitId,
                                    mediatorErrorCode = error.code,
                                ),
                            )
                        }
                        cont.resume(null)
                    }
                },
            )
        }

    override suspend fun show(activity: Activity, ad: LoadedAd): AdResult {
        val loaded = ad as? AdMobLoadedAd ?: return AdResult.NoFill
        val rewardedAd = loaded.rewardedAd
        val impressionId = rewardedAd.responseInfo.responseId.orEmpty()
        val earned = CompletableDeferred<Boolean>()

        // Suspends until the ad is *closed*, not until the reward fires. That used to be the
        // other way round, and closing an ad early resumed nothing at all — the CONTINUE?
        // screen sat on its spinner for good. Dismissal is now always an answer.
        val failure: String? = suspendCancellableCoroutine { cont ->
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdImpression() = track {
                    tracker.trackAdDisplayed(
                        AdDisplayedData(
                            networkName = rewardedAd.networkName(),
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.REWARDED,
                            placement = loaded.placement.trackingName,
                            adUnitId = loaded.adUnitId,
                            impressionId = impressionId,
                        ),
                    )
                }

                override fun onAdClicked() = track {
                    tracker.trackAdOpened(
                        AdOpenedData(
                            networkName = rewardedAd.networkName(),
                            mediatorName = AdMediatorName.AD_MOB,
                            adFormat = AdFormat.REWARDED,
                            placement = loaded.placement.trackingName,
                            adUnitId = loaded.adUnitId,
                            impressionId = impressionId,
                        ),
                    )
                }

                override fun onAdDismissedFullScreenContent() {
                    earned.complete(false) // no-op if the reward already fired
                    if (cont.isActive) cont.resume(null)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    earned.complete(false)
                    if (cont.isActive) cont.resume(error.message)
                }
            }
            rewardedAd.show(activity) { earned.complete(true) }
        }

        if (failure != null) return AdResult.Error(failure)
        if (!earned.await()) return AdResult.UserCancelled
        val transactionId = loaded.clientTransactionId ?: return AdResult.Unverified

        // AdMob fires the SSV callback the moment the reward is earned, so by the time the ad
        // is closed it has usually landed. The cap is for when it hasn't: the user is looking at
        // a VERIFYING… spinner, and a verification that takes longer than this isn't coming.
        val result = withTimeoutOrNull(VERIFY_TIMEOUT_MS) {
            runCatching { Purchases.sharedInstance.awaitPollRewardVerification(transactionId) }.getOrNull()
        }
        if (result == null || result.failed) return AdResult.Unverified
        val rewards = (listOfNotNull(result.verifiedReward) + result.moreRewards).mapNotNull { reward ->
            when (reward) {
                is VerifiedReward.VirtualCurrency -> AdReward.Currency(reward.code, reward.amount)
                is VerifiedReward.Entitlement -> AdReward.Entitlement(reward.identifier, reward.expiresAt?.time)
                else -> null
            }
        }
        return if (rewards.isEmpty()) AdResult.Unverified else AdResult.Verified(rewards)
    }

    private fun trackRevenue(ad: RewardedAd, placement: AdPlacement, adUnitId: String, value: AdValue) = track {
        tracker.trackAdRevenue(
            AdRevenueData(
                networkName = ad.networkName(),
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = placement.trackingName,
                adUnitId = adUnitId,
                impressionId = ad.responseInfo.responseId.orEmpty(),
                revenueMicros = value.valueMicros,
                currency = value.currencyCode,
                precision = when (value.precisionType) {
                    AdValue.PrecisionType.PRECISE -> AdRevenuePrecision.EXACT
                    AdValue.PrecisionType.ESTIMATED -> AdRevenuePrecision.ESTIMATED
                    AdValue.PrecisionType.PUBLISHER_PROVIDED -> AdRevenuePrecision.PUBLISHER_DEFINED
                    else -> AdRevenuePrecision.UNKNOWN
                },
            ),
        )
    }

    private fun RewardedAd.networkName(): String? =
        responseInfo.loadedAdapterResponseInfo?.adSourceName

    /** Reporting must never be able to break an ad. A preview API that throws is ignored. */
    private inline fun track(block: () -> Unit) {
        runCatching(block)
    }

    private companion object {
        const val VERIFY_TIMEOUT_MS = 10_000L
    }
}
