package com.mikhilnaika.continueapp.core.ads

import android.app.Activity
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug stand-in. It reports every watch as [AdResult.Unverified] because that is the truth:
 * nothing here can reach RevenueCat's verification. That exercises INSERT COIN's local
 * fallback, and shows FREE PLAY's honest failure message — FREE PLAY can only ever be seen
 * working in a release build against real ad units.
 */
@Singleton
class FakeAdRepository @Inject constructor() : AdRepository {
    private object FakeLoadedAd : LoadedAd

    override suspend fun load(placement: AdPlacement): LoadedAd {
        delay(300) // simulate load latency so UI loading states are exercised
        return FakeLoadedAd
    }

    override suspend fun show(activity: Activity, ad: LoadedAd): AdResult {
        delay(800) // simulate watch time
        return AdResult.Unverified
    }
}
