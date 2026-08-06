package com.mikhilnaika.continueapp.core.ads

import android.app.Activity

sealed interface AdResult {
    data object Granted : AdResult
    data object NoFill : AdResult
    data object UserCancelled : AdResult
    data class Error(val message: String) : AdResult
}

/** Opaque handle so callers don't depend on the AdMob SDK type directly. */
interface LoadedAd

/**
 * AdMob wrapper — docs/05-TECH-ARCHITECTURE.md §AdMob integration shape. No real ad units
 * exist yet (docs/09-PENDING-INPUTS.md); [FakeAdRepository] simulates a reward so the
 * ad→coin and ad→FREE PLAY loops can be built and demoed before AdMob approval lands.
 * Never grant client-side in the real implementation — the verified callback is the only
 * source of truth.
 */
interface AdRepository {
    suspend fun loadCoinAd(): LoadedAd?
    suspend fun loadFreePlayAd(): LoadedAd?
    suspend fun show(activity: Activity, ad: LoadedAd): AdResult
}
