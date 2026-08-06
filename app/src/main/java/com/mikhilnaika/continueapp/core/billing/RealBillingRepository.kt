package com.mikhilnaika.continueapp.core.billing

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Real RevenueCat-backed implementation. `Purchases.configure()` is called once in
 * `ContinueApplication.onCreate` with the public SDK key — this works today per
 * docs/05-TECH-ARCHITECTURE.md even with no products configured yet, and gets the
 * anonymous app-user ID flowing. `/coins/spend` isn't live on the Worker yet
 * (docs/09-PENDING-INPUTS.md), so [spendCoins] fails closed with a clear error rather than
 * silently granting currency.
 */
@Singleton
class RealBillingRepository @Inject constructor() : BillingRepository {
    private val _isPro = MutableStateFlow(false)
    override val isPro: StateFlow<Boolean> = _isPro

    private val _proExpiresAt = MutableStateFlow<Long?>(null)
    override val proExpiresAt: StateFlow<Long?> = _proExpiresAt

    private val _coinBalance = MutableStateFlow(0)
    override val coinBalance: StateFlow<Int> = _coinBalance

    init {
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

    override suspend fun spendCoins(amount: Int, sku: String): SpendResult =
        SpendResult.Error("Coin spend requires the Worker's /coins/spend endpoint, not yet configured (docs/09-PENDING-INPUTS.md)")

    override suspend fun refreshBalance() {
        // Balance lives server-side once /coins/spend is live; nothing to refresh yet.
    }

    companion object {
        const val ENTITLEMENT_PRO = "pro"
    }
}
