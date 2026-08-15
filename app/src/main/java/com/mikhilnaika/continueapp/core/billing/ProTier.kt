package com.mikhilnaika.continueapp.core.billing

/**
 * One purchasable route to PRO, flattened out of RevenueCat's `Package`/`StoreProduct`/
 * `SubscriptionOption`/`PricingPhase` chain.
 *
 * Exists so `feature/paywall` never imports a RevenueCat type. Two concrete payoffs:
 *
 * - **The paywall is demoable in a debug build.** `FakeBillingRepository` can hand back
 *   convincing tiers, so the screen can be laid out, screenshotted for the submission kit, and
 *   reviewed without a Play Store connection. Against the real SDK a debug build shows nothing
 *   purchasable, which would have made the paywall impossible to iterate on.
 * - If the store layer is ever swapped, the design work doesn't move.
 *
 * [priceFormatted] is always the store's own localised string (`Price.formatted`) — never
 * built from an amount and a currency code here, because Google already knows how to write
 * prices for every locale we ship in and we don't.
 */
data class ProTier(
    /** The RevenueCat package identifier — `$rc_monthly`, `$rc_lifetime`. */
    val id: String,
    /** Short display name: "MONTHLY", "LIFETIME". */
    val label: String,
    /** Localised, store-formatted price. */
    val priceFormatted: String,
    /** "per month" for a subscription; null for a one-time purchase. */
    val cadence: String?,
    /** Free-trial length in days, or null if the tier has no introductory free phase. */
    val freeTrialDays: Int?,
    /** One-time purchase — drives the "BEST VALUE" treatment and the copy that isn't a renewal. */
    val isLifetime: Boolean,
)
