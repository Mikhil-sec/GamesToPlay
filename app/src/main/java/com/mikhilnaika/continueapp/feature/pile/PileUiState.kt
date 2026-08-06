package com.mikhilnaika.continueapp.feature.pile

import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame

enum class PileSort {
    DATE_ADDED, TITLE, LENGTH_SHORT_FIRST, RATING, PLATFORM, RELEASE_DATE
}

enum class LengthBucket(val maxHours: Int?) {
    UNDER_5(5), FIVE_TO_15(15), FIFTEEN_TO_40(40), OVER_40(null)
}

/**
 * Shown when a 4th game is pushed into NOW PLAYING — docs/02-PRODUCT-SPEC.md §1: "Your
 * cabinet only fits 3. What are you swapping out?"
 */
data class SwapPrompt(
    val incomingEntryId: Long,
    val currentlyPlaying: List<PileEntryWithGame>,
)

data class PileUiState(
    val selectedState: PileState = PileState.BACKLOG,
    val entries: List<PileEntryWithGame> = emptyList(),
    val sort: PileSort = PileSort.DATE_ADDED,
    val platformFilter: String? = null,
    val genreFilter: String? = null,
    val totalHours: Int = 0,
    val totalGames: Int = 0,
    val hoursPerWeek: Float = 6f,
    val swapPrompt: SwapPrompt? = null,
    val isLoading: Boolean = true,
) {
    /** "412 HRS · 87 GAMES · FINISHED BY 2029" — docs/02-PRODUCT-SPEC.md §1. */
    val timeBudgetFinishCopy: String
        get() {
            if (totalHours <= 0 || hoursPerWeek <= 0f) return "FINISHED BY NOW"
            val weeksNeeded = totalHours / hoursPerWeek
            val monthsNeeded = weeksNeeded / 4.345f
            return when {
                monthsNeeded < 6 -> {
                    val month = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.MONTH, monthsNeeded.toInt())
                    }
                    val monthName = month.getDisplayName(
                        java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault()
                    )
                    "FINISHED BY $monthName"
                }
                monthsNeeded > 120 -> {
                    val year = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.MONTH, monthsNeeded.toInt())
                    }.get(java.util.Calendar.YEAR)
                    "FINISHED BY $year. CONSIDER RETIRING SOME."
                }
                else -> {
                    val year = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.MONTH, monthsNeeded.toInt())
                    }.get(java.util.Calendar.YEAR)
                    "FINISHED BY $year"
                }
            }
        }
}
