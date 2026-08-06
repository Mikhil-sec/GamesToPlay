package com.mikhilnaika.continueapp.feature.rank

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.RankBucket
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import com.mikhilnaika.continueapp.core.data.entity.RankingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * docs/02-PRODUCT-SPEC.md §5 "Pairwise placement" — the interactive binary search. Pure
 * insertion math lives in [PairwiseRanker]; this class owns the I/O and the wait-for-a-tap
 * step sequencing that math alone can't express.
 */
@HiltViewModel
class RankViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gameDao: GameDao,
    private val rankingDao: RankingDao,
) : ViewModel() {

    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])

    private val _state = MutableStateFlow(RankUiState())
    val state: StateFlow<RankUiState> = _state

    private var allRankings: List<RankingEntity> = emptyList()
    private var bucketMembers: List<RankingEntity> = emptyList()
    private var lo = 0
    private var hi = 0
    private var pendingPosition: Int? = null

    init {
        viewModelScope.launch {
            val game = gameDao.get(gameId)
            _state.update {
                it.copy(
                    phase = RankPhase.BUCKET_SELECT,
                    gameId = gameId,
                    gameName = game?.name ?: "",
                    coverUrl = game?.coverUrl,
                )
            }
        }
    }

    fun selectBucket(bucket: RankBucket) {
        viewModelScope.launch {
            allRankings = rankingDao.getAll()
            bucketMembers = PairwiseRanker.bucketMembers(allRankings, bucket)
            _state.update { it.copy(selectedBucket = bucket, comparisonsUsed = 0) }

            if (bucketMembers.isEmpty()) {
                finalizePosition(PairwiseRanker.globalPosition(allRankings, bucket, 0))
                return@launch
            }
            lo = 0
            hi = bucketMembers.size
            askNextComparison()
        }
    }

    private suspend fun askNextComparison() {
        val idx = PairwiseRanker.nextComparisonIndex(lo, hi)
        val opponentEntity = bucketMembers[idx]
        val opponentGame = gameDao.get(opponentEntity.gameId)
        _state.update {
            it.copy(
                phase = RankPhase.COMPARING,
                currentOpponent = RankOpponent(opponentEntity.gameId, opponentGame?.name ?: "", opponentGame?.coverUrl),
            )
        }
    }

    /** [newGameWon] — true if the user picked the game being ranked as more enjoyable. */
    fun chooseWinner(newGameWon: Boolean) {
        val bucket = _state.value.selectedBucket ?: return
        val idx = PairwiseRanker.nextComparisonIndex(lo, hi)
        if (newGameWon) hi = idx else lo = idx + 1
        val comparisons = _state.value.comparisonsUsed + 1
        _state.update { it.copy(comparisonsUsed = comparisons) }

        viewModelScope.launch {
            if (lo >= hi || comparisons >= MAX_COMPARISONS) {
                finalizePosition(PairwiseRanker.globalPosition(allRankings, bucket, lo))
            } else {
                askNextComparison()
            }
        }
    }

    private fun finalizePosition(position: Int) {
        pendingPosition = position
        _state.update { it.copy(phase = RankPhase.VERDICT, finalPosition = position) }
    }

    fun setVerdictText(text: String) = _state.update { it.copy(verdictText = text.take(140)) }
    fun setWouldReplay(value: Boolean) = _state.update { it.copy(wouldReplay = value) }

    fun save() {
        val bucket = _state.value.selectedBucket ?: return
        val position = pendingPosition ?: return
        viewModelScope.launch {
            rankingDao.shiftDown(position)
            rankingDao.upsert(
                RankingEntity(
                    gameId = gameId,
                    bucket = bucket,
                    position = position,
                    verdict = _state.value.verdictText.ifBlank { null },
                    wouldReplay = _state.value.wouldReplay,
                    rankedAt = System.currentTimeMillis(),
                )
            )
            _state.update { it.copy(phase = RankPhase.DONE) }
        }
    }
}
