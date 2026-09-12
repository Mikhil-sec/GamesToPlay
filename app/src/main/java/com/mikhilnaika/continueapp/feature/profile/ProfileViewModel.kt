package com.mikhilnaika.continueapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.mikhilnaika.continueapp.core.ads.ConsentManager
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.TimeBudget
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import com.mikhilnaika.continueapp.core.data.dao.DrawDao
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import com.mikhilnaika.continueapp.core.share.HighScoreLine
import com.mikhilnaika.continueapp.core.share.ShareCardRenderer
import com.mikhilnaika.continueapp.core.share.ShareLinks
import com.mikhilnaika.continueapp.core.util.Playtime
import com.mikhilnaika.continueapp.core.util.estimatedHours
import com.mikhilnaika.continueapp.feature.rank.PairwiseRanker
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

data class HighScoreEntry(
    val gameId: Long,
    val position: Int,
    val name: String,
    val coverUrl: String?,
    val verdict: String?,
    /** Estimated length, for the HIGH SCORES share card's right-hand column. */
    val hours: Int? = null,
)

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
    val isPro: Boolean = false,
    /** True while the HIGH SCORES card renders — the button says so rather than going dead. */
    val isPreparingShare: Boolean = false,
    /**
     * Whether to show the "AD PRIVACY CHOICES" row.
     *
     * False for most users — Google only requires the entry point where consent was actually
     * collected, so a user in Mauritius would otherwise get a settings row that opens an empty
     * form. It follows the SDK's own answer rather than a guess at the user's geography.
     */
    val privacyOptionsRequired: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val rankingDao: RankingDao,
    private val pileDao: PileDao,
    private val gameDao: GameDao,
    private val drawDao: DrawDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val renderer: ShareCardRenderer,
    private val consentManager: ConsentManager,
    billingRepository: com.mikhilnaika.continueapp.core.billing.BillingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    init {
        rankingDao.observeAll().onEach { rankings ->
            val entries = rankings.mapIndexed { index, ranking ->
                val game = gameDao.get(ranking.gameId)
                HighScoreEntry(
                    gameId = ranking.gameId,
                    position = index + 1,
                    name = game?.name ?: "",
                    coverUrl = game?.coverUrl,
                    verdict = ranking.verdict,
                    hours = Playtime.estimateHours(
                        game?.playtimeHoursHastily,
                        game?.playtimeHoursNormally,
                        game?.playtimeHoursCompletely,
                    ),
                )
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

        billingRepository.isPro
            .onEach { pro -> _state.update { it.copy(isPro = pro) } }
            .launchIn(viewModelScope)

        consentManager.privacyOptionsRequired
            .onEach { required -> _state.update { it.copy(privacyOptionsRequired = required) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            loadThisYear()
            refreshTrophies()
        }
    }

    /**
     * HIGH SCORES as an arcade high-score table — docs/02-PRODUCT-SPEC.md §6, the card whose
     * whole job is to be argued with.
     *
     * Capped at ten rows because that is what an arcade leaderboard is, and because a
     * screenshot of forty games is a spreadsheet nobody reads.
     */
    fun shareHighScores() {
        val current = _state.value
        if (current.highScores.isEmpty() || current.isPreparingShare) return
        _state.update { it.copy(isPreparingShare = true) }
        viewModelScope.launch {
            try {
                val card = renderer.renderHighScoresCard(
                    current.highScores.take(HIGH_SCORE_CARD_ROWS).map {
                        HighScoreLine(name = it.name, hours = it.hours)
                    }
                )
                renderer.share(
                    bitmap = card,
                    fileName = "high_scores",
                    message = ShareLinks.messageForHighScores(current.highScores.firstOrNull()?.name),
                )
            } finally {
                _state.update { it.copy(isPreparingShare = false) }
            }
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
                    hoursCleared = completed.sumOf { e -> e.estimatedHours ?: 0 },
                    longestGameHours = completed.maxOfOrNull { e -> e.estimatedHours ?: 0 } ?: 0,
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

    /**
     * Nudge one game up or down the leaderboard.
     *
     * RANK places a game with up to five pairwise comparisons, which is a good way to *enter* a
     * ranking and a poor way to correct one — there was no way at all to say "no, that one's
     * higher", and a mis-tap during the comparisons was permanent. The arithmetic (including
     * what happens to the game's bucket when it crosses a boundary) is in
     * [PairwiseRanker.reorderedPlacements]; this just supplies the current order and writes the
     * result back in one transaction.
     */
    fun moveHighScore(gameId: Long, delta: Int) = viewModelScope.launch {
        val current = rankingDao.getAll()
        val index = current.sortedBy { it.position }.indexOfFirst { it.gameId == gameId }
        if (index < 0) return@launch
        rankingDao.reorder(PairwiseRanker.reorderedPlacements(current, index, delta))
    }

    /**
     * Drop a game off the leaderboard without touching the pile.
     *
     * The game stays exactly where it is in CLEARED — this only says "I don't want it ranked",
     * which until now had no expression at all: the only way out of HIGH SCORES was REMOVE FROM
     * PILE, which erases the game entirely. The survivors are renumbered so the leaderboard has
     * no hole in it.
     */
    fun removeHighScore(gameId: Long) = viewModelScope.launch {
        val current = rankingDao.getAll()
        rankingDao.deleteByGameId(gameId)
        rankingDao.reorder(PairwiseRanker.withoutGame(current, gameId))
    }

    /**
     * Reopens Google's consent form so a user can change an answer they already gave.
     *
     * Required, not a courtesy: an app that collects consent in the EEA/UK/CH without offering
     * a persistent way to withdraw it is non-compliant with Google's own policy, however
     * correct the original dialog was.
     */
    fun showPrivacyOptions(activity: Activity) = consentManager.showPrivacyOptionsForm(activity)

    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { userPreferencesRepository.setHapticsEnabled(enabled) }
    fun setClipboardDetectionEnabled(enabled: Boolean) = viewModelScope.launch { userPreferencesRepository.setClipboardDetectionEnabled(enabled) }

    private companion object {
        /** An arcade leaderboard is ten rows. Anything longer stops being one. */
        const val HIGH_SCORE_CARD_ROWS = 10
    }
}
