package com.mikhilnaika.continueapp.feature.draw

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.ads.AdPlacement
import com.mikhilnaika.continueapp.core.ads.AdRepository
import com.mikhilnaika.continueapp.core.ads.AdReward
import com.mikhilnaika.continueapp.core.ads.FreePlay
import com.mikhilnaika.continueapp.core.ads.FreePlayOutcome
import com.mikhilnaika.continueapp.core.ads.AdResult
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import com.mikhilnaika.continueapp.core.billing.PurchaseResult
import com.mikhilnaika.continueapp.core.billing.SpendResult
import com.mikhilnaika.continueapp.core.data.Mood
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.TimeBudget
import com.mikhilnaika.continueapp.core.data.dao.DrawDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.entity.DrawEntity
import com.mikhilnaika.continueapp.core.util.GameFacet
import com.mikhilnaika.continueapp.core.util.facets
import com.mikhilnaika.continueapp.core.util.platforms
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import javax.inject.Inject

/** docs/02-PRODUCT-SPEC.md §3 "The economy of DRAW" — free users get 1 draw per day. */
private const val FREE_DRAWS_PER_DAY = 1
private const val COIN_COST_PER_CONTINUE = 1
private const val NO_AD = "No ad available right now — try again shortly."

@HiltViewModel
class DrawViewModel @Inject constructor(
    private val pileDao: PileDao,
    private val drawDao: DrawDao,
    private val billingRepository: BillingRepository,
    private val adRepository: AdRepository,
    private val freePlay: FreePlay,
) : ViewModel() {

    private val _state = MutableStateFlow(DrawUiState())
    val state: StateFlow<DrawUiState> = _state

    private val json = Json { ignoreUnknownKeys = true }
    private var countdownJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            billingRepository.isPro.collect { pro -> _state.update { it.copy(isPro = pro) } }
        }
        viewModelScope.launch {
            billingRepository.coinBalance.collect { balance -> _state.update { it.copy(coinBalance = balance) } }
        }
        loadDials()
    }

    /**
     * Fills the PLATFORM and GENRE dials from the games actually in the BACKLOG.
     *
     * Both are derived from the candidate pool rather than from a fixed list, so a dial can
     * never offer a setting that would return nothing. GENRE uses the same
     * [com.mikhilnaika.continueapp.core.util.GameTaxonomy] facets as PILE's filter chips —
     * closed testing reported the two as inconsistent, and they were: DRAW had no genre dial at
     * all, so the two screens narrowed the same pile by different things.
     */
    private fun loadDials() {
        viewModelScope.launch {
            val candidates = pileDao.getDrawCandidates()
            val platforms = candidates.flatMap { it.platforms }.distinct().sorted()
            val present = candidates.flatMapTo(mutableSetOf()) { it.facets }
            _state.update {
                it.copy(
                    availablePlatforms = platforms,
                    availableFacets = GameFacet.entries.filter { facet -> facet in present },
                    // A dial that no longer offers a setting must not keep filtering by it.
                    selectedFacets = it.selectedFacets.intersect(present),
                    selectedPlatforms = it.selectedPlatforms.intersect(platforms.toSet()),
                )
            }
        }
    }

    fun setTimeBudget(timeBudget: TimeBudget) = _state.update { it.copy(timeBudget = timeBudget) }
    fun setMood(mood: Mood) = _state.update { it.copy(mood = mood) }
    fun togglePlatform(platform: String) = _state.update {
        val next = if (platform in it.selectedPlatforms) it.selectedPlatforms - platform else it.selectedPlatforms + platform
        it.copy(selectedPlatforms = next)
    }

    fun toggleFacet(facet: GameFacet) = _state.update {
        val next = if (facet in it.selectedFacets) it.selectedFacets - facet else it.selectedFacets + facet
        it.copy(selectedFacets = next)
    }

    /** The lever release — docs/02-PRODUCT-SPEC.md §3. Gated by the daily free-draw limit. */
    fun pullLever() {
        if (_state.value.phase != DrawPhase.DIALS) return
        viewModelScope.launch {
            val drawsToday = drawDao.since(startOfTodayMillis()).size
            if (_state.value.isPro || drawsToday < FREE_DRAWS_PER_DAY) {
                deal()
            } else {
                openGate()
            }
        }
    }

    private fun openGate() {
        _state.update { it.copy(phase = DrawPhase.GATE, gateCountdown = 9, gateError = null) }
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                for (n in 9 downTo 0) {
                    _state.update { it.copy(gateCountdown = n) }
                    delay(700)
                }
            }
        }
    }

    fun dismissGate() {
        countdownJob?.cancel()
        _state.update { it.copy(phase = DrawPhase.DIALS, gateError = null) }
    }

    /**
     * GATE action: INSERT COIN — watch a rewarded ad for +1 coin (it doesn't itself continue).
     *
     * The coin is granted by **RevenueCat**, not by this app: the ad's reward rule adds 1 COIN
     * server-side once AdMob's verification callback checks out, and [BillingRepository.syncStoreCoins]
     * reads it back in. Only if verification can't complete — offline, or AdMob running late —
     * does the app pay the coin itself. Someone who sat through an ad has earned it, and the
     * price of that honesty is at most one coin counted twice if a late verification lands
     * after all. A coin gates one extra draw; that's the right side to err on.
     */
    fun insertCoin(activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(gateBusy = true, gateError = null, gateStatus = "LOADING AD…") }
            val ad = adRepository.load(AdPlacement.COIN)
            if (ad == null) {
                _state.update { it.copy(gateBusy = false, gateStatus = null, gateError = NO_AD) }
                return@launch
            }
            val result = adRepository.show(activity, ad)
            _state.update { it.copy(gateStatus = "VERIFYING WITH REVENUECAT…") }
            when (result) {
                is AdResult.Verified -> {
                    val paidCoins = result.rewards.any { it is AdReward.Currency }
                    if (!paidCoins) {
                        // Verified, but the unit's rule paid something other than coins —
                        // a dashboard misconfiguration the user shouldn't pay for.
                        billingRepository.earnCoins(1, "draw_continue_ad_unmatched")
                    } else if (billingRepository.syncStoreCoins() == 0) {
                        // Granted server-side but not readable yet; one short retry, and if
                        // it's still not there the next launch's sync will bring it in.
                        delay(1_500)
                        billingRepository.syncStoreCoins()
                    }
                    _state.update { it.copy(gateBusy = false, gateStatus = null) }
                }
                is AdResult.Unverified -> {
                    billingRepository.earnCoins(1, "draw_continue_ad_unverified")
                    _state.update { it.copy(gateBusy = false, gateStatus = null) }
                }
                is AdResult.NoFill -> _state.update { it.copy(gateBusy = false, gateStatus = null, gateError = NO_AD) }
                is AdResult.UserCancelled -> _state.update {
                    it.copy(gateBusy = false, gateStatus = null, gateError = "Closed early — no coin this time.")
                }
                is AdResult.Error -> _state.update { it.copy(gateBusy = false, gateStatus = null, gateError = result.message) }
            }
        }
    }

    /**
     * GATE action: FREE PLAY — see [FreePlay]. Nothing here deals the cards: once PRO is active,
     * [onReturnedFromPaywall] (keyed on `isPro` in DrawScreen) sees the gate open and deals, the
     * same path a purchase takes.
     */
    fun freePlay(activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(gateBusy = true, gateError = null) }
            val outcome = freePlay.start(activity) { status -> _state.update { it.copy(gateStatus = status) } }
            _state.update {
                it.copy(
                    gateBusy = false,
                    gateStatus = null,
                    gateError = (outcome as? FreePlayOutcome.Failed)?.message,
                )
            }
        }
    }

    /** GATE action: spend an existing coin to continue this draw. */
    fun useCoin() {
        viewModelScope.launch {
            _state.update { it.copy(gateBusy = true, gateError = null) }
            when (val result = billingRepository.spendCoins(COIN_COST_PER_CONTINUE, "draw_continue")) {
                is SpendResult.Success -> {
                    countdownJob?.cancel()
                    _state.update { it.copy(gateBusy = false) }
                    deal()
                }
                is SpendResult.InsufficientFunds -> _state.update {
                    it.copy(gateBusy = false, gateError = "Not enough coins — INSERT COIN to earn one.")
                }
                is SpendResult.Error -> _state.update { it.copy(gateBusy = false, gateError = result.message) }
            }
        }
    }

    /**
     * Called when the user comes back from the paywall, so a purchase made there takes effect
     * immediately instead of leaving them staring at the gate they just paid to remove.
     *
     * The purchase itself lives in `feature/paywall` now: GO PRO used to buy
     * `currentOfferingPackage()` — whatever happened to be first in the offering — with no price
     * shown and no choice between Monthly and Lifetime. That was defensible when no products
     * existed; now that they do, the dashboard-configured paywall is both the honest UI and the
     * one that can be re-priced without an APK.
     */
    fun onReturnedFromPaywall() {
        if (!_state.value.isPro || _state.value.phase != DrawPhase.GATE) return
        viewModelScope.launch {
            countdownJob?.cancel()
            _state.update { it.copy(gateBusy = false, gateError = null) }
            deal()
        }
    }

    private suspend fun deal() {
        countdownJob?.cancel()
        val candidates = pileDao.getDrawCandidates()
        if (candidates.isEmpty()) {
            _state.update { it.copy(phase = DrawPhase.EMPTY_PILE) }
            return
        }
        _state.update { it.copy(phase = DrawPhase.DEALING) }
        val result = DrawSelector.select(
            candidates = candidates,
            timeBudget = _state.value.timeBudget,
            mood = _state.value.mood,
            platforms = _state.value.selectedPlatforms,
            facets = _state.value.selectedFacets,
        )
        val now = System.currentTimeMillis()
        drawDao.insert(
            DrawEntity(
                drawnAt = now,
                timeBudget = _state.value.timeBudget,
                mood = _state.value.mood,
                platformsJson = json.encodeToString(_state.value.selectedPlatforms.toList()),
                gameIdsJson = json.encodeToString(result.picks.map { it.candidate.gameId }),
            )
        )
        result.picks.forEach { pileDao.markDrawn(it.candidate.entryId, now) }
        delay(DEAL_ANIMATION_MS) // let the cards finish being ejected before they're interactive
        _state.update {
            it.copy(
                phase = DrawPhase.CARDS,
                picks = result.picks,
                currentCardIndex = 0,
                loosenedMessage = result.loosenedMessage,
            )
        }
    }

    fun applyVerdict(verdict: SwipeVerdict) {
        val pick = _state.value.currentPick ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            when (verdict) {
                SwipeVerdict.PLAYING_IT -> movePlaying(pick.candidate.entryId)
                SwipeVerdict.NOT_TONIGHT -> pileDao.snooze(pick.candidate.entryId, now + 14L * 24 * 60 * 60 * 1000)
                SwipeVerdict.SAVE_FOR_LATER -> pileDao.pin(pick.candidate.entryId, now + 365L * 24 * 60 * 60 * 1000)
                SwipeVerdict.RETIRE -> {
                    val entry = pileDao.getById(pick.candidate.entryId)
                    if (entry != null) {
                        pileDao.update(entry.copy(state = PileState.DROPPED, droppedAt = now))
                    }
                }
            }
            val nextIndex = _state.value.currentCardIndex + 1
            _state.update {
                it.copy(
                    currentCardIndex = nextIndex,
                    lastVerdictGameName = pick.candidate.name,
                    lastVerdict = verdict,
                    phase = if (nextIndex >= it.picks.size) DrawPhase.DONE else DrawPhase.CARDS,
                )
            }
        }
    }

    private suspend fun movePlaying(entryId: Long) {
        val playingCount = pileDao.getByState(PileState.PLAYING).size
        val entry = pileDao.getById(entryId) ?: return
        if (playingCount < com.mikhilnaika.continueapp.feature.pile.NOW_PLAYING_CAP) {
            pileDao.update(entry.copy(state = PileState.PLAYING, startedAt = System.currentTimeMillis()))
        }
        // If the cabinet is already full, the game stays in the pile rather than silently
        // failing — the swap decision belongs on PILE (docs/02-PRODUCT-SPEC.md §1), not here.
    }

    fun playAgain() {
        _state.update {
            it.copy(
                phase = DrawPhase.DIALS,
                picks = emptyList(),
                currentCardIndex = 0,
                loosenedMessage = null,
                lastVerdictGameName = null,
                lastVerdict = null,
            )
        }
        loadDials()
    }

    private fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
