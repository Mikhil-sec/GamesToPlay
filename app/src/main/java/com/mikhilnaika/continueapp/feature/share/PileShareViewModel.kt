package com.mikhilnaika.continueapp.feature.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PileShareUiState(val totalHours: Int = 0, val totalGames: Int = 0, val isLoading: Boolean = true)

/** Backs the "THE PILE" share card — docs/02-PRODUCT-SPEC.md §6: "412 HOURS. 87 GAMES. SEND HELP." */
@HiltViewModel
class PileShareViewModel @Inject constructor(
    private val pileDao: PileDao,
) : ViewModel() {

    private val _state = MutableStateFlow(PileShareUiState())
    val state: StateFlow<PileShareUiState> = _state

    init {
        viewModelScope.launch {
            val backlog = pileDao.getByState(PileState.BACKLOG)
            _state.value = PileShareUiState(
                totalHours = backlog.sumOf { it.playtimeHoursNormally ?: 0 },
                totalGames = backlog.size,
                isLoading = false,
            )
        }
    }
}
