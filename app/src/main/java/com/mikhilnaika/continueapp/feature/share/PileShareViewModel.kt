package com.mikhilnaika.continueapp.feature.share

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.friends.FriendRepository
import com.mikhilnaika.continueapp.core.share.ShareCardRenderer
import com.mikhilnaika.continueapp.core.share.ShareLinks
import com.mikhilnaika.continueapp.core.util.estimatedHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How many covers the card's backdrop grid can show — 4 columns × 4 rows. */
private const val WALL_SIZE = 16

data class PileShareUiState(
    val totalHours: Int = 0,
    val totalGames: Int = 0,
    val card: Bitmap? = null,
    val isLoading: Boolean = true,
    /** True between the tap and the share sheet appearing — the link is signed in that gap. */
    val isSharing: Boolean = false,
)

/**
 * Backs "SHARE YOUR PILE" — the THE PILE card (docs/02-PRODUCT-SPEC.md §6: "412 HOURS. 87 GAMES.
 * SEND HELP.") plus the link that lets a friend with CONTINUE? follow the pile behind it.
 *
 * The card is rendered here rather than in the composable, which is a change from how this
 * screen used to work. Rendering it in a `produceState` re-ran the whole thing — including
 * decoding sixteen covers — on every recomposition that happened to re-key it, and made the
 * bitmap's lifetime the composition's rather than the screen's.
 */
@HiltViewModel
class PileShareViewModel @Inject constructor(
    private val pileDao: PileDao,
    private val renderer: ShareCardRenderer,
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PileShareUiState())
    val state: StateFlow<PileShareUiState> = _state

    init {
        viewModelScope.launch {
            val backlog = pileDao.getByState(PileState.BACKLOG)
            val hours = backlog.sumOf { it.estimatedHours ?: 0 }
            val games = backlog.size
            // The longest games first, so the wall is the stuff actually weighing the pile
            // down — and so the same person gets a recognisably stable card rather than a
            // reshuffle every time they open the screen.
            val covers = backlog
                .sortedByDescending { it.estimatedHours ?: 0 }
                .mapNotNull { it.coverUrl }
                .take(WALL_SIZE)

            // Numbers first, art second: the preview appears the moment the query returns
            // instead of waiting on sixteen image decodes.
            _state.update { it.copy(totalHours = hours, totalGames = games, isLoading = true) }
            val card = renderer.renderPileCard(hours, games, covers)
            _state.update { it.copy(card = card, isLoading = false) }
        }
    }

    /**
     * Signs a fresh snapshot at the moment of sharing, not when the screen opened, so the link
     * always matches the pile as it is when it leaves the phone — and so each share is a newer
     * version that friends' copies will accept.
     */
    fun share() {
        val current = _state.value
        val card = current.card ?: return
        if (current.isSharing) return
        _state.update { it.copy(isSharing = true) }
        viewModelScope.launch {
            try {
                renderer.share(
                    bitmap = card,
                    fileName = "the_pile",
                    message = ShareLinks.messageForPile(
                        totalHours = current.totalHours,
                        totalGames = current.totalGames,
                        pileLink = friendRepository.buildMyPileLink(),
                    ),
                )
            } finally {
                _state.update { it.copy(isSharing = false) }
            }
        }
    }
}
