package com.mikhilnaika.continueapp.feature.pile

import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame

enum class PileSort {
    DATE_ADDED, TITLE, LENGTH_SHORT_FIRST, RATING, PLATFORM, RELEASE_DATE
}

/**
 * docs/02-PRODUCT-SPEC.md §1 "Views". STACK is the signature receding-3D view and the spec's
 * default; GRID is the practical one and LIST the dense one. Declared in the order the view
 * toggle cycles through them.
 */
enum class PileViewMode { STACK, GRID, LIST }

enum class LengthBucket(val label: String, val minHours: Int, val maxHours: Int?) {
    UNDER_5("UNDER 5H", 0, 5),
    FIVE_TO_15("5-15H", 5, 15),
    FIFTEEN_TO_40("15-40H", 15, 40),
    OVER_40("40H+", 40, null),
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
    val viewMode: PileViewMode = PileViewMode.STACK,
    val entries: List<PileEntryWithGame> = emptyList(),
    val sort: PileSort = PileSort.DATE_ADDED,
    val platformFilter: String? = null,
    val genreFilter: String? = null,
    val lengthBucketFilter: LengthBucket? = null,
    val availablePlatforms: List<String> = emptyList(),
    val availableGenres: List<String> = emptyList(),
    val totalHours: Int = 0,
    val totalGames: Int = 0,
    val hoursPerWeek: Float = 6f,
    val swapPrompt: SwapPrompt? = null,
    /** One-time teach for STACK's drag gesture — see `UserPreferencesRepository`. */
    val showStackHint: Boolean = false,
    val isLoading: Boolean = true,
) {
    /** How many filters are narrowing the list — surfaced on the collapsed SORT & FILTER chip
     * so a hidden filter can never silently explain an empty-looking pile. */
    val activeFilterCount: Int
        get() = listOfNotNull(platformFilter, genreFilter, lengthBucketFilter).size

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
