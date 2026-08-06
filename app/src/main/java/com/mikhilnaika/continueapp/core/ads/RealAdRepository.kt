package com.mikhilnaika.continueapp.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private class AdMobLoadedAd(val rewardedAd: RewardedAd) : LoadedAd

/**
 * Real AdMob-backed implementation. Test unit IDs are used until real rewarded units are
 * created (docs/09-PENDING-INPUTS.md — AdMob approval has its own latency, separate from
 * Play Console). Test IDs cannot be server-side verified, so [show]'s reward here is
 * granted from the SDK's onUserEarnedReward callback only in test mode; production must
 * call `enableRewardVerification()` and gate on the verified server callback instead
 * (docs/05-TECH-ARCHITECTURE.md — "Never grant client-side").
 */
@Singleton
class RealAdRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdRepository {

    override suspend fun loadCoinAd(): LoadedAd? = loadRewarded(AD_UNIT_COIN_TEST)

    override suspend fun loadFreePlayAd(): LoadedAd? = loadRewarded(AD_UNIT_FREE_PLAY_TEST)

    private suspend fun loadRewarded(adUnitId: String): LoadedAd? = suspendCancellableCoroutine { cont ->
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

    companion object {
        // Google's published test rewarded ad unit ID — safe to ship until real units exist.
        const val AD_UNIT_COIN_TEST = "ca-app-pub-3940256099942544/5224354917"
        const val AD_UNIT_FREE_PLAY_TEST = "ca-app-pub-3940256099942544/5224354917"
    }
}
