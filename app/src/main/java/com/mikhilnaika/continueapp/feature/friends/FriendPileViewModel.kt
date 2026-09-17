package com.mikhilnaika.continueapp.feature.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.GameCacheRepository
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.FriendGameRow
import com.mikhilnaika.continueapp.core.data.dao.FriendRankRow
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.entity.FriendEntity
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import com.mikhilnaika.continueapp.core.friends.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A friend's pile, one tab at a time — the same five states as yours, plus their top ten. */
enum class FriendTab(val label: String, val state: PileState?) {
    PLAYING("NOW PLAYING", PileState.PLAYING),
    BACKLOG("THE PILE", PileState.BACKLOG),
    CLEARED("CLEARED", PileState.COMPLETED),
    TOP("HIGH SCORES", null),
    RETIRED("RETIRED", PileState.DROPPED),
    WANTED("WANTED", PileState.WISHLIST),
}

data class FriendPileUiState(
    val isLoaded: Boolean = false,
    /** Null once loaded means the friend was removed (possibly from another screen). */
    val friend: FriendEntity? = null,
    val tab: FriendTab = FriendTab.PLAYING,
    val games: List<FriendGameRow> = emptyList(),
    val ranks: List<FriendRankRow> = emptyList(),
    val counts: Map<FriendTab, Int> = emptyMap(),
    /** How many of this tab's games the link couldn't carry. */
    val notInLink: Int = 0,
    val inCommon: Int = 0,
    val isHydrating: Boolean = false,
)

@HiltViewModel
class FriendPileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val friendRepository: FriendRepository,
    private val pileDao: PileDao,
    private val gameCache: GameCacheRepository,
) : ViewModel() {

    private val friendId: Long = checkNotNull(savedStateHandle["friendId"])

    /** Null until the user picks a tab; until then the best default is derived from the data. */
    private val selectedTab = MutableStateFlow<FriendTab?>(null)
    private val hydrating = MutableStateFlow(true)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val state: StateFlow<FriendPileUiState> = combine(
        friendRepository.observeFriend(friendId),
        friendRepository.observeGames(friendId),
        friendRepository.observeRanks(friendId),
        selectedTab,
        hydrating,
    ) { friend, games, ranks, picked, isHydrating ->
        val byState = games.groupBy { it.state }
        val counts = FriendTab.entries.associateWith { tab ->
            if (tab.state == null) ranks.size else totalFor(friend, tab)
        }
        val tab = picked ?: defaultTab(counts)
        val tabGames = tab.state?.let { byState[it] }.orEmpty()
        FriendPileUiState(
            isLoaded = true,
            friend = friend,
            tab = tab,
            games = tabGames,
            ranks = ranks,
            counts = counts,
            notInLink = if (tab.state == null) 0 else (counts.getValue(tab) - tabGames.size).coerceAtLeast(0),
            inCommon = games.count { it.inMyPile },
            isHydrating = isHydrating,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendPileUiState())

    init {
        // Catches up anything the import sheet couldn't fetch — it may have been opened offline.
        viewModelScope.launch {
            try {
                friendRepository.hydrate(friendId)
            } finally {
                hydrating.value = false
            }
        }
    }

    fun selectTab(tab: FriendTab) {
        selectedTab.value = tab
    }

    /** "I want that one too." Straight into your own pile, as a backlog game. */
    fun addToMyPile(gameId: Long, name: String, coverUrl: String?) {
        viewModelScope.launch {
            if (pileDao.findByGameId(gameId) != null) {
                _message.value = "$name is already on your pile."
                return@launch
            }
            gameCache.cacheMinimal(gameId, name, coverUrl)
            pileDao.insert(
                PileEntryEntity(
                    gameId = gameId,
                    state = PileState.BACKLOG,
                    addedAt = System.currentTimeMillis(),
                    source = AddSource.FRIEND,
                )
            )
            _message.value = "$name added to your pile."
        }
    }

    fun rename(name: String) {
        viewModelScope.launch {
            if (!friendRepository.rename(friendId, name)) _message.value = "That name is empty."
        }
    }

    /** The screen leaves by itself once the friend's row disappears from [state]. */
    fun remove() {
        viewModelScope.launch { friendRepository.remove(friendId) }
    }

    fun messageShown() {
        _message.value = null
    }

    private fun totalFor(friend: FriendEntity?, tab: FriendTab): Int = when (tab) {
        FriendTab.PLAYING -> friend?.playingTotal
        FriendTab.BACKLOG -> friend?.backlogTotal
        FriendTab.CLEARED -> friend?.clearedTotal
        FriendTab.RETIRED -> friend?.retiredTotal
        FriendTab.WANTED -> friend?.wantedTotal
        FriendTab.TOP -> 0
    } ?: 0

    /** Open on whatever says most about them: what they're playing, else their pile, else anything. */
    private fun defaultTab(counts: Map<FriendTab, Int>): FriendTab =
        listOf(FriendTab.PLAYING, FriendTab.BACKLOG, FriendTab.CLEARED, FriendTab.TOP)
            .firstOrNull { (counts[it] ?: 0) > 0 }
            ?: FriendTab.BACKLOG
}
