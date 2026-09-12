package com.mikhilnaika.continueapp.feature.pile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import com.mikhilnaika.continueapp.core.data.clearRewardKey
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import com.mikhilnaika.continueapp.core.share.ShareLauncher
import com.mikhilnaika.continueapp.core.share.ShareLinks
import com.mikhilnaika.continueapp.core.data.dao.StackDao
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import com.mikhilnaika.continueapp.core.util.GameFacet
import com.mikhilnaika.continueapp.core.util.estimatedHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** docs/02-PRODUCT-SPEC.md §1 — NOW PLAYING is hard-capped at 3. */
const val NOW_PLAYING_CAP = 3

@HiltViewModel
class PileViewModel @Inject constructor(
    private val pileDao: PileDao,
    private val stackDao: StackDao,
    private val rankingDao: RankingDao,
    private val billingRepository: BillingRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val shareLauncher: ShareLauncher,
) : ViewModel() {

    /**
     * Recommend one game to a friend.
     *
     * Text, not a rendered card, and that is the interesting choice. The link is an Android
     * App Link served by the Worker, so WhatsApp/Discord/iMessage fetch it and unfurl it into a
     * preview card carrying the game's real key art — the artwork arrives without us drawing or
     * uploading anything, and the *recipient* gets something tappable rather than a JPEG of a
     * recommendation. Tapping it opens CONTINUE? straight onto that game, or the store if they
     * don't have it yet.
     *
     * Fires immediately with no coroutine and no render, because the whole point of this
     * affordance is that it costs nothing to use.
     */
    fun shareGame(gameId: Long, gameName: String, campaign: ShareLinks.Campaign) {
        shareLauncher.shareText(ShareLinks.messageFor(campaign, gameName, gameId))
    }

    private val selectedState = MutableStateFlow(PileState.BACKLOG)
    private val viewMode = MutableStateFlow(PileUiState().viewMode)
    private val filters = MutableStateFlow(PileFilters())
    private val hoursPerWeek = MutableStateFlow(UserPreferencesRepository.DEFAULT_HOURS_PER_WEEK)
    private val swapPrompt = MutableStateFlow<SwapPrompt?>(null)
    private val showStackHint = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(PileUiState())
    val state: StateFlow<PileUiState> = _uiState

    init {
        // One query for the whole pile rather than one per tab. The tab split is a cheap
        // in-memory partition of a list that is tens of rows long, and doing it here is what
        // lets the filter chips be built from *every* game while their counts describe only the
        // tab on screen — see PileFiltering.facetOptions.
        combine(
            pileDao.observeAll(),
            selectedState,
            filters,
            hoursPerWeek,
        ) { all, tab, activeFilters, hpw ->
            val inTab = all.filter { it.state == tab }
            val visible = PileFiltering.apply(inTab, activeFilters)
            PileUiState(
                selectedState = tab,
                viewMode = viewMode.value,
                entries = visible,
                sort = activeFilters.sort,
                facetFilters = activeFilters.facets,
                platformFilters = activeFilters.platforms,
                lengthBucketFilter = activeFilters.length,
                availableFacets = PileFiltering.facetOptions(all, inTab, activeFilters.facets),
                availablePlatforms = PileFiltering.platformOptions(all, inTab, activeFilters.platforms),
                availableLengths = PileFiltering.lengthOptions(inTab),
                totalHours = visible.sumOf { it.estimatedHours ?: 0 },
                totalGames = visible.size,
                hoursPerWeek = hpw,
                swapPrompt = swapPrompt.value,
                showStackHint = showStackHint.value,
                isLoading = false,
            )
        }.onEach { snapshot -> _uiState.value = snapshot }.launchIn(viewModelScope)

        // Persisted, per docs/02-PRODUCT-SPEC.md §1. The stored value is a bare string, so an
        // enum constant that no longer exists (a downgrade, or a mode we drop later) falls back
        // to the default rather than crashing on `valueOf`.
        userPreferencesRepository.pileViewMode
            .map { stored -> stored?.let { name -> PileViewMode.entries.firstOrNull { it.name == name } } }
            .onEach { stored -> if (stored != null) viewMode.value = stored }
            .launchIn(viewModelScope)

        viewMode.onEach { vm -> _uiState.update { it.copy(viewMode = vm) } }.launchIn(viewModelScope)
        swapPrompt.onEach { prompt -> _uiState.update { it.copy(swapPrompt = prompt) } }.launchIn(viewModelScope)

        userPreferencesRepository.hoursPerWeek
            .onEach { hours -> hoursPerWeek.value = hours }
            .launchIn(viewModelScope)

        userPreferencesRepository.isStackSwipeHintSeen
            .onEach { seen -> showStackHint.value = !seen }
            .launchIn(viewModelScope)
        showStackHint.onEach { show -> _uiState.update { it.copy(showStackHint = show) } }.launchIn(viewModelScope)
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

    fun setSort(newSort: PileSort) = filters.update { it.copy(sort = newSort) }

    fun togglePlatformFilter(platform: String) = filters.update {
        it.copy(platforms = it.platforms.toggle(platform))
    }

    fun toggleFacetFilter(facet: GameFacet) = filters.update {
        it.copy(facets = it.facets.toggle(facet))
    }

    /** Length stays single-select: the buckets are contiguous, so "under 5h or 40h+" is noise. */
    fun setLengthBucketFilter(bucket: LengthBucket?) = filters.update {
        it.copy(length = if (it.length == bucket) null else bucket)
    }

    fun clearFilters() = filters.update { PileFilters(sort = it.sort) }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

    /**
     * The one input to "FINISHED BY 2029" — and, until now, the one input nothing could set.
     *
     * Written straight through to DataStore rather than held here: the flow above collects it
     * back, so the value on screen is always the value on disk and a half-finished write can't
     * leave the two disagreeing.
     */
    fun setHoursPerWeek(hpw: Float) {
        viewModelScope.launch { userPreferencesRepository.setHoursPerWeek(hpw) }
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
     * Rewrites when a game was started and finished — the editor behind BACKDATE, and the whole
     * answer to "I cleared this before I had the app".
     *
     * Setting a cleared date files the game under CLEARED, because a game with a finish date
     * that isn't in CLEARED is a contradiction the rest of the app would have to keep
     * apologising for (THIS YEAR counts it, the Credits Roll ordinal doesn't, STATS disagrees
     * with both). The dialog says so before it writes.
     *
     * **A backdated clear pays no coins, and burns the reward key so it can never pay any.**
     * Clearing a game is worth +5 because it took months; logging a game you finished in 2019
     * takes four taps, and paying for it would be a faster faucet than the repeatable-clear loop
     * this release just closed. See `CoinLedger.markClaimed`.
     */
    fun setDates(entryId: Long, startedAt: Long?, finishedAt: Long?) = viewModelScope.launch {
        val entry = pileDao.getById(entryId) ?: return@launch
        if (finishedAt != null && entry.state != PileState.COMPLETED) {
            billingRepository.markRewardClaimed(clearRewardKey(entry.gameId))
        }
        pileDao.update(
            entry.copy(
                startedAt = startedAt,
                finishedAt = finishedAt,
                state = if (finishedAt != null) PileState.COMPLETED else entry.state,
            )
        )
    }

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
