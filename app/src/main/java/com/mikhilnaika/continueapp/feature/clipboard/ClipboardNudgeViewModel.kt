package com.mikhilnaika.continueapp.feature.clipboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.GameCacheRepository
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import com.mikhilnaika.continueapp.core.network.dto.ResolveCandidateDto
import com.mikhilnaika.continueapp.core.offline.OfflineGameIndex
import com.mikhilnaika.continueapp.core.util.GameNameCandidates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The offer on screen: a game the clipboard plausibly names, and the text it came from. */
data class ClipboardSuggestion(
    val candidate: ResolveCandidateDto,
    val sourceText: String,
)

/**
 * docs/02-PRODUCT-SPEC.md §2d "Clipboard nudge" — *"a delight when it's right and an irritation
 * when it's wrong, so bias toward silence."*
 *
 * **This existed as a settings toggle and nothing else.** `CLIPBOARD DETECTION` was stored in
 * DataStore and drawn in YOU, and no other file in the app read it — the same shape as the
 * HAPTICS bug fixed in the same release, and sitting one row below it in the same list. A switch
 * that does nothing is worse than a missing feature: it tells the user the app is doing something
 * it isn't.
 *
 * Matching is **offline only**, against the IGDB name index already shipped in the APK
 * (`OfflineGameIndex`). Not for speed — because a nudge that costs a network round trip on every
 * foreground is a nudge that fires late, after the user has moved on, and would spend Worker
 * quota on text that is usually a phone number or a URL.
 */
@HiltViewModel
class ClipboardNudgeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val offlineGameIndex: OfflineGameIndex,
    private val gameCacheRepository: GameCacheRepository,
    private val pileDao: PileDao,
) : ViewModel() {

    private val _suggestion = MutableStateFlow<ClipboardSuggestion?>(null)
    val suggestion: StateFlow<ClipboardSuggestion?> = _suggestion

    /**
     * Whether the feature may read the clipboard at all — exposed so the **view** can gate the
     * read itself.
     *
     * This is not a duplicate of the check inside [onClipboardText], and the difference is the
     * whole privacy story. Checking only in the ViewModel would mean the clipboard was already
     * read by the time anything looked at the setting, and on Android 12+ the system toast
     * ("CONTINUE? pasted from your clipboard") fires on the *read* — so every user would see it
     * on every app open whether or not they had opted in. The setting has to gate the read, and
     * the read happens in the composable.
     *
     * Starts `false` and fails closed: on the very first resume, before DataStore has emitted,
     * nothing is read. Missing one nudge is the correct trade against reading a clipboard we
     * haven't yet confirmed we're allowed to.
     */
    val isEnabled: StateFlow<Boolean> = userPreferencesRepository.isClipboardDetectionEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Called with whatever is on the clipboard each time the app comes to the foreground.
     *
     * Every rung below is a reason to stay silent, and they're ordered cheapest-first so the
     * common case (a URL, a paragraph, nothing) costs almost nothing.
     */
    fun onClipboardText(rawText: String?) {
        viewModelScope.launch {
            val text = rawText?.trim().orEmpty()
            if (!isPlausibleGameName(text)) {
                _suggestion.value = null
                return@launch
            }
            // Already added or already refused — the strongest silence rule there is.
            if (userPreferencesRepository.lastClipboardSuggestion.first() == text) return@launch
            // Re-checked here as well as at the read site: defence in depth against a future
            // caller that forgets, since this one is cheap and the other one is load-bearing.
            if (!userPreferencesRepository.isClipboardDetectionEnabled.first()) return@launch

            val match = try {
                offlineGameIndex.match(text).firstOrNull()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            } ?: return@launch

            // Only a *confident* hit. The share target can afford to offer three guesses because
            // the user asked it a question; this one interrupts unprompted, so it has to be right.
            if (match.confidence < GameNameCandidates.CONFIDENT_ENOUGH) return@launch
            if (pileDao.findByGameId(match.id) != null) {
                // Already in the pile. Burn the text so it can't nag on every foreground.
                userPreferencesRepository.setLastClipboardSuggestion(text)
                return@launch
            }
            _suggestion.value = ClipboardSuggestion(match, text)
        }
    }

    fun add() {
        val offer = _suggestion.value ?: return
        _suggestion.value = null
        viewModelScope.launch {
            userPreferencesRepository.setLastClipboardSuggestion(offer.sourceText)
            gameCacheRepository.cacheMinimal(offer.candidate.id, offer.candidate.name, offer.candidate.coverUrl)
            if (pileDao.findByGameId(offer.candidate.id) == null) {
                pileDao.insert(
                    PileEntryEntity(
                        gameId = offer.candidate.id,
                        state = PileState.BACKLOG,
                        addedAt = System.currentTimeMillis(),
                        source = AddSource.CLIPBOARD,
                    )
                )
            }
            gameCacheRepository.hydrateInBackground(offer.candidate.id)
        }
    }

    fun dismiss() {
        val offer = _suggestion.value ?: return
        _suggestion.value = null
        // Persisted, so "no" survives a relaunch. Re-asking is the irritation the spec warns of.
        viewModelScope.launch { userPreferencesRepository.setLastClipboardSuggestion(offer.sourceText) }
    }

    companion object {
        /**
         * Whether [text] is worth spending a dictionary lookup on.
         *
         * Internal and pure so `ClipboardNudgeTest` can pin it — this is the function that decides
         * how often the feature interrupts, and every rule in it is a real category of clipboard
         * content that is definitely not a game name.
         */
        internal fun isPlausibleGameName(text: String): Boolean {
            if (text.length !in MIN_LENGTH..MAX_LENGTH) return false
            // A link belongs to the share target, which resolves it properly. Checked without a
            // regex on purpose — plain `contains` can't be mangled by an escaping mistake.
            if (text.contains("http", ignoreCase = true)) return false
            if (text.contains("www.", ignoreCase = true)) return false
            if (text.contains("@")) return false
            if (text.any { it == '\n' || it == '\r' }) return false
            val words = text.split(' ').filter { it.isNotBlank() }
            if (words.size > MAX_WORDS) return false
            // Copied one-time codes, phone numbers, order references: no letters, not a title.
            if (text.none { it.isLetter() }) return false
            return true
        }

        /** "Ico" is a real game; two characters is not worth interrupting anyone over. */
        private const val MIN_LENGTH = 3

        /** Longer than any game title and well into "this is a paragraph" territory. */
        private const val MAX_LENGTH = 60

        /** *Sid Meier's Civilization VI: Gathering Storm* is 6. Past that it's prose. */
        private const val MAX_WORDS = 8
    }
}
