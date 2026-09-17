package com.mikhilnaika.continueapp.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.entity.FriendEntity
import com.mikhilnaika.continueapp.core.friends.FriendRepository
import com.mikhilnaika.continueapp.core.friends.PileDiff
import com.mikhilnaika.continueapp.core.friends.PileLinkOutcome
import com.mikhilnaika.continueapp.core.friends.PileSnapshot
import com.mikhilnaika.continueapp.core.offline.OfflineGameIndex
import com.mikhilnaika.continueapp.core.util.IgdbImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The headline numbers of a pile someone just sent, shown before anything is saved. */
data class PilePreview(
    val backlog: Int,
    val playing: Int,
    val cleared: Int,
    val wanted: Int,
    val retired: Int,
    val sharedAtMillis: Long,
    val covers: List<String>,
)

sealed interface FriendImportState {
    data object Loading : FriendImportState
    data object Invalid : FriendImportState
    data object OwnPile : FriendImportState

    data class NewFriend(
        val preview: PilePreview,
        val existingFriends: List<FriendEntity>,
        val atLimit: Boolean,
        /** Set when a save was refused, so the sheet can say why instead of doing nothing. */
        val error: String? = null,
    ) : FriendImportState

    data class Saved(val friendId: Long, val name: String) : FriendImportState
    data class Updated(val friendId: Long, val name: String, val diff: PileDiff) : FriendImportState
    data class AlreadyCurrent(val friendId: Long, val name: String) : FriendImportState
    data class Older(val friendId: Long, val name: String) : FriendImportState
}

@HiltViewModel
class FriendImportViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val offlineIndex: OfflineGameIndex,
) : ViewModel() {

    private val _state = MutableStateFlow<FriendImportState>(FriendImportState.Loading)
    val state: StateFlow<FriendImportState> = _state

    /** Held only in memory until the user names the friend; never written before that tap. */
    private var pending: PileSnapshot? = null
    private var started = false
    private var saving = false

    fun open(payload: String?) {
        // The Activity calls this from onCreate, which also runs after a rotation — the link
        // must be inspected once, or an update would be applied and then reported as current.
        if (started) return
        started = true
        viewModelScope.launch {
            _state.value = when (val outcome = friendRepository.inspect(payload)) {
                PileLinkOutcome.Invalid -> FriendImportState.Invalid
                PileLinkOutcome.OwnPile -> FriendImportState.OwnPile
                is PileLinkOutcome.NewFriend -> {
                    pending = outcome.snapshot
                    FriendImportState.NewFriend(
                        preview = preview(outcome.snapshot),
                        existingFriends = outcome.existingFriends,
                        atLimit = outcome.atLimit,
                    )
                }
                is PileLinkOutcome.Updated -> FriendImportState.Updated(outcome.friendId, outcome.name, outcome.diff)
                is PileLinkOutcome.AlreadyCurrent -> FriendImportState.AlreadyCurrent(outcome.friendId, outcome.name)
                is PileLinkOutcome.Older -> FriendImportState.Older(outcome.friendId, outcome.name)
            }
        }
    }

    fun addFriend(name: String) {
        val snapshot = pending ?: return
        val current = _state.value as? FriendImportState.NewFriend ?: return
        if (saving) return
        if (FriendRepository.cleanName(name) == null) {
            _state.value = current.copy(error = "Give them a name you'll recognise.")
            return
        }
        saving = true
        viewModelScope.launch {
            val id = friendRepository.addFriend(snapshot, name)
            saving = false
            _state.value = if (id != null) {
                pending = null
                FriendImportState.Saved(id, FriendRepository.cleanName(name).orEmpty())
            } else {
                current.copy(
                    atLimit = true,
                    error = "You're following ${FriendRepository.MAX_FRIENDS} piles already — remove one in FRIENDS first.",
                )
            }
        }
    }

    fun replaceFriend(friend: FriendEntity) {
        val snapshot = pending ?: return
        val current = _state.value as? FriendImportState.NewFriend ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            val id = friendRepository.replaceFriend(friend.id, snapshot)
            saving = false
            _state.value = if (id != null) {
                pending = null
                FriendImportState.Saved(id, friend.name)
            } else {
                current.copy(error = "Couldn't update ${friend.name} — try adding them as a new friend.")
            }
        }
    }

    /**
     * Covers from the bundled index, so the preview shows real games before anything is saved or
     * fetched. What they're playing first — it's the most "them" part of a pile.
     */
    private suspend fun preview(snapshot: PileSnapshot): PilePreview {
        val ids = listOf(PileState.PLAYING, PileState.COMPLETED, PileState.BACKLOG)
            .flatMap { snapshot.section(it).gameIds }
        val covers = runCatching { offlineIndex.lookup(ids) }.getOrDefault(emptyList())
            .mapNotNull { record -> record.coverImageId?.let { IgdbImage.coverUrl(it) } }
            .take(PREVIEW_COVERS)
        return PilePreview(
            backlog = snapshot.section(PileState.BACKLOG).total,
            playing = snapshot.section(PileState.PLAYING).total,
            cleared = snapshot.section(PileState.COMPLETED).total,
            wanted = snapshot.section(PileState.WISHLIST).total,
            retired = snapshot.section(PileState.DROPPED).total,
            sharedAtMillis = minOf(snapshot.sharedAtSeconds * 1000, System.currentTimeMillis()),
            covers = covers,
        )
    }

    private companion object {
        const val PREVIEW_COVERS = 5
    }
}
