package com.mikhilnaika.continueapp

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.mikhilnaika.continueapp.core.data.SeedLoader
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ContinueApplication : Application() {

    @Inject
    lateinit var seedLoader: SeedLoader

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // docs/05-TECH-ARCHITECTURE.md: this works today even with no products configured,
        // and gets the anonymous app-user ID flowing. Must never block the first frame.
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
        Purchases.configure(
            PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_PUBLIC_KEY).build()
        )

        // AdMob and the seed load both do I/O; keep them off the main thread and off the
        // cold-start critical path (docs/05-TECH-ARCHITECTURE.md — cold start < 1.5s).
        applicationScope.launch {
            MobileAds.initialize(this@ContinueApplication)
        }
        applicationScope.launch {
            seedLoader.loadIfEmpty()
        }
    }
}
