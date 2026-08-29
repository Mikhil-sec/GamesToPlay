package com.mikhilnaika.continueapp.core.billing

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.mikhilnaika.continueapp.core.data.CoinLedger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Real RevenueCat-backed implementation. `Purchases.configure()` is called once in
 * `ContinueApplication.onCreate` with the public SDK key — this works today per
 * docs/05-TECH-ARCHITECTURE.md even with no products configured yet, and gets the anonymous
 * app-user ID flowing.
 *
 * Entitlements (`pro`) are read from RevenueCat and are authoritative. **Coins are not** —
 * they're held in [CoinLedger] on-device, because the coin economy gates DRAW and CLAUDE.md's
 * constraint #5 requires that to work offline. See [CoinLedger] for why that's the right
 * trade-off today and what replaces it once AdMob server-side verification is possible.
 */
@Singleton
class RealBillingRepository @Inject constructor(
    private val coinLedger: CoinLedger,
) : BillingRepository {
    private val _isPro = MutableStateFlow(false)
    override val isPro: StateFlow<Boolean> = _isPro

    private val _proExpiresAt = MutableStateFlow<Long?>(null)
    override val proExpiresAt: StateFlow<Long?> = _proExpiresAt

    private val _coinBalance = MutableStateFlow(0)
    override val coinBalance: StateFlow<Int> = _coinBalance

    /** Singleton-scoped, so it lives as long as the process — nothing to cancel. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { coinLedger.balance.collect { _coinBalance.value = it } }
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
            applyCustomerInfo(info)
        }
        Purchases.sharedInstance.getCustomerInfo(object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) = applyCustomerInfo(customerInfo)
            override fun onError(error: PurchasesError) = Unit
        })
    }

    private fun applyCustomerInfo(info: CustomerInfo) {
        val entitlement = info.entitlements[ENTITLEMENT_PRO]
        _isPro.value = entitlement?.isActive == true
        _proExpiresAt.value = entitlement?.expirationDate?.time
    }

    override suspend fun purchase(activity: Activity, pkg: Package): PurchaseResult = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.purchase(
            com.revenuecat.purchases.PurchaseParams.Builder(
                activity = activity,
                packageToPurchase = pkg,
            ).build(),
            object : PurchaseCallback {
                override fun onCompleted(purchase: com.revenuecat.purchases.models.StoreTransaction, customerInfo: CustomerInfo) {
                    applyCustomerInfo(customerInfo)
                    cont.resume(PurchaseResult.Success)
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    cont.resume(if (userCancelled) PurchaseResult.UserCancelled else PurchaseResult.Error(error.message))
                }
            },
        )
    }

    override suspend fun spendCoins(amount: Int, sku: String): SpendResult {
        val newBalance = coinLedger.spend(amount) ?: return SpendResult.InsufficientFunds
        return SpendResult.Success(newBalance)
    }

    override suspend fun refreshBalance() {
        // The ledger's Flow is already collected into _coinBalance in init; nothing to pull.
    }

    override suspend fun earnCoins(amount: Int, reason: String): SpendResult =
        SpendResult.Success(coinLedger.earn(amount))

    override suspend fun earnCoinsOnce(rewardKey: String, amount: Int, reason: String): Boolean =
        coinLedger.earnOnce(rewardKey, amount) != null

    override suspend fun markRewardClaimed(rewardKey: String) = coinLedger.markClaimed(rewardKey)

    override suspend fun currentOfferingPackage(): Package? =
        currentPackages().firstOrNull()

    private suspend fun currentPackages(): List<Package> = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.getOfferings(
            object : com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback {
                override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                    cont.resume(offerings.current?.availablePackages.orEmpty())
                }

                override fun onError(error: PurchasesError) {
                    cont.resume(emptyList())
                }
            },
        )
    }

    /**
     * Ordered cheapest-commitment first, so the paywall reads Monthly → Lifetime regardless of
     * how the packages happen to be ordered in the dashboard.
     */
    override suspend fun proTiers(): List<ProTier> =
        currentPackages().map { it.toProTier() }.sortedBy { it.isLifetime }

    override suspend fun purchaseTier(activity: Activity, tierId: String): PurchaseResult {
        val pkg = currentPackages().firstOrNull { it.identifier == tierId }
            ?: return PurchaseResult.Error("That plan isn't available right now.")
        return purchase(activity, pkg)
    }

    override suspend fun restorePurchases(): PurchaseResult = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.restorePurchases(
            object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    applyCustomerInfo(customerInfo)
                    cont.resume(
                        if (customerInfo.entitlements[ENTITLEMENT_PRO]?.isActive == true) {
                            PurchaseResult.Success
                        } else {
                            // Restore *succeeded* and found nothing — a different outcome from a
                            // failure, and the user needs to be told which one happened.
                            PurchaseResult.Error("No previous purchase found on this Google account.")
                        },
                    )
                }

                override fun onError(error: PurchasesError) {
                    cont.resume(PurchaseResult.Error(error.message))
                }
            },
        )
    }

    private fun Package.toProTier(): ProTier {
        val isLifetime = packageType == com.revenuecat.purchases.PackageType.LIFETIME ||
            product.type == com.revenuecat.purchases.ProductType.INAPP
        // A subscription's real terms live on its default SubscriptionOption, not on the
        // product: `product.price` is the full-price phase, and the free trial is a separate
        // pricing phase that only appears there.
        val option = product.defaultOption
        val trialDays = option?.freePhase?.billingPeriod?.let { period ->
            when (period.unit) {
                com.revenuecat.purchases.models.Period.Unit.DAY -> period.value
                com.revenuecat.purchases.models.Period.Unit.WEEK -> period.value * 7
                com.revenuecat.purchases.models.Period.Unit.MONTH -> period.value * 30
                com.revenuecat.purchases.models.Period.Unit.YEAR -> period.value * 365
                else -> null
            }
        }
        return ProTier(
            id = identifier,
            label = if (isLifetime) "LIFETIME" else billingLabel(option?.billingPeriod),
            priceFormatted = option?.fullPricePhase?.price?.formatted ?: product.price.formatted,
            cadence = if (isLifetime) null else cadenceLabel(option?.billingPeriod),
            freeTrialDays = trialDays,
            isLifetime = isLifetime,
        )
    }

    private fun billingLabel(period: com.revenuecat.purchases.models.Period?): String =
        when (period?.unit) {
            com.revenuecat.purchases.models.Period.Unit.MONTH -> "MONTHLY"
            com.revenuecat.purchases.models.Period.Unit.YEAR -> "ANNUAL"
            com.revenuecat.purchases.models.Period.Unit.WEEK -> "WEEKLY"
            else -> "PRO"
        }

    private fun cadenceLabel(period: com.revenuecat.purchases.models.Period?): String =
        when (period?.unit) {
            com.revenuecat.purchases.models.Period.Unit.MONTH -> "per month"
            com.revenuecat.purchases.models.Period.Unit.YEAR -> "per year"
            com.revenuecat.purchases.models.Period.Unit.WEEK -> "per week"
            else -> "recurring"
        }

    companion object {
        const val ENTITLEMENT_PRO = "pro"
    }
}
