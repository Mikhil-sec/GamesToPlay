package com.mikhilnaika.continueapp.feature.draw

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.ads.AdRepository
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

@HiltViewModel
class DrawViewModel @Inject constructor(
    private val pileDao: PileDao,
    private val drawDao: DrawDao,
    private val billingRepository: BillingRepository,
    private val adRepository: AdRepository,
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
        loadAvailablePlatforms()
    }

    private fun loadAvailablePlatforms() {
        viewModelScope.launch {
            val candidates = pileDao.getDrawCandidates()
            val platforms = candidates
                .flatMap { runCatching { json.decodeFromString<List<String>>(it.platformsJson) }.getOrDefault(emptyList()) }
                .distinct()
                .sorted()
            _state.update { it.copy(availablePlatforms = platforms) }
        }
    }

    fun setTimeBudget(timeBudget: TimeBudget) = _state.update { it.copy(timeBudget = timeBudget) }
    fun setMood(mood: Mood) = _state.update { it.copy(mood = mood) }
    fun togglePlatform(platform: String) = _state.update {
        val next = if (platform in it.selectedPlatforms) it.selectedPlatforms - platform else it.selectedPlatforms + platform
        it.copy(selectedPlatforms = next)
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

    /** GATE action: watch a rewarded ad for +1 coin (does not itself continue the draw). */
    fun insertCoin(activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(gateBusy = true, gateError = null) }
            val ad = adRepository.loadCoinAd()
            if (ad == null) {
                _state.update { it.copy(gateBusy = false, gateError = "No ad available right now — try again shortly.") }
                return@launch
            }
            when (val result = adRepository.show(activity, ad)) {
                is AdResult.Granted -> {
                    when (val earn = billingRepository.earnCoins(1, "draw_continue_ad")) {
                        is SpendResult.Success -> _state.update { it.copy(gateBusy = false) }
                        is SpendResult.Error -> _state.update { it.copy(gateBusy = false, gateError = earn.message) }
                        else -> _state.update { it.copy(gateBusy = false) }
                    }
                }
                is AdResult.NoFill -> _state.update { it.copy(gateBusy = false, gateError = "No ad available right now — try again shortly.") }
                is AdResult.UserCancelled -> _state.update { it.copy(gateBusy = false) }
                is AdResult.Error -> _state.update { it.copy(gateBusy = false, gateError = result.message) }
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

    /** GATE action: RevenueCat paywall — see docs/09-PENDING-INPUTS.md, no real offering yet. */
    fun goPro(activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(gateBusy = true, gateError = null) }
            val pkg = billingRepository.currentOfferingPackage()
            if (pkg == null) {
                _state.update {
                    it.copy(gateBusy = false, gateError = "GO PRO isn't live yet — Play Store products land soon.")
                }
                return@launch
            }
            when (val result = billingRepository.purchase(activity, pkg)) {
                is PurchaseResult.Success -> {
                    countdownJob?.cancel()
                    _state.update { it.copy(gateBusy = false) }
                    deal()
                }
                is PurchaseResult.UserCancelled -> _state.update { it.copy(gateBusy = false) }
                is PurchaseResult.Error -> _state.update { it.copy(gateBusy = false, gateError = result.message) }
            }
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
        delay(900) // lets the CRT power-on flicker play before cards are interactive
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
        loadAvailablePlatforms()
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
