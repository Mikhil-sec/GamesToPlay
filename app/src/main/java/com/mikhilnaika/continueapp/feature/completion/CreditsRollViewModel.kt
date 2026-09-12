package com.mikhilnaika.continueapp.feature.completion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.clearRewardKey
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import com.mikhilnaika.continueapp.core.network.GameDataSource
import com.mikhilnaika.continueapp.core.share.ShareCardRenderer
import com.mikhilnaika.continueapp.core.share.ShareLinks
import com.mikhilnaika.continueapp.core.util.Playtime
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

/**
 * docs/02-PRODUCT-SPEC.md §4 — awarded on completion, "completion should pay".
 *
 * **1, not 5 (changed 2026-09-12.)** The spec's 5 was set before the economy around it existed
 * and, once it did, it broke it: a DRAW re-roll costs 1, so clearing two games bought ten
 * re-rolls and a normal player never ran out of coins. A currency nobody runs out of gives
 * nobody a reason to watch a rewarded ad or buy Pro — which are the two things the coin exists
 * to motivate. The generous number was also convenient during closed testing, where the point
 * was getting testers *through* the paid surfaces rather than metering them.
 *
 * This deliberately makes clearing a game pay the same as watching one ad. That reads odd on
 * paper — forty hours versus thirty seconds — but the coin was never the reward for finishing
 * a game. The Credits Roll is. The coin is an acknowledgement, and pricing it as anything more
 * puts the app in the position of implying its own core loop is the grind.
 */
const val COMPLETION_COIN_REWARD = 1

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
    /**
     * Coins this clear actually paid — 0 when the game has already been paid for once.
     *
     * The screen prints this rather than a hardcoded "+5 COINS", because it is now possible for
     * a clear to pay nothing and the cinematic must not claim otherwise. See the reward-key
     * comment on [CreditsRollViewModel].
     */
    val coinsAwarded: Int = 0,
    /** Position on HIGH SCORES, if this game has been ranked. Null otherwise. */
    val allTimeRank: Int? = null,
    /** True while the CLEARED card is rendering, so the button can say so instead of nothing. */
    val isPreparingShare: Boolean = false,
)

@HiltViewModel
class CreditsRollViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pileDao: PileDao,
    private val gameDao: GameDao,
    private val gameDataSource: GameDataSource,
    private val billingRepository: BillingRepository,
    private val rankingDao: RankingDao,
    private val renderer: ShareCardRenderer,
) : ViewModel() {

    /** Held so [shareClear] doesn't re-read Room for art it already had. */
    private var coverUrl: String? = null
    private var estimatedHours: Int? = null

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])

    private val _state = MutableStateFlow(CreditsRollUiState())
    val state: StateFlow<CreditsRollUiState> = _state

    init {
        viewModelScope.launch {
            val entry = pileDao.getById(entryId) ?: return@launch
            val game = gameDao.get(entry.gameId)
            val now = System.currentTimeMillis()

            // The coin grant is keyed on the *game*, not on this transition.
            //
            // A closed tester found the loop: clear a game, move it back to THE PILE, clear it
            // again, +5 every time, forever. The guard below only ever stopped the *same*
            // Credits Roll paying twice, which was never the problem. Keying the reward on the
            // game closes it without punishing anyone — a real replay still gets the whole
            // cinematic, it just doesn't get paid for a second time.
            var awarded = 0
            if (entry.state != PileState.COMPLETED) {
                pileDao.update(
                    entry.copy(
                        state = PileState.COMPLETED,
                        finishedAt = now,
                        startedAt = entry.startedAt ?: entry.addedAt,
                    )
                )
                val paid = billingRepository.earnCoinsOnce(
                    clearRewardKey(entry.gameId),
                    COMPLETION_COIN_REWARD,
                    "game_cleared",
                )
                if (paid) awarded = COMPLETION_COIN_REWARD
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

            coverUrl = game?.coverUrl
            // The user's own logged hours beat the crowdsourced estimate whenever they exist —
            // a card that says "38 HOURS" about *your* clear should mean your 38 hours.
            estimatedHours = entry.hoursPlayed?.takeIf { it > 0f }?.toInt()
                ?: Playtime.estimateHours(
                    game?.playtimeHoursHastily,
                    game?.playtimeHoursNormally,
                    game?.playtimeHoursCompletely,
                )
            // `getAll` is ordered by position, so the index is the rank. Absent from the list
            // simply means "not ranked yet", which is the common case right after a clear —
            // the card then just omits the line rather than inventing a position.
            val rank = rankingDao.getAll()
                .indexOfFirst { it.gameId == entry.gameId }
                .takeIf { it >= 0 }
                ?.plus(1)

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
                    coinsAwarded = awarded,
                    allTimeRank = rank,
                )
            }

            if (game != null && game.backgroundUrl == null) backfillKeyArt(game.id)
        }
    }

    /**
     * Renders the CLEARED card and opens the chooser.
     *
     * This screen is where a share button always belonged and never was. The one share surface
     * the app had was THE PILE — a card about how much you *haven't* finished — reachable only
     * from a button on the pile screen. The moment somebody rolls credits on a game they've
     * been carrying for two years is the moment they want to tell someone, and until now the
     * app said nothing and offered nothing.
     *
     * Rendering happens on demand rather than up front: it decodes key art, and most Credits
     * Rolls are watched and dismissed without a share.
     */
    fun shareClear() {
        val current = _state.value
        if (current.isLoading || current.isPreparingShare) return
        _state.update { it.copy(isPreparingShare = true) }
        viewModelScope.launch {
            try {
                val card = renderer.renderClearedCard(
                    gameName = current.gameName,
                    keyArtUrl = current.backgroundUrl,
                    coverUrl = coverUrl,
                    hours = estimatedHours,
                    allTimeRank = current.allTimeRank,
                )
                renderer.share(
                    bitmap = card,
                    fileName = "cleared_${current.gameId}",
                    message = ShareLinks.messageFor(
                        ShareLinks.Campaign.CLEARED,
                        current.gameName,
                        current.gameId,
                    ),
                )
            } finally {
                // In a `finally` so a failed render or a missing chooser can't strand the
                // button in its disabled state for the life of the screen.
                _state.update { it.copy(isPreparingShare = false) }
            }
        }
    }

    /**
     * Fetches real landscape key art for a game stored before the Worker started returning it.
     *
     * The Credits Roll is the one screen that paints an image full-bleed, so falling back to a
     * 264px cover is very visible — it's the "pixelated" report from device testing. Games added
     * before 2026-08-14 have `backgroundUrl == null` in Room forever otherwise, since nothing
     * else re-reads a game once it's in the pile. Runs *after* the state emit so the cinematic
     * starts on time either way, and writes through to Room so it's a once-per-game cost.
     */
    private suspend fun backfillKeyArt(gameId: Long) {
        val fetched = gameDataSource.detail(gameId)?.backgroundUrl ?: return
        gameDao.updateBackgroundUrl(gameId, fetched)
        _state.update { it.copy(backgroundUrl = fetched) }
    }
}
