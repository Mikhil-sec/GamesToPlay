package com.mikhilnaika.continueapp.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
        val HOURS_PER_WEEK = floatPreferencesKey("hours_per_week")
        val LAST_CLIPBOARD_SUGGESTION = stringPreferencesKey("last_clipboard_suggestion")
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

    /**
     * How many hours a week the user actually plays — the input to PILE's "FINISHED BY 2029".
     *
     * It was a `MutableStateFlow` inside `PileViewModel` that nothing could write to, so the
     * headline number on the app's signature bar was derived from a hardcoded 6 and the same for
     * everybody. Persisted rather than kept per-ViewModel because it's a fact about the person,
     * not about this visit to the screen.
     */
    val hoursPerWeek: Flow<Float> =
        dataStore.data.map { it[Keys.HOURS_PER_WEEK] ?: DEFAULT_HOURS_PER_WEEK }

    suspend fun setHoursPerWeek(hours: Float) {
        dataStore.edit { it[Keys.HOURS_PER_WEEK] = hours.coerceIn(MIN_HOURS_PER_WEEK, MAX_HOURS_PER_WEEK) }
    }

    /**
     * The last clipboard text the nudge already acted on — added or dismissed.
     *
     * Persisted so a dismissal survives a relaunch. Re-offering a suggestion the user has already
     * said no to is precisely the "irritation when it's wrong" the spec warns about
     * (docs/02-PRODUCT-SPEC.md §2d, "bias toward silence").
     */
    val lastClipboardSuggestion: Flow<String?> =
        dataStore.data.map { it[Keys.LAST_CLIPBOARD_SUGGESTION] }

    suspend fun setLastClipboardSuggestion(text: String) {
        dataStore.edit { it[Keys.LAST_CLIPBOARD_SUGGESTION] = text }
    }

    companion object {
        /** A couple of evenings a week — the assumption the bar used to hardcode. */
        const val DEFAULT_HOURS_PER_WEEK = 6f

        /** Below this the finish date stops being a projection and starts being a joke. */
        const val MIN_HOURS_PER_WEEK = 1f

        /** 40h/week is a full-time job. Anything past it isn't a backlog problem. */
        const val MAX_HOURS_PER_WEEK = 40f
    }
}
