package com.mikhilnaika.continueapp.core.ads

import android.app.Activity
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import javax.inject.Inject

/** How a FREE PLAY attempt ended, already phrased for the screen that offered it. */
sealed interface FreePlayOutcome {
    data object Started : FreePlayOutcome
    data class Failed(val message: String) : FreePlayOutcome
}

/**
 * FREE PLAY — one rewarded ad buys sixty minutes of real PRO (docs/04-MONETIZATION.md,
 * "the differentiator"). Offered from the CONTINUE? screen and from GO PRO.
 *
 * RevenueCat grants the genuine `pro` entitlement, time-limited, as the verified reward of the
 * FREE PLAY ad unit. There is deliberately no client-side fallback, unlike INSERT COIN: an
 * entitlement is RevenueCat's to grant, so an ad that couldn't be verified is reported honestly
 * rather than faked into a PRO the server doesn't know about.
 *
 * One class so the two entry points can't drift into two different sets of rules.
 */
class FreePlay @Inject constructor(
    private val adRepository: AdRepository,
    private val billingRepository: BillingRepository,
) {
    /** [onStatus] receives what to show under the spinner as the attempt progresses. */
    suspend fun start(activity: Activity, onStatus: (String) -> Unit): FreePlayOutcome {
        onStatus("LOADING AD…")
        val ad = adRepository.load(AdPlacement.FREE_PLAY)
            ?: return FreePlayOutcome.Failed("No ad available right now — try again shortly.")
        val result = adRepository.show(activity, ad)
        onStatus("VERIFYING WITH REVENUECAT…")
        return when (result) {
            is AdResult.Verified -> {
                // RevenueCat has already refreshed customer info by now; re-reading is belt and
                // braces, and it's what starts the local expiry timer immediately.
                billingRepository.refreshEntitlements()
                if (billingRepository.isPro.value) {
                    FreePlayOutcome.Started
                } else {
                    FreePlayOutcome.Failed("Ad verified, but FREE PLAY didn't start. Try again in a moment.")
                }
            }
            is AdResult.Unverified ->
                FreePlayOutcome.Failed("Couldn't verify that ad, so FREE PLAY didn't start. Try again in a moment.")
            is AdResult.NoFill -> FreePlayOutcome.Failed("No ad available right now — try again shortly.")
            is AdResult.UserCancelled -> FreePlayOutcome.Failed("Closed early — FREE PLAY needs the whole ad.")
            is AdResult.Error -> FreePlayOutcome.Failed(result.message)
        }
    }

    companion object {
        /** What RevenueCat's reward rule grants, and therefore what every screen promises. */
        const val MINUTES = 60
    }
}
