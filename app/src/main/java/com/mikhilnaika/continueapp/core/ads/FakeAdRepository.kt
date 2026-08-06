package com.mikhilnaika.continueapp.core.ads

import android.app.Activity
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAdRepository @Inject constructor() : AdRepository {
    private object FakeLoadedAd : LoadedAd

    override suspend fun loadCoinAd(): LoadedAd {
        delay(300) // simulate load latency so UI loading states are exercised
        return FakeLoadedAd
    }

    override suspend fun loadFreePlayAd(): LoadedAd {
        delay(300)
        return FakeLoadedAd
    }

    override suspend fun show(activity: Activity, ad: LoadedAd): AdResult {
        delay(800) // simulate watch time
        return AdResult.Granted
    }
}
