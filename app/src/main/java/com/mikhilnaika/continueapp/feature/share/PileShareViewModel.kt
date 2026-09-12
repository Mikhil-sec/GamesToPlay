package com.mikhilnaika.continueapp.feature.share

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.share.ShareCardRenderer
import com.mikhilnaika.continueapp.core.share.ShareLinks
import com.mikhilnaika.continueapp.core.util.estimatedHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How many covers the card's backdrop grid can show — 4 columns × 4 rows. */
private const val WALL_SIZE = 16

data class PileShareUiState(
    val totalHours: Int = 0,
    val totalGames: Int = 0,
    val card: Bitmap? = null,
    val isLoading: Boolean = true,
)

/**
 * Backs the "THE PILE" share card — docs/02-PRODUCT-SPEC.md §6: "412 HOURS. 87 GAMES. SEND HELP."
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
            _state.value = PileShareUiState(totalHours = hours, totalGames = games, isLoading = true)
            _state.value = PileShareUiState(
                totalHours = hours,
                totalGames = games,
                card = renderer.renderPileCard(hours, games, covers),
                isLoading = false,
            )
        }
    }

    fun share() {
        val card = _state.value.card ?: return
        viewModelScope.launch {
            renderer.share(
                bitmap = card,
                fileName = "the_pile",
                message = ShareLinks.messageForPile(_state.value.totalHours, _state.value.totalGames),
            )
        }
    }
}
