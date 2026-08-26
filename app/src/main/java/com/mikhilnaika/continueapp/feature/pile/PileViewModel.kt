package com.mikhilnaika.continueapp.feature.pile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import com.mikhilnaika.continueapp.core.data.dao.StackDao
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import com.mikhilnaika.continueapp.core.util.estimatedHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** docs/02-PRODUCT-SPEC.md §1 — NOW PLAYING is hard-capped at 3. */
const val NOW_PLAYING_CAP = 3

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PileViewModel @Inject constructor(
    private val pileDao: PileDao,
    private val stackDao: StackDao,
    private val rankingDao: RankingDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val selectedState = MutableStateFlow(PileState.BACKLOG)
    private val viewMode = MutableStateFlow(PileUiState().viewMode)
    private val sort = MutableStateFlow(PileSort.DATE_ADDED)
    private val platformFilter = MutableStateFlow<String?>(null)
    private val genreFilter = MutableStateFlow<String?>(null)
    private val lengthBucketFilter = MutableStateFlow<LengthBucket?>(null)
    private val hoursPerWeek = MutableStateFlow(6f)
    private val swapPrompt = MutableStateFlow<SwapPrompt?>(null)
    private val showStackHint = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(PileUiState())
    val state: StateFlow<PileUiState> = _uiState

    private val json = Json { ignoreUnknownKeys = true }

    init {
        combine(
            selectedState.flatMapLatest { pileDao.observeByState(it) },
            sort,
            platformFilter,
            genreFilter,
            lengthBucketFilter,
        ) { rawEntries, sortOrder, platform, genre, lengthBucket ->
            val filtered = rawEntries
                .filter { platform == null || platformsOf(it).contains(platform) }
                .filter { genre == null || genresOf(it).contains(genre) }
                .filter { lengthBucket == null || fitsLengthBucket(it, lengthBucket) }
            val sorted = sortEntries(filtered, sortOrder)
            RawSnapshot(
                sorted = sorted,
                totalHours = filtered.sumOf { it.estimatedHours ?: 0 },
                totalGames = filtered.size,
                availablePlatforms = rawEntries.flatMap { platformsOf(it) }.distinct().sorted(),
                availableGenres = rawEntries.flatMap { genresOf(it) }.distinct().sorted(),
            )
        }.onEach { snapshot ->
            _uiState.update {
                it.copy(
                    selectedState = selectedState.value,
                    entries = snapshot.sorted,
                    totalHours = snapshot.totalHours,
                    totalGames = snapshot.totalGames,
                    availablePlatforms = snapshot.availablePlatforms,
                    availableGenres = snapshot.availableGenres,
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)

        // Persisted, per docs/02-PRODUCT-SPEC.md §1. The stored value is a bare string, so an
        // enum constant that no longer exists (a downgrade, or a mode we drop later) falls back
        // to the default rather than crashing on `valueOf`.
        userPreferencesRepository.pileViewMode
            .map { stored -> stored?.let { name -> PileViewMode.entries.firstOrNull { it.name == name } } }
            .onEach { stored -> if (stored != null) viewMode.value = stored }
            .launchIn(viewModelScope)

        viewMode.onEach { vm -> _uiState.update { it.copy(viewMode = vm) } }.launchIn(viewModelScope)
        sort.onEach { s -> _uiState.update { it.copy(sort = s) } }.launchIn(viewModelScope)
        platformFilter.onEach { p -> _uiState.update { it.copy(platformFilter = p) } }.launchIn(viewModelScope)
        genreFilter.onEach { g -> _uiState.update { it.copy(genreFilter = g) } }.launchIn(viewModelScope)
        lengthBucketFilter.onEach { l -> _uiState.update { it.copy(lengthBucketFilter = l) } }.launchIn(viewModelScope)
        hoursPerWeek.onEach { hpw -> _uiState.update { it.copy(hoursPerWeek = hpw) } }.launchIn(viewModelScope)
        swapPrompt.onEach { prompt -> _uiState.update { it.copy(swapPrompt = prompt) } }.launchIn(viewModelScope)

        userPreferencesRepository.isStackSwipeHintSeen
            .onEach { seen -> showStackHint.value = !seen }
            .launchIn(viewModelScope)
        showStackHint.onEach { show -> _uiState.update { it.copy(showStackHint = show) } }.launchIn(viewModelScope)
    }

    private data class RawSnapshot(
        val sorted: List<PileEntryWithGame>,
        val totalHours: Int,
        val totalGames: Int,
        val availablePlatforms: List<String>,
        val availableGenres: List<String>,
    )

    private fun platformsOf(entry: PileEntryWithGame): List<String> = decodeStringList(entry.platformsJson)
    private fun genresOf(entry: PileEntryWithGame): List<String> = decodeStringList(entry.genresJson)

    private fun decodeStringList(jsonStr: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(jsonStr) }.getOrDefault(emptyList())

    private fun fitsLengthBucket(entry: PileEntryWithGame, bucket: LengthBucket): Boolean {
        val hours = entry.estimatedHours ?: return false
        val fitsMax = bucket.maxHours == null || hours <= bucket.maxHours
        return hours >= bucket.minHours && fitsMax
    }

    private fun sortEntries(entries: List<PileEntryWithGame>, sortOrder: PileSort): List<PileEntryWithGame> =
        when (sortOrder) {
            PileSort.DATE_ADDED -> entries.sortedByDescending { it.addedAt }
            PileSort.TITLE -> entries.sortedBy { it.name.lowercase() }
            PileSort.LENGTH_SHORT_FIRST -> entries.sortedBy { it.estimatedHours ?: Int.MAX_VALUE }
            PileSort.RATING -> entries // rating not denormalized onto PileEntryWithGame yet
            PileSort.PLATFORM -> entries.sortedBy { it.ownedPlatform ?: "" }
            PileSort.RELEASE_DATE -> entries.sortedByDescending { it.addedAt }
        }

    fun selectTab(pileState: PileState) {
        selectedState.value = pileState
    }

    fun setViewMode(mode: PileViewMode) {
        viewMode.value = mode
        viewModelScope.launch { userPreferencesRepository.setPileViewMode(mode.name) }
    }

    /**
     * Called the first time the user actually flicks the stack. Hides the hint immediately and
     * remembers it, so the teach never comes back — and never blocks the gesture itself, which
     * is why it's fire-and-forget rather than something the view waits on.
     */
    fun markStackHintSeen() {
        if (!showStackHint.value) return
        showStackHint.value = false
        viewModelScope.launch { userPreferencesRepository.setStackSwipeHintSeen() }
    }

    fun setSort(newSort: PileSort) {
        sort.value = newSort
    }

    fun setPlatformFilter(platform: String?) {
        platformFilter.value = if (platformFilter.value == platform) null else platform
    }

    fun setGenreFilter(genre: String?) {
        genreFilter.value = if (genreFilter.value == genre) null else genre
    }

    fun setLengthBucketFilter(bucket: LengthBucket?) {
        lengthBucketFilter.value = if (lengthBucketFilter.value == bucket) null else bucket
    }

    fun setHoursPerWeek(hpw: Float) {
        hoursPerWeek.value = hpw.coerceAtLeast(0.5f)
    }

    /**
     * Attempts to move an entry into NOW PLAYING. If the cap is already full, surfaces a
     * [SwapPrompt] instead of failing silently — docs/02-PRODUCT-SPEC.md §1.
     */
    fun moveToPlaying(entryId: Long) {
        viewModelScope.launch {
            val playingEntries = pileDao.getByState(PileState.PLAYING)
            if (playingEntries.size >= NOW_PLAYING_CAP) {
                swapPrompt.value = SwapPrompt(entryId, playingEntries)
                return@launch
            }
            setState(entryId, PileState.PLAYING)
        }
    }

    fun confirmSwap(swapOutEntryId: Long) {
        val prompt = swapPrompt.value ?: return
        viewModelScope.launch {
            setState(swapOutEntryId, PileState.BACKLOG)
            setState(prompt.incomingEntryId, PileState.PLAYING)
            swapPrompt.value = null
        }
    }

    fun dismissSwapPrompt() {
        swapPrompt.value = null
    }

    fun retire(entryId: Long) = viewModelScope.launch { setState(entryId, PileState.DROPPED) }

    /**
     * Take a game out of the pile altogether — the undo for an accidental add, which the app had
     * no answer for at all: a tester who shared in the wrong game could only RETIRE it, which
     * files it under a decision they never made and counts toward the "retire 10 games" trophy.
     *
     * Sweeps the two tables that reference a game by id but have no foreign key to enforce it,
     * so nothing is left pointing at a game that isn't in the pile any more. The cached `games`
     * row is deliberately kept: it's shared cache, not user data, and dropping it would force a
     * network round trip if the same game is ever added back or surfaced in DISCOVER.
     */
    fun removeFromPile(entryId: Long) = viewModelScope.launch {
        val entry = pileDao.getById(entryId) ?: return@launch
        pileDao.deleteById(entryId)
        stackDao.removeFromAllStacks(entry.gameId)
        rankingDao.deleteByGameId(entry.gameId)
    }

    fun complete(entryId: Long) = viewModelScope.launch { setState(entryId, PileState.COMPLETED) }

    fun backToBacklog(entryId: Long) = viewModelScope.launch { setState(entryId, PileState.BACKLOG) }

    fun wishlist(entryId: Long) = viewModelScope.launch { setState(entryId, PileState.WISHLIST) }

    /**
     * Routes a chosen target state to the right transition. NOW PLAYING is deliberately not
     * just a `setState` — it has to go through [moveToPlaying] so the cap-of-3 swap prompt
     * still fires (docs/02-PRODUCT-SPEC.md §1).
     */
    fun moveTo(entryId: Long, target: PileState) {
        when (target) {
            PileState.PLAYING -> moveToPlaying(entryId)
            PileState.BACKLOG -> backToBacklog(entryId)
            PileState.COMPLETED -> complete(entryId)
            PileState.DROPPED -> retire(entryId)
            PileState.WISHLIST -> wishlist(entryId)
        }
    }

    suspend fun addGame(gameId: Long, source: AddSource, state: PileState = PileState.BACKLOG) {
        if (pileDao.findByGameId(gameId) != null) return
        pileDao.insert(
            PileEntryEntity(
                gameId = gameId,
                state = state,
                addedAt = System.currentTimeMillis(),
                source = source,
            )
        )
    }

    private suspend fun setState(entryId: Long, newState: PileState) {
        val entry = pileDao.getById(entryId) ?: return
        val now = System.currentTimeMillis()
        pileDao.update(
            entry.copy(
                state = newState,
                startedAt = if (newState == PileState.PLAYING) now else entry.startedAt,
                finishedAt = if (newState == PileState.COMPLETED) now else entry.finishedAt,
                droppedAt = if (newState == PileState.DROPPED) now else entry.droppedAt,
            )
        )
    }
}
