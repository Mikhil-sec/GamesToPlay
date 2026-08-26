package com.mikhilnaika.continueapp.feature.sharetarget

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.entity.GameEntity
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import com.mikhilnaika.continueapp.core.network.GameDataSource
import com.mikhilnaika.continueapp.core.network.dto.ResolveCandidateDto
import com.mikhilnaika.continueapp.core.offline.OfflineGameIndex
import com.mikhilnaika.continueapp.core.util.GameNameCandidates
import com.mikhilnaika.continueapp.core.util.TitleParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * A tiktok.com link, matched by host label. `(?:^|[^\w.-])` refuses a match that continues
 * leftwards into a longer label (`eviltiktok.com`) and the lookahead refuses one that continues
 * rightwards (`tiktok.com.example.org`), which is the whole difference between a host check and
 * a substring check.
 */
private val TIKTOK_LINK = Regex("""(?:^|[^\w.-])(?:[\w-]+\.)*tiktok\.com(?=[/:?#]|$)""", RegexOption.IGNORE_CASE)

/**
 * True when [raw] carries a tiktok.com link.
 *
 * Matched on the *host*, not `contains("tiktok.com")` — the same distinction the Worker makes in
 * `security.ts`, for the same reason it's easy to get wrong: `tiktok.com.example` contains the
 * substring and is not TikTok, while `vm.tiktok.com` and `vt.tiktok.com` (the two short forms
 * the Android app actually shares) are.
 */
internal fun looksLikeTikTokLink(raw: String?): Boolean =
    raw != null && TIKTOK_LINK.containsMatchIn(raw)

/**
 * Drives the transparent share-sheet Activity — docs/02-PRODUCT-SPEC.md §2a. Two entry
 * points: [resolveText] for `text/plain` shares, [resolveImage] for image shares (the
 * Instagram-Reels workaround, run entirely on-device via ML Kit).
 */
@HiltViewModel
class ShareTargetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDataSource: GameDataSource,
    private val gameDao: GameDao,
    private val pileDao: PileDao,
    private val offlineGameIndex: OfflineGameIndex,
) : ViewModel() {

    private val _state = MutableStateFlow<ShareResolutionState>(ShareResolutionState.Loading)
    val state: StateFlow<ShareResolutionState> = _state

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * What to put in the manual-entry field — a cleaned, URL-free fragment, or null.
     *
     * This deliberately no longer holds the raw shared text. It used to, and that is exactly
     * what the first closed-test report was about: a YouTube or Instagram share dropped its
     * whole URL into the search box, so every fallback started with the user selecting and
     * deleting a link before they could type anything. An empty field is strictly better than
     * a field with a URL in it; a partial title is better still, which is what this holds when
     * one is available. See docs/10-BUILD-STATUS.md 2026-08-19.
     */
    private var lastPrefill: String? = null

    /**
     * Whether the share that opened this sheet came from TikTok — see [classify], which is where
     * it changes the outcome. Sticky for the life of the sheet so "search instead" and a failed
     * manual search land back on the same explanation rather than the generic one.
     */
    private var isTikTokShare: Boolean = false

    fun resolveText(text: String?, subject: String?) {
        lastPrefill = cleanPrefill(text) ?: cleanPrefill(subject)
        isTikTokShare = looksLikeTikTokLink(text) || looksLikeTikTokLink(subject)
        viewModelScope.launch {
            _state.value = ShareResolutionState.Loading
            resolveAgainstWorker(text, subject)
        }
    }

    fun resolveImage(imageUri: Uri) {
        viewModelScope.launch {
            _state.value = ShareResolutionState.Loading
            val recognizedText = runCatching { recognizeText(imageUri) }.getOrNull()
            lastPrefill = cleanPrefill(recognizedText)
            // A screenshot is pixels, not a link — OCR text stands on its own merits whatever app
            // it was captured in, so the TikTok rule deliberately doesn't reach this path.
            isTikTokShare = false
            if (recognizedText.isNullOrBlank()) {
                _state.value = ShareResolutionState.ManualEntry(prefillText = null)
                return@launch
            }
            resolveAgainstWorker(recognizedText, subject = null)
        }
    }

    /**
     * The best game-name-shaped fragment of [raw], or null if there isn't one.
     *
     * [TitleParser] strips links (including scheme-less ones like `youtu.be/abc`), so anything
     * that survives is prose rather than a URL. A share that was *only* a link therefore
     * correctly yields null — Instagram's are structurally unresolvable, and a blank field is
     * the honest answer there.
     */
    private fun cleanPrefill(raw: String?): String? =
        TitleParser.extractCandidates(raw).firstOrNull()?.takeIf { it.isNotBlank() }

    private suspend fun resolveAgainstWorker(text: String?, subject: String?) {
        // Local candidate extraction happens first so the manual-entry prefill is always the
        // cleanest available text even if everything below fails outright. Note it falls back
        // to null, never to `text` — falling back to the raw share text is what put URLs in the
        // search box (see [lastPrefill]).
        val bestLocalGuess = cleanPrefill(text) ?: cleanPrefill(subject)
        val original = text ?: subject

        // Offline-first (docs/08-GAME-DATA.md §Data dumps): the on-device index can answer
        // instantly with zero network, so it always runs first. A confident hit skips the
        // Worker call entirely — this is CLAUDE.md constraint #5 actually holding for the
        // headline demo feature, not just claimed for it.
        val offlineMatches = runCatching { offlineGameIndex.match(original) }.getOrElse { emptyList() }

        if ((offlineMatches.firstOrNull()?.confidence ?: 0f) >= GameNameCandidates.CONFIDENT_ENOUGH) {
            _state.value = classify(
                resolvedTitle = original,
                candidates = offlineMatches,
                needsManualEntry = false,
                rawText = bestLocalGuess,
                isTikTok = isTikTokShare,
            )
            return
        }

        val response = gameDataSource.resolve(text = text, subject = subject)
        val merged = mergeCandidates(offlineMatches, response.candidates)

        // Preference order for the field, best first: the Worker's ranked guess at a game name,
        // then the Worker's resolved title cleaned locally (a YouTube video title is real text
        // but still full of channel branding), then whatever the share text itself yielded.
        // Every rung is URL-free by construction, and the whole thing may be null.
        //
        // The Worker's `suggestion` goes back through [cleanPrefill] rather than being trusted
        // as-is. It is documented as never containing a URL and it never has in testing, but
        // this field is the one place in the app where being wrong costs the user a
        // select-all-and-delete before they can type — so the guarantee is enforced on the side
        // that suffers if it breaks, not just asserted on the side that makes it.
        val prefill = cleanPrefill(response.suggestion)
            ?: cleanPrefill(response.resolvedTitle)
            ?: bestLocalGuess
        lastPrefill = prefill

        _state.value = classify(
            resolvedTitle = response.resolvedTitle,
            candidates = merged,
            needsManualEntry = merged.isEmpty(),
            rawText = prefill,
            isTikTok = isTikTokShare,
        )
    }

    /**
     * Combines offline and network candidates by IGDB id — safe because both sides are real
     * IGDB game ids from the same `games` table, one read from the REST API, one from the CSV
     * dump. Keeps the higher-confidence entry per id, so a weak offline hit never shadows a
     * strong network one or vice versa.
     */
    private fun mergeCandidates(
        offline: List<ResolveCandidateDto>,
        network: List<ResolveCandidateDto>,
    ): List<ResolveCandidateDto> {
        val byId = LinkedHashMap<Long, ResolveCandidateDto>()
        for (candidate in network) byId[candidate.id] = candidate
        for (candidate in offline) {
            val existing = byId[candidate.id]
            if (existing == null || existing.confidence < candidate.confidence) byId[candidate.id] = candidate
        }
        return byId.values.sortedByDescending { it.confidence }.take(5)
    }

    private suspend fun recognizeText(imageUri: Uri): String? = suspendCancellableCoroutine { cont ->
        runCatching {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText -> cont.resume(visionText.text) }
                .addOnFailureListener { cont.resume(null) }
        }.onFailure { cont.resume(null) }
    }

    fun addCandidate(candidate: ResolveCandidateDto) {
        viewModelScope.launch {
            gameDao.upsert(
                GameEntity(
                    id = candidate.id,
                    slug = candidate.name.lowercase().replace(Regex("[^a-z0-9]+"), "-"),
                    name = candidate.name,
                    coverUrl = candidate.coverUrl,
                    backgroundUrl = null,
                    released = null,
                    metacritic = null,
                    rating = null,
                    playtimeHoursHastily = null,
                    playtimeHoursNormally = null,
                    playtimeHoursCompletely = null,
                    genresJson = json.encodeToString(emptyList<String>()),
                    tagsJson = json.encodeToString(emptyList<String>()),
                    platformsJson = json.encodeToString(emptyList<String>()),
                    cachedAt = System.currentTimeMillis(),
                )
            )
            if (pileDao.findByGameId(candidate.id) == null) {
                pileDao.insert(
                    PileEntryEntity(
                        gameId = candidate.id,
                        state = PileState.BACKLOG,
                        addedAt = System.currentTimeMillis(),
                        source = AddSource.SHARE_TARGET,
                    )
                )
            }
            _state.value = ShareResolutionState.Added(candidate.name)
        }
    }

    /** "search instead" — always available, per the degradation ladder. */
    fun fallBackToManualEntry() {
        _state.value = if (isTikTokShare) {
            ShareResolutionState.ManualEntry(prefillText = null, note = TIKTOK_NOTE)
        } else {
            ShareResolutionState.ManualEntry(prefillText = lastPrefill)
        }
    }

    fun searchManually(query: String) {
        viewModelScope.launch {
            val results = runCatching { gameDataSource.search(query) }.getOrDefault(emptyList())
            _state.value = if (results.isEmpty()) {
                // The user's own typing comes back in the field — unlike a machine guess, they
                // want to edit it rather than clear it. No TikTok note here: they've already
                // typed, so the explanation for why they had to has done its job.
                ShareResolutionState.ManualEntry(prefillText = query)
            } else {
                ShareResolutionState.Ambiguous(
                    results.take(3).map {
                        ResolveCandidateDto(id = it.id, name = it.name, confidence = 1f, coverUrl = it.coverUrl)
                    }
                )
            }
        }
    }
}
