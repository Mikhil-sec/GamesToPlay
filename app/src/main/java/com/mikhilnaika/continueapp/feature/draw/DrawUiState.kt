package com.mikhilnaika.continueapp.feature.draw

import com.mikhilnaika.continueapp.core.data.Mood
import com.mikhilnaika.continueapp.core.data.TimeBudget
import com.mikhilnaika.continueapp.core.util.GameFacet

enum class DrawPhase { DIALS, GATE, DEALING, CARDS, DONE, EMPTY_PILE }

enum class SwipeVerdict { PLAYING_IT, NOT_TONIGHT, SAVE_FOR_LATER, RETIRE }

data class DrawUiState(
    val phase: DrawPhase = DrawPhase.DIALS,
    val timeBudget: TimeBudget = TimeBudget.TWO_HOURS,
    val mood: Mood = Mood.STORY,
    val availablePlatforms: List<String> = emptyList(),
    val selectedPlatforms: Set<String> = emptySet(),
    /**
     * The GENRE dial — the same [GameFacet] vocabulary PILE filters on and STATS charts.
     *
     * DRAW had TIME, MOOD and PLATFORM and nothing else, so "give me a horror game" was
     * expressible on PILE and not on the machine that actually picks what to play. A closed
     * tester filed that as the filters being inconsistent "across categories and the draw
     * button", which it was: they were two different vocabularies, one of which didn't have
     * genres in it at all.
     *
     * Only facets actually present in the BACKLOG are offered — a dial that can only ever
     * return nothing is worse than no dial.
     */
    val availableFacets: List<GameFacet> = emptyList(),
    val selectedFacets: Set<GameFacet> = emptySet(),
    val leverPulled: Boolean = false,
    val picks: List<DrawPick> = emptyList(),
    val currentCardIndex: Int = 0,
    val loosenedMessage: String? = null,
    val isPro: Boolean = false,
    val coinBalance: Int = 0,
    val gateCountdown: Int = 9,
    val gateBusy: Boolean = false,
    /** What the gate is waiting on while [gateBusy] — "VERIFYING…" reads very differently from a bare spinner. */
    val gateStatus: String? = null,
    val gateError: String? = null,
    val lastVerdictGameName: String? = null,
    val lastVerdict: SwipeVerdict? = null,
) {
    val currentPick: DrawPick? get() = picks.getOrNull(currentCardIndex)
}
