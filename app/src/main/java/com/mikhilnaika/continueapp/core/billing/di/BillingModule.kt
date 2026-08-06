package com.mikhilnaika.continueapp.core.billing.di

import com.mikhilnaika.continueapp.BuildConfig
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import com.mikhilnaika.continueapp.core.billing.FakeBillingRepository
import com.mikhilnaika.continueapp.core.billing.RealBillingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * No real RevenueCat products/offerings/paywall exist yet — only the project, app,
 * entitlement, and virtual currency are provisioned (docs/09-PENDING-INPUTS.md). Debug
 * builds get [FakeBillingRepository] so paywall/coin UI can be built and demoed today;
 * `Purchases.configure()` still runs for real in both (see ContinueApplication), so the
 * anonymous app-user ID flows regardless. Flip DEBUG builds to Real once products exist.
 */
@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun provideBillingRepository(
        fake: FakeBillingRepository,
        real: RealBillingRepository,
    ): BillingRepository = if (BuildConfig.DEBUG) fake else real
}
