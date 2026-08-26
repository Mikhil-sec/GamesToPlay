package com.mikhilnaika.continueapp.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val PILE_VIEW_MODE = stringPreferencesKey("pile_view_mode")
        val STACK_SWIPE_HINT_SEEN = booleanPreferencesKey("stack_swipe_hint_seen")
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

    /**
     * PILE's view mode — docs/02-PRODUCT-SPEC.md §1 says the toggle's state is persisted.
     *
     * Deliberately stored and returned as a raw string: `PileViewMode` belongs to
     * `feature/pile`, and having core/data name a feature type would invert the dependency for
     * the sake of one enum. The caller maps it, and is the right place to decide what an
     * unrecognised value from an older or newer build should fall back to.
     */
    val pileViewMode: Flow<String?> = dataStore.data.map { it[Keys.PILE_VIEW_MODE] }

    suspend fun setPileViewMode(mode: String) {
        dataStore.edit { it[Keys.PILE_VIEW_MODE] = mode }
    }

    /**
     * Whether the user has ever flicked through the STACK view.
     *
     * STACK is the pile's default view and its only gesture is a vertical drag that nothing on
     * screen asks for — a closed-test tester read the receding cards as decoration and never
     * tried. Persisted rather than kept per-session so the prompt is a one-time teach, not a
     * permanent label on the app's signature view.
     */
    val isStackSwipeHintSeen: Flow<Boolean> =
        dataStore.data.map { it[Keys.STACK_SWIPE_HINT_SEEN] ?: false }

    suspend fun setStackSwipeHintSeen() {
        dataStore.edit { it[Keys.STACK_SWIPE_HINT_SEEN] = true }
    }
}
