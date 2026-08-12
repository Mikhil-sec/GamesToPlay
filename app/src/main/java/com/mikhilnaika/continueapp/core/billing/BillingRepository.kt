package com.mikhilnaika.continueapp.core.billing

import android.app.Activity
import com.revenuecat.purchases.Package
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

    /** The first purchasable package in the current offering, or null if none is configured yet. */
    suspend fun currentOfferingPackage(): Package?
}
