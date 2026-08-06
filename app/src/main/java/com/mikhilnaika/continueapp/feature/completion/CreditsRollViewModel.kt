package com.mikhilnaika.continueapp.feature.completion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** docs/02-PRODUCT-SPEC.md §4 — awarded on completion, "completion should pay". */
private const val COMPLETION_COIN_REWARD = 5

data class CreditsRollUiState(
    val isLoading: Boolean = true,
    val gameId: Long = 0,
    val gameName: String = "",
    val backgroundUrl: String? = null,
    val daysInThePile: Long = 0,
    val startedLabel: String = "",
    val finishedLabel: String = "",
    val clearOrdinal: Int = 1,
    val clearYear: Int = Calendar.getInstance().get(Calendar.YEAR),
)

@HiltViewModel
class CreditsRollViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pileDao: PileDao,
    private val gameDao: GameDao,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])

    private val _state = MutableStateFlow(CreditsRollUiState())
    val state: StateFlow<CreditsRollUiState> = _state

    init {
        viewModelScope.launch {
            val entry = pileDao.getById(entryId) ?: return@launch
            val game = gameDao.get(entry.gameId)
            val now = System.currentTimeMillis()

            if (entry.state != PileState.COMPLETED) {
                pileDao.update(
                    entry.copy(
                        state = PileState.COMPLETED,
                        finishedAt = now,
                        startedAt = entry.startedAt ?: entry.addedAt,
                    )
                )
                billingRepository.earnCoins(COMPLETION_COIN_REWARD, "game_cleared")
            }

            val startedAt = entry.startedAt ?: entry.addedAt
            val finishedAt = entry.finishedAt ?: now
            val yearStart = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val ordinal = pileDao.countCompletedSince(yearStart).coerceAtLeast(1)
            val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

            _state.update {
                it.copy(
                    isLoading = false,
                    gameId = entry.gameId,
                    gameName = game?.name ?: "",
                    backgroundUrl = game?.backgroundUrl ?: game?.coverUrl,
                    daysInThePile = TimeUnit.MILLISECONDS.toDays(finishedAt - entry.addedAt),
                    startedLabel = dateFormat.format(Date(startedAt)),
                    finishedLabel = dateFormat.format(Date(finishedAt)),
                    clearOrdinal = ordinal,
                )
            }
        }
    }
}
