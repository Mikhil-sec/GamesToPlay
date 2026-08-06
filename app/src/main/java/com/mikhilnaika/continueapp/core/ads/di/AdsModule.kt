package com.mikhilnaika.continueapp.core.ads.di

import com.mikhilnaika.continueapp.BuildConfig
import com.mikhilnaika.continueapp.core.ads.AdRepository
import com.mikhilnaika.continueapp.core.ads.FakeAdRepository
import com.mikhilnaika.continueapp.core.ads.RealAdRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {

    @Provides
    @Singleton
    fun provideAdRepository(
        fake: FakeAdRepository,
        real: RealAdRepository,
    ): AdRepository = if (BuildConfig.DEBUG) fake else real
}
