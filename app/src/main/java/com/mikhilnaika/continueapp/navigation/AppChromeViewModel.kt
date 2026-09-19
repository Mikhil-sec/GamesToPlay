package com.mikhilnaika.continueapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * State for the persistent app chrome (the coin counter in the top bar).
 *
 * Lives at the nav-host level rather than inside each screen so the balance is visible
 * everywhere the economy is reachable — docs/02-PRODUCT-SPEC.md §3 makes coins the gate on
 * DRAW, and a currency the user can't see is a currency they won't spend.
 */
@HiltViewModel
class AppChromeViewModel @Inject constructor(
    billingRepository: BillingRepository,
) : ViewModel() {

    val coinBalance: StateFlow<Int> = billingRepository.coinBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isPro: StateFlow<Boolean> = billingRepository.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * When FREE PLAY ends, or null when it isn't running.
     *
     * FREE PLAY *is* the `pro` entitlement — RevenueCat grants the real thing, time-limited — so
     * it's recognised by its length rather than by a flag: nothing sold lasts under a week (the
     * trial is 7 days, Monthly a month, Lifetime forever), so PRO ending within [FREE_PLAY_WINDOW_MS]
     * can only be an ad reward.
     */
    val freePlayEndsAt: StateFlow<Long?> = combine(billingRepository.isPro, billingRepository.proExpiresAt) { pro, expiresAt ->
        expiresAt?.takeIf { pro && it - System.currentTimeMillis() in 0..FREE_PLAY_WINDOW_MS }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val coinDrops: SharedFlow<Int> = billingRepository.coinDrops

    private companion object {
        const val FREE_PLAY_WINDOW_MS = 2 * 60 * 60 * 1000L
    }
}
