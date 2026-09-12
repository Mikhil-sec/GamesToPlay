package com.mikhilnaika.continueapp.core.share.di

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ShareModule {

    /**
     * Hands out **the app's existing** Coil loader, not a new one.
     *
     * This matters more than it looks. `ContinueApplication` configures a 192 MB disk cache
     * specifically so a pile viewed offline isn't a wall of grey rectangles — and share cards
     * draw the same covers the pile just displayed. Reaching the same instance means the card
     * renders from bytes already on disk: instant, no network, and it works on a train, which
     * is the offline-first promise in CLAUDE.md actually holding for a feature rather than
     * being claimed for one.
     *
     * Building a second `ImageLoader` here would compile, work on the bench, and quietly give
     * the share cards their own empty cache and their own memory budget.
     */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        SingletonImageLoader.get(context)
}
