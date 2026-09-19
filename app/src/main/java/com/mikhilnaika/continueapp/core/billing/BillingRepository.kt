package com.mikhilnaika.continueapp.core.billing

import android.app.Activity
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object UserCancelled : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}

sealed interface SpendResult {
    data class Success(val newBalance: Int) : SpendResult
    data object InsufficientFunds : SpendResult
    data class Error(val message: String) : SpendResult
}

/**
 * RevenueCat wrapper — docs/05-TECH-ARCHITECTURE.md §RevenueCat integration shape.
 * No real products/offerings exist yet (docs/09-PENDING-INPUTS.md, blocked on Play Console),
 * so [FakeBillingRepository] is bound in debug builds and swapped for [RealBillingRepository]
 * without touching any feature code — see core/billing/di/BillingModule.kt.
 */
interface BillingRepository {
    val isPro: StateFlow<Boolean>
    val proExpiresAt: StateFlow<Long?>
    val coinBalance: StateFlow<Int>

    /**
     * Coins RevenueCat granted that just landed in the cabinet — a PRO purchase or renewal
     * (50), a verified rewarded ad (1). Emitted once per reconcile that credited anything, so
     * the app chrome can announce a drop wherever the user happens to be.
     */
    val coinDrops: SharedFlow<Int>

    /**
     * Pulls RevenueCat's COIN balance and credits whatever it granted since the last look.
     * Safe to call any time; offline it does nothing. Returns the coins credited.
     */
    suspend fun syncStoreCoins(): Int

    /**
     * Re-reads entitlements from RevenueCat — after a verified ad granted temporary PRO, so
     * FREE PLAY starts the moment the ad closes rather than on the next app launch.
     */
    suspend fun refreshEntitlements()

    // Deviates from the bare `purchase(pkg)` sketch in docs/05-TECH-ARCHITECTURE.md: the
    // RevenueCat SDK's purchase flow needs a foreground Activity, so it's threaded through.
    suspend fun purchase(activity: Activity, pkg: Package): PurchaseResult
    suspend fun spendCoins(amount: Int, sku: String): SpendResult
    suspend fun refreshBalance()

    /**
     * Credits coins after a rewarded-ad watch, a cleared game, or a streak —
     * docs/02-PRODUCT-SPEC.md §3 "INSERT COIN → watch rewarded ad → 1 coin".
     *
     * Granted on-device via [com.mikhilnaika.continueapp.core.data.CoinLedger]; see that class
     * for why the balance isn't server-authoritative and what changes when AdMob server-side
     * verification becomes available.
     */
    suspend fun earnCoins(amount: Int, reason: String): SpendResult

    /**
     * Credits [amount] the first time [rewardKey] is claimed, and never again.
     *
     * Returns true when coins were actually granted. Backs the clear-a-game reward, which was
     * repeatable — clear, move back to THE PILE, clear again — until a closed tester pointed
     * out the loop. See [com.mikhilnaika.continueapp.core.data.CoinLedger.earnOnce].
     */
    suspend fun earnCoinsOnce(rewardKey: String, amount: Int, reason: String): Boolean

    /**
     * Records [rewardKey] as claimed without paying anything out — what a backdated clear does,
     * so logging your back catalogue can't be used as a coin faucet.
     */
    suspend fun markRewardClaimed(rewardKey: String)

    /** The first purchasable package in the current offering, or null if none is configured yet. */
    suspend fun currentOfferingPackage(): Package?

    /**
     * Every route to PRO in the current offering, newest store prices included. Empty when no
     * offering is configured or the store is unreachable — the paywall renders an honest
     * "out of order" screen rather than a broken purchase button.
     */
    suspend fun proTiers(): List<ProTier>

    /** Buys a tier returned by [proTiers], by its [ProTier.id]. */
    suspend fun purchaseTier(activity: Activity, tierId: String): PurchaseResult

    /**
     * Re-applies entitlements already bought on this Google account.
     *
     * Not optional politeness: Play requires a restore path for non-consumables, and Lifetime is
     * one. Without it, a reinstall silently loses a purchase the user actually made.
     */
    suspend fun restorePurchases(): PurchaseResult
}
