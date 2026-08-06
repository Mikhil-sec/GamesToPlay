package com.mikhilnaika.continueapp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { COLD_OPEN, PILE_SIZE, SEED, FIRST_DRAW }

enum class PileSizeAnswer { UNDER_20, TWENTY_TO_100, DONT_ASK }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.COLD_OPEN,
    val pileSizeAnswer: PileSizeAnswer? = null,
)

/**
 * Drives the under-40-second onboarding flow — docs/02-PRODUCT-SPEC.md §8. No account
 * creation, no permissions requested up front; RevenueCat's anonymous app-user ID is already
 * flowing from `Purchases.configure()` in ContinueApplication by the time this runs.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state

    fun onColdOpenContinue() {
        _state.value = _state.value.copy(step = OnboardingStep.PILE_SIZE)
    }

    fun onPileSizeAnswered(answer: PileSizeAnswer) {
        _state.value = _state.value.copy(pileSizeAnswer = answer, step = OnboardingStep.SEED)
    }

    fun onSeedStepDone() {
        _state.value = _state.value.copy(step = OnboardingStep.FIRST_DRAW)
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingComplete()
            onDone()
        }
    }
}
