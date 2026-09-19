package com.mikhilnaika.continueapp.core.ads

import android.app.Activity

/** What a verified ad paid out, as RevenueCat reports it. */
sealed interface AdReward {
    /** RevenueCat has already added [amount] of [code] to the user's server-side balance. */
    data class Currency(val code: String, val amount: Int) : AdReward

    /** RevenueCat has already granted [identifier] until [expiresAtMillis]. */
    data class Entitlement(val identifier: String, val expiresAtMillis: Long?) : AdReward
}

sealed interface AdResult {
    /**
     * The ad was watched to the end and RevenueCat confirmed it: AdMob called RevenueCat's
     * server-side-verification webhook, RevenueCat matched it to this impression's token and
     * applied the reward rule configured in the dashboard. [rewards] is what that rule granted.
     */
    data class Verified(val rewards: List<AdReward>) : AdResult

    /**
     * The ad was watched to the end, but no verified result arrived in time — offline, AdMob's
     * callback running late, or no reward rule configured for the unit. The user *did* watch
     * it; what that's worth is the caller's decision (see `DrawViewModel.insertCoin`).
     */
    data object Unverified : AdResult

    data object NoFill : AdResult
    data object UserCancelled : AdResult
    data class Error(val message: String) : AdResult
}

/** Opaque handle so callers don't depend on the AdMob SDK type directly. */
interface LoadedAd

/** Which rewarded unit, and therefore which RevenueCat reward rule, an ad belongs to. */
enum class AdPlacement(val trackingName: String) {
    /** Unit A → reward rule: 1 COIN. The INSERT COIN button on the CONTINUE? screen. */
    COIN("continue_insert_coin"),

    /** Unit B → reward rule: the `pro` entitlement for 60 minutes. FREE PLAY. */
    FREE_PLAY("free_play"),
}

/**
 * AdMob, with RevenueCat as the verification and reporting layer — docs/04-MONETIZATION.md
 * §Layer 1. [FakeAdRepository] stands in for debug builds.
 *
 * Two units exist because a RevenueCat reward rule can carry at most one currency reward per
 * ad unit: one unit pays coins, the other pays temporary PRO.
 */
interface AdRepository {
    suspend fun load(placement: AdPlacement): LoadedAd?
    suspend fun show(activity: Activity, ad: LoadedAd): AdResult
}
