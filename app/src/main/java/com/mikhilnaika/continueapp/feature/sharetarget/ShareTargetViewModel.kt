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
    private var lastRawText: String? = null

    fun resolveText(text: String?, subject: String?) {
        lastRawText = text
        viewModelScope.launch {
            _state.value = ShareResolutionState.Loading
            resolveAgainstWorker(text, subject)
        }
    }

    fun resolveImage(imageUri: Uri) {
        viewModelScope.launch {
            _state.value = ShareResolutionState.Loading
            val recognizedText = runCatching { recognizeText(imageUri) }.getOrNull()
            lastRawText = recognizedText
            if (recognizedText.isNullOrBlank()) {
                _state.value = ShareResolutionState.ManualEntry(prefillText = null)
                return@launch
            }
            resolveAgainstWorker(recognizedText, subject = null)
        }
    }

    private suspend fun resolveAgainstWorker(text: String?, subject: String?) {
        // Local candidate extraction happens first so the manual-entry prefill is always the
        // cleanest available text even if everything below fails outright.
        val localCandidates = TitleParser.extractCandidates(text ?: subject)
        val bestLocalGuess = localCandidates.firstOrNull() ?: text
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
                rawText = original ?: bestLocalGuess,
            )
            return
        }

        val response = gameDataSource.resolve(text = text, subject = subject)
        val merged = mergeCandidates(offlineMatches, response.candidates)
        _state.value = classify(
            resolvedTitle = response.resolvedTitle,
            candidates = merged,
            needsManualEntry = merged.isEmpty(),
            rawText = response.resolvedTitle ?: bestLocalGuess ?: text,
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
        _state.value = ShareResolutionState.ManualEntry(prefillText = lastRawText)
    }

    fun searchManually(query: String) {
        viewModelScope.launch {
            val results = runCatching { gameDataSource.search(query) }.getOrDefault(emptyList())
            _state.value = if (results.isEmpty()) {
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
