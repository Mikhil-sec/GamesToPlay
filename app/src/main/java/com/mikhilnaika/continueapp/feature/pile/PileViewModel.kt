package com.mikhilnaika.continueapp.feature.pile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** docs/02-PRODUCT-SPEC.md §1 — NOW PLAYING is hard-capped at 3. */
const val NOW_PLAYING_CAP = 3

@HiltViewModel
class PileViewModel @Inject constructor(
    private val pileDao: PileDao,
) : ViewModel() {

    private val selectedState = MutableStateFlow(PileState.BACKLOG)
    private val sort = MutableStateFlow(PileSort.DATE_ADDED)
    private val hoursPerWeek = MutableStateFlow(6f)
    private val swapPrompt = MutableStateFlow<SwapPrompt?>(null)

    private val _uiState = MutableStateFlow(PileUiState())
    val state: StateFlow<PileUiState> = _uiState

    init {
        viewModelScope.launch {
            selectedState.collect { pileState ->
                launch {
                    pileDao.observeByState(pileState).collect { entries ->
                        applyEntries(pileState, entries)
                    }
                }
            }
        }
        viewModelScope.launch {
            sort.collect { s -> _uiState.update { it.copy(entries = sortEntries(it.entries, s), sort = s) } }
        }
        viewModelScope.launch {
            hoursPerWeek.collect { hpw -> _uiState.update { it.copy(hoursPerWeek = hpw) } }
        }
        viewModelScope.launch {
            swapPrompt.collect { prompt -> _uiState.update { it.copy(swapPrompt = prompt) } }
        }
    }

    private fun applyEntries(pileState: PileState, entries: List<PileEntryWithGame>) {
        val sorted = sortEntries(entries, sort.value)
        val totalHours = entries.sumOf { (it.playtimeHoursNormally ?: 0) }
        _uiState.update {
            it.copy(
                selectedState = pileState,
                entries = sorted,
                totalHours = totalHours,
                totalGames = entries.size,
                isLoading = false,
            )
        }
    }

    private fun sortEntries(entries: List<PileEntryWithGame>, sortOrder: PileSort): List<PileEntryWithGame> =
        when (sortOrder) {
            PileSort.DATE_ADDED -> entries.sortedByDescending { it.addedAt }
            PileSort.TITLE -> entries.sortedBy { it.name.lowercase() }
            PileSort.LENGTH_SHORT_FIRST -> entries.sortedBy { it.playtimeHoursNormally ?: Int.MAX_VALUE }
            PileSort.RATING -> entries // rating not denormalized onto PileEntryWithGame yet
            PileSort.PLATFORM -> entries.sortedBy { it.ownedPlatform ?: "" }
            PileSort.RELEASE_DATE -> entries.sortedByDescending { it.addedAt }
        }

    fun selectTab(pileState: PileState) {
        selectedState.value = pileState
    }

    fun setSort(newSort: PileSort) {
        sort.value = newSort
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

    fun complete(entryId: Long) = viewModelScope.launch { setState(entryId, PileState.COMPLETED) }

    fun backToBacklog(entryId: Long) = viewModelScope.launch { setState(entryId, PileState.BACKLOG) }

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
