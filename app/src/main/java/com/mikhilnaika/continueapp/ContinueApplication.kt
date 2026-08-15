package com.mikhilnaika.continueapp

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.google.android.gms.ads.MobileAds
import com.mikhilnaika.continueapp.core.data.SeedLoader
import com.mikhilnaika.continueapp.core.offline.OfflineGameIndex
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
class ContinueApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var seedLoader: SeedLoader

    @Inject
    lateinit var offlineGameIndex: OfflineGameIndex

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
        // Warms the offline share-matching index (docs/08-GAME-DATA.md §Data dumps) so it's
        // already loaded by the time a share arrives, not decompressing 41k rows on the first
        // one. Injecting the field already started the load; this just makes it explicit.
        applicationScope.launch {
            offlineGameIndex.warmUp()
        }
    }

    /**
     * Cover art is the one part of the pile that wasn't offline-first: Room holds the games, but
     * every cover went back to the network, so a pile viewed on a train was a wall of grey
     * rectangles. A generous, explicitly-sized disk cache fixes that for anything seen once.
     *
     * An IGDB cover URL contains the image's own hash (`.../t_cover_big/co1abc.jpg`), so a given
     * URL's bytes can never change — a cached cover is never stale, only ever absent.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(192L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .build()
}
