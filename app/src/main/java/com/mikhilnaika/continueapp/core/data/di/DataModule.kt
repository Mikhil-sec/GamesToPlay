package com.mikhilnaika.continueapp.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.mikhilnaika.continueapp.core.data.AppDatabase
import com.mikhilnaika.continueapp.core.data.AppMigrations
import com.mikhilnaika.continueapp.core.data.SeedLoader
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import com.mikhilnaika.continueapp.core.data.dao.DrawDao
import com.mikhilnaika.continueapp.core.data.dao.FriendDao
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import com.mikhilnaika.continueapp.core.data.dao.StackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "continue_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(dataStore: DataStore<Preferences>): UserPreferencesRepository =
        UserPreferencesRepository(dataStore)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // Every schema change ships with its migration in `AppMigrations.ALL`. Note there is
            // deliberately **no** `fallbackToDestructiveMigration()` — see the ban and the
            // reasoning in DatabaseSchema.
            .addMigrations(*AppMigrations.ALL)
            .build()

    @Provides
    fun provideGameDao(db: AppDatabase): GameDao = db.gameDao()

    @Provides
    fun providePileDao(db: AppDatabase): PileDao = db.pileDao()

    @Provides
    fun provideRankingDao(db: AppDatabase): RankingDao = db.rankingDao()

    @Provides
    fun provideStackDao(db: AppDatabase): StackDao = db.stackDao()

    @Provides
    fun provideDrawDao(db: AppDatabase): DrawDao = db.drawDao()

    @Provides
    fun provideFriendDao(db: AppDatabase): FriendDao = db.friendDao()

    @Provides
    @Singleton
    fun provideSeedLoader(@ApplicationContext context: Context, gameDao: GameDao): SeedLoader =
        SeedLoader(context, gameDao)
}
