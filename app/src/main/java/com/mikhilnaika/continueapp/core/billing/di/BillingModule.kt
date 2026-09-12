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
 * **Every shipped build uses [RealBillingRepository] and the RevenueCat SDK for real.** Only
 * `debug` gets [FakeBillingRepository], and only so the paywall and coin UI can be worked on
 * without a Play licence-tester account and a live purchase per iteration.
 *
 * `Purchases.configure()` runs for real in *both* (see `ContinueApplication`), so the anonymous
 * app-user ID flows either way.
 *
 * **Correction 2026-09-12 — the note that used to be here was stale.** It said "no real
 * RevenueCat products/offerings/paywall exist yet" and told a future reader to flip debug to
 * Real "once products exist". All five products, both offerings and the `pro` entitlement have
 * been provisioned since 2026-08-12, and closed testers have completed real Play transactions
 * through this class with the entitlement going active (verified in RevenueCat 2026-09-07).
 * The debug/release split stays as it is — it's a development convenience, not a gap.
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
