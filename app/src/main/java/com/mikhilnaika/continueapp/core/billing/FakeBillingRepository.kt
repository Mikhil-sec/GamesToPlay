package com.mikhilnaika.continueapp.core.billing

import android.app.Activity
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.MutableSharedFlow
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
    override val coinDrops = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Debug has no RevenueCat balance to read. */
    override suspend fun syncStoreCoins(): Int = 0

    override suspend fun refreshEntitlements() = Unit

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

    /** In-memory mirror of the real ledger's claimed-reward set, so debug builds behave the same. */
    private val claimedRewards = mutableSetOf<String>()

    override suspend fun earnCoinsOnce(rewardKey: String, amount: Int, reason: String): Boolean {
        if (!claimedRewards.add(rewardKey)) return false
        coinBalance.value += amount
        return true
    }

    override suspend fun markRewardClaimed(rewardKey: String) {
        claimedRewards.add(rewardKey)
    }

    /** Still null: faking a `Package` the RevenueCat SDK never issued would be a lie the
     *  purchase call can't honour. [proTiers] is the honest way to make the paywall demoable. */
    override suspend fun currentOfferingPackage(): Package? = null

    /**
     * Mirrors the real `default` offering (docs/09-PENDING-INPUTS.md) so the paywall can be
     * designed, screenshotted and reviewed in a debug build, which can't reach Play Billing.
     *
     * Prices here are the ones configured in Play Console. They are **display-only in debug**
     * and are never charged — [purchaseTier] just flips the flag. If pricing changes, the real
     * paywall follows automatically because it reads `Price.formatted` from the store; only
     * this fake needs a manual edit, which is the correct place for the drift to sit.
     */
    override suspend fun proTiers(): List<ProTier> = listOf(
        ProTier(
            id = "\$rc_monthly",
            label = "MONTHLY",
            priceFormatted = "$3.99",
            cadence = "per month",
            freeTrialDays = 7,
            isLifetime = false,
        ),
        ProTier(
            id = "\$rc_lifetime",
            label = "LIFETIME",
            priceFormatted = "$9.99",
            cadence = null,
            freeTrialDays = null,
            isLifetime = true,
        ),
    )

    override suspend fun purchaseTier(activity: Activity, tierId: String): PurchaseResult {
        isPro.value = true
        return PurchaseResult.Success
    }

    override suspend fun restorePurchases(): PurchaseResult =
        if (isPro.value) PurchaseResult.Success else PurchaseResult.Error("Nothing to restore in debug.")
}
