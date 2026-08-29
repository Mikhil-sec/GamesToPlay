package com.mikhilnaika.continueapp.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class StatsUiState(
    val scope: StatsScope = StatsScope.ALL,
    val snapshot: StatsSnapshot = StatsSnapshot(),
    val isLoading: Boolean = true,
)

/**
 * STATS reads the whole pile once and slices it in memory, exactly as PILE does.
 *
 * Deliberately its own ViewModel rather than a second face on `PileViewModel`: they're separate
 * nav destinations, so `hiltViewModel()` would hand out a different instance anyway, and the
 * scope selector here means something different from PILE's tab (this one has an EVERYTHING
 * option, which a list of games can't have).
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    pileDao: PileDao,
) : ViewModel() {

    private val scope = MutableStateFlow(StatsScope.ALL)

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state

    init {
        combine(pileDao.observeAll(), scope) { all, selected ->
            val scoped = selected.state?.let { s -> all.filter { it.state == s } } ?: all
            StatsUiState(
                scope = selected,
                snapshot = PileStats.compute(scoped, all),
                isLoading = false,
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    fun selectScope(newScope: StatsScope) {
        scope.value = newScope
    }
}
