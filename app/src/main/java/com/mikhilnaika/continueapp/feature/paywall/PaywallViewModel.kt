package com.mikhilnaika.continueapp.feature.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.ads.FreePlay
import com.mikhilnaika.continueapp.core.ads.FreePlayOutcome
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import com.mikhilnaika.continueapp.core.billing.ProTier
import com.mikhilnaika.continueapp.core.billing.PurchaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallUiState(
    val isLoading: Boolean = true,
    val tiers: List<ProTier> = emptyList(),
    val selectedTierId: String? = null,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val error: String? = null,
    val purchased: Boolean = false,
    /** Non-null while a FREE PLAY ad is loading or verifying — what to show instead of the link. */
    val freePlayStatus: String? = null,
) {
    val selectedTier: ProTier? get() = tiers.firstOrNull { it.id == selectedTierId }

    /** No tiers and not still loading means there is genuinely nothing to sell. */
    val isUnavailable: Boolean get() = !isLoading && tiers.isEmpty()
}

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val freePlay: FreePlay,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state

    init {
        viewModelScope.launch {
            val tiers = billingRepository.proTiers()
            _state.update {
                it.copy(
                    isLoading = false,
                    tiers = tiers,
                    // Lifetime is preselected on purpose. Monthly exists largely to make
                    // Lifetime look obvious (docs/07-SUBMISSION-KIT.md §Pricing rationale), so
                    // the default should be the one we actually want people to take — and the
                    // one that isn't a recurring charge they have to remember to cancel.
                    selectedTierId = (tiers.firstOrNull { tier -> tier.isLifetime } ?: tiers.firstOrNull())?.id,
                )
            }
        }
    }

    fun selectTier(tierId: String) {
        _state.update { it.copy(selectedTierId = tierId, error = null) }
    }

    fun purchase(activity: Activity) {
        val tierId = _state.value.selectedTierId ?: return
        if (_state.value.isPurchasing) return
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, error = null) }
            when (val result = billingRepository.purchaseTier(activity, tierId)) {
                is PurchaseResult.Success -> _state.update { it.copy(isPurchasing = false, purchased = true) }
                // A cancel is a choice, not a failure — showing an error for it is the classic
                // way a paywall starts feeling hostile.
                is PurchaseResult.UserCancelled -> _state.update { it.copy(isPurchasing = false) }
                is PurchaseResult.Error -> _state.update { it.copy(isPurchasing = false, error = result.message) }
            }
        }
    }

    fun restore() {
        if (_state.value.isRestoring) return
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, error = null) }
            when (val result = billingRepository.restorePurchases()) {
                is PurchaseResult.Success -> _state.update { it.copy(isRestoring = false, purchased = true) }
                is PurchaseResult.UserCancelled -> _state.update { it.copy(isRestoring = false) }
                is PurchaseResult.Error -> _state.update { it.copy(isRestoring = false, error = result.message) }
            }
        }
    }

    /**
     * Try-before-you-buy, paid for with attention: one ad, an hour of the real thing. Success
     * closes the paywall exactly as a purchase does, because for the next hour it *is* one.
     */
    fun startFreePlay(activity: Activity) {
        if (_state.value.freePlayStatus != null) return
        viewModelScope.launch {
            _state.update { it.copy(error = null, freePlayStatus = "LOADING AD…") }
            val outcome = freePlay.start(activity) { status -> _state.update { it.copy(freePlayStatus = status) } }
            _state.update {
                when (outcome) {
                    is FreePlayOutcome.Started -> it.copy(freePlayStatus = null, purchased = true)
                    is FreePlayOutcome.Failed -> it.copy(freePlayStatus = null, error = outcome.message)
                }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
