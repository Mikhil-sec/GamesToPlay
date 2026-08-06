package com.mikhilnaika.continueapp.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Onboarding must be under 40 seconds and never shown twice — docs/02-PRODUCT-SPEC.md §8. */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val CLIPBOARD_DETECTION_ENABLED = booleanPreferencesKey("clipboard_detection_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val isOnboardingComplete: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete() {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = true }
    }

    val isClipboardDetectionEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.CLIPBOARD_DETECTION_ENABLED] ?: false }

    suspend fun setClipboardDetectionEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.CLIPBOARD_DETECTION_ENABLED] = enabled }
    }

    val isHapticsEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.HAPTICS_ENABLED] ?: true }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }
}
