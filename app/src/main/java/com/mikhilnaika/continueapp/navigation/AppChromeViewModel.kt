package com.mikhilnaika.continueapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
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
}
