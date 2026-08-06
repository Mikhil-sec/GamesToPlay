package com.mikhilnaika.continueapp.core.billing

import android.app.Activity
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in used until real RevenueCat products/offerings exist
 * (docs/09-PENDING-INPUTS.md — blocked on the Play Console). Lets every Pro-gated and
 * coin-spending feature be built and demoed today.
 */
@Singleton
class FakeBillingRepository @Inject constructor() : BillingRepository {
    override val isPro: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val proExpiresAt: MutableStateFlow<Long?> = MutableStateFlow(null)
    override val coinBalance: MutableStateFlow<Int> = MutableStateFlow(10)

    override suspend fun purchase(activity: Activity, pkg: Package): PurchaseResult {
        isPro.value = true
        return PurchaseResult.Success
    }

    override suspend fun spendCoins(amount: Int, sku: String): SpendResult {
        val current = coinBalance.value
        if (current < amount) return SpendResult.InsufficientFunds
        coinBalance.value = current - amount
        return SpendResult.Success(coinBalance.value)
    }

    override suspend fun refreshBalance() {
        // No-op: nothing to refresh from without a real backend.
    }

    override suspend fun earnCoins(amount: Int, reason: String): SpendResult {
        coinBalance.value += amount
        return SpendResult.Success(coinBalance.value)
    }

    /** No real offering exists in debug either — honest about the actual limitation rather
     * than faking a Package the RevenueCat SDK never issued. */
    override suspend fun currentOfferingPackage(): Package? = null
}
