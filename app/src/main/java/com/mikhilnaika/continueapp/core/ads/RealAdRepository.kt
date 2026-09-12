package com.mikhilnaika.continueapp.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.mikhilnaika.continueapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private class AdMobLoadedAd(val rewardedAd: RewardedAd) : LoadedAd

/**
 * Real AdMob-backed implementation.
 *
 * Ad unit IDs come from `BuildConfig`, defaulting to Google's official public **test** units.
 * Real units are created but can't fill ads until the app is live on Play, so test units are
 * the correct choice today — and switching is a `local.properties` edit
 * (`ADMOB_UNIT_COIN` / `ADMOB_UNIT_FREE_PLAY`), not a code change.
 *
 * Test units cannot do server-side verification, so the reward is granted from the SDK's
 * `onUserEarnedReward` callback. Once real units are live, call `enableRewardVerification()`
 * and gate on the verified server callback instead — see
 * [com.mikhilnaika.continueapp.core.data.CoinLedger] for the balance side of that swap.
 */
@Singleton
class RealAdRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentManager: ConsentManager,
) : AdRepository {

    override suspend fun loadCoinAd(): LoadedAd? = loadRewarded(BuildConfig.ADMOB_UNIT_COIN)

    override suspend fun loadFreePlayAd(): LoadedAd? = loadRewarded(BuildConfig.ADMOB_UNIT_FREE_PLAY)

    /**
     * The consent gate, and the only one.
     *
     * It sits here rather than at the two call sites (DRAW's coin earn and the FREE PLAY
     * gate) because a rule enforced at call sites is enforced only at the call sites somebody
     * remembered — the lesson this codebase learned from a HAPTICS toggle that controlled
     * nothing for weeks. A third ad surface added later inherits this automatically; it cannot
     * forget to ask.
     *
     * Returning null on refusal is already a state every caller handles, since it is what a
     * no-fill looks like — and a no-fill is exactly what an un-consented ad request *is* from
     * the user's point of view: no ad, no coin, nothing else different.
     */
    private suspend fun loadRewarded(adUnitId: String): LoadedAd? {
        if (!consentManager.canRequestAds()) return null
        return loadRewardedUnchecked(adUnitId)
    }

    private suspend fun loadRewardedUnchecked(adUnitId: String): LoadedAd? = suspendCancellableCoroutine { cont ->
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    cont.resume(AdMobLoadedAd(ad))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    cont.resume(null)
                }
            },
        )
    }

    override suspend fun show(activity: Activity, ad: LoadedAd): AdResult {
        val rewardedAd = (ad as? AdMobLoadedAd)?.rewardedAd ?: return AdResult.NoFill
        return suspendCancellableCoroutine { cont ->
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (cont.isActive) cont.resume(AdResult.Error(error.message))
                }
            }
            rewardedAd.show(activity) { _ ->
                // No-fill/error already resolved via fullScreenContentCallback if it fires
                // first; this only fires on a genuine earn.
                if (cont.isActive) cont.resume(AdResult.Granted)
            }
        }
    }

}
