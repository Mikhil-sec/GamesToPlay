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

    override suspend fun currentOfferingPackage(): Package? = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.getOfferings(
            object : com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback {
                override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                    cont.resume(offerings.current?.availablePackages?.firstOrNull())
                }

                override fun onError(error: PurchasesError) {
                    cont.resume(null)
                }
            },
        )
    }

    companion object {
        const val ENTITLEMENT_PRO = "pro"
    }
}
