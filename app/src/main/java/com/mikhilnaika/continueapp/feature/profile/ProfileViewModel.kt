package com.mikhilnaika.continueapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.TimeBudget
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import com.mikhilnaika.continueapp.core.data.dao.DrawDao
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HighScoreEntry(val position: Int, val name: String, val coverUrl: String?, val verdict: String?)

data class Trophy(val id: String, val label: String, val description: String, val unlocked: Boolean)

data class ThisYearStats(
    val gamesCleared: Int = 0,
    val hoursCleared: Int = 0,
    val longestGameHours: Int = 0,
    val fastestClearDays: Long? = null,
    val currentStreakDays: Int = 0,
)

data class ProfileUiState(
    val highScores: List<HighScoreEntry> = emptyList(),
    val thisYear: ThisYearStats = ThisYearStats(),
    val trophies: List<Trophy> = emptyList(),
    val hapticsEnabled: Boolean = true,
    val clipboardDetectionEnabled: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val rankingDao: RankingDao,
    private val pileDao: PileDao,
    private val gameDao: GameDao,
    private val drawDao: DrawDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    init {
        rankingDao.observeAll().onEach { rankings ->
            val entries = rankings.mapIndexed { index, ranking ->
                val game = gameDao.get(ranking.gameId)
                HighScoreEntry(index + 1, game?.name ?: "", game?.coverUrl, ranking.verdict)
            }
            _state.update { it.copy(highScores = entries) }
            refreshTrophies()
        }.launchIn(viewModelScope)

        combine(
            userPreferencesRepository.isHapticsEnabled,
            userPreferencesRepository.isClipboardDetectionEnabled,
        ) { haptics, clipboard -> haptics to clipboard }
            .onEach { (haptics, clipboard) ->
                _state.update { it.copy(hapticsEnabled = haptics, clipboardDetectionEnabled = clipboard) }
            }.launchIn(viewModelScope)

        viewModelScope.launch {
            loadThisYear()
            refreshTrophies()
        }
    }

    private suspend fun loadThisYear() {
        val yearStart = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val completed = pileDao.getByState(PileState.COMPLETED).filter { (it.finishedAt ?: 0) >= yearStart }
        val fastestDays = completed.mapNotNull { entry ->
            val started = entry.startedAt ?: return@mapNotNull null
            val finished = entry.finishedAt ?: return@mapNotNull null
            (finished - started) / (1000L * 60 * 60 * 24)
        }.minOrNull()

        val draws = drawDao.since(yearStart)
        val streak = currentStreakDays(draws.map { it.drawnAt })

        _state.update {
            it.copy(
                thisYear = ThisYearStats(
                    gamesCleared = completed.size,
                    hoursCleared = completed.sumOf { e -> e.playtimeHoursNormally ?: 0 },
                    longestGameHours = completed.maxOfOrNull { e -> e.playtimeHoursNormally ?: 0 } ?: 0,
                    fastestClearDays = fastestDays,
                    currentStreakDays = streak,
                )
            )
        }
    }

    private fun currentStreakDays(drawTimestamps: List<Long>): Int {
        if (drawTimestamps.isEmpty()) return 0
        val days = drawTimestamps.map { it / (1000L * 60 * 60 * 24) }.toSortedSet().toList().reversed()
        val today = System.currentTimeMillis() / (1000L * 60 * 60 * 24)
        if (days.first() < today - 1) return 0 // streak broken if no draw yesterday or today
        var streak = 1
        for (i in 1 until days.size) {
            if (days[i - 1] - days[i] == 1L) streak++ else break
        }
        return streak
    }

    private suspend fun refreshTrophies() {
        val allDraws = drawDao.since(0L)
        val completedCount = pileDao.countByState(PileState.COMPLETED)
        val droppedCount = pileDao.countByState(PileState.DROPPED)
        val rankedCount = rankingDao.getAll().size
        val hadAllNighter = allDraws.any { it.timeBudget == TimeBudget.ALL_NIGHT }

        _state.update {
            it.copy(
                trophies = listOf(
                    Trophy("first_continue", "FIRST CONTINUE", "Pull the lever for the first time", allDraws.isNotEmpty()),
                    Trophy("pile_slayer", "PILE SLAYER", "Clear 10 games", completedCount >= 10),
                    Trophy("critic", "CRITIC", "Rank 25 games", rankedCount >= 25),
                    Trophy("spring_cleaning", "SPRING CLEANING", "Retire 10 games", droppedCount >= 10),
                    Trophy("night_shift", "NIGHT SHIFT", "Draw for A WHOLE WEEKEND at 2am — ok, just draw ALL NIGHT once", hadAllNighter),
                ),
            )
        }
    }

    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { userPreferencesRepository.setHapticsEnabled(enabled) }
    fun setClipboardDetectionEnabled(enabled: Boolean) = viewModelScope.launch { userPreferencesRepository.setClipboardDetectionEnabled(enabled) }
}
