package com.mikhilnaika.continueapp.feature.draw

import com.mikhilnaika.continueapp.core.data.Mood
import com.mikhilnaika.continueapp.core.data.TimeBudget

enum class DrawPhase { DIALS, GATE, DEALING, CARDS, DONE, EMPTY_PILE }

enum class SwipeVerdict { PLAYING_IT, NOT_TONIGHT, SAVE_FOR_LATER, RETIRE }

data class DrawUiState(
    val phase: DrawPhase = DrawPhase.DIALS,
    val timeBudget: TimeBudget = TimeBudget.TWO_HOURS,
    val mood: Mood = Mood.STORY,
    val availablePlatforms: List<String> = emptyList(),
    val selectedPlatforms: Set<String> = emptySet(),
    val leverPulled: Boolean = false,
    val picks: List<DrawPick> = emptyList(),
    val currentCardIndex: Int = 0,
    val loosenedMessage: String? = null,
    val isPro: Boolean = false,
    val coinBalance: Int = 0,
    val gateCountdown: Int = 9,
    val gateBusy: Boolean = false,
    val gateError: String? = null,
    val lastVerdictGameName: String? = null,
    val lastVerdict: SwipeVerdict? = null,
) {
    val currentPick: DrawPick? get() = picks.getOrNull(currentCardIndex)
}
