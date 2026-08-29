package com.mikhilnaika.continueapp.feature.pile

import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.util.GameFacet

/**
 * PILE's sort options, each carrying the label the chip shows.
 *
 * Three of these were previously declared here and offered nowhere, and two of *those* did
 * nothing when selected — `RATING` returned the list untouched and `RELEASE_DATE` sorted by
 * date *added*. Both now work (the columns they need are on `PileEntryWithGame`), and the chip
 * row is generated from this enum, so an option can no longer exist without being reachable.
 */
enum class PileSort(val label: String) {
    DATE_ADDED("RECENT"),
    TITLE("A-Z"),
    LENGTH_SHORT_FIRST("SHORTEST"),
    RATING("TOP RATED"),
    RELEASE_DATE("NEWEST"),
    PLATFORM("PLATFORM"),
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
 * One filter chip: the thing being filtered on, and how many games in the tab you're looking at
 * would survive it.
 *
 * The count is what makes the filter row honest. Chips used to be built only from the games in
 * the *current* tab, so the set changed under your thumb every time you switched tab, and a
 * filter you'd left on could silently empty a tab that never offered that chip in the first
 * place — a closed tester's *"filters not consistent across categories"*. The chip set is now
 * built from the whole pile and is therefore stable, and a chip that would match nothing here
 * says so instead of quietly disappearing.
 */
data class FilterOption<T>(val value: T, val label: String, val countInTab: Int) {
    val isEmptyHere: Boolean get() = countInTab == 0
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
    /** Multi-select, OR within the group — "show me horror *or* co-op". */
    val facetFilters: Set<GameFacet> = emptySet(),
    val platformFilters: Set<String> = emptySet(),
    val lengthBucketFilter: LengthBucket? = null,
    val availableFacets: List<FilterOption<GameFacet>> = emptyList(),
    val availablePlatforms: List<FilterOption<String>> = emptyList(),
    val availableLengths: List<FilterOption<LengthBucket>> = emptyList(),
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
        get() = facetFilters.size + platformFilters.size + (if (lengthBucketFilter != null) 1 else 0)

    /** "412 HRS · 87 GAMES · FINISHED BY 2029" — docs/02-PRODUCT-SPEC.md §1. */
    val timeBudgetFinishCopy: String
        get() = finishByCopy(totalHours, hoursPerWeek)
}

/**
 * "FINISHED BY 2029" from a pile size and a weekly playing habit.
 *
 * A top-level function rather than only a property on the state so the hours-per-week dialog can
 * show the answer changing **as the slider moves**, without inventing a second copy of the
 * arithmetic that would drift from the bar it's meant to be previewing. Also makes the one piece
 * of real maths on PILE unit-testable.
 *
 * Months are converted with 4.345 weeks/month (365.25 / 7 / 12) rather than 4, because at a
 * decade's distance the difference is over a year.
 */
fun finishByCopy(
    totalHours: Int,
    hoursPerWeek: Float,
    now: java.util.Calendar = java.util.Calendar.getInstance(),
): String {
    if (totalHours <= 0 || hoursPerWeek <= 0f) return "FINISHED BY NOW"
    val weeksNeeded = totalHours / hoursPerWeek
    val monthsNeeded = weeksNeeded / 4.345f

    // Guard before the Calendar call: a huge pile at one hour a week overflows `Int` months and
    // `Calendar.add` wraps it into the past, which would print a finish date that has already
    // happened for the most hopeless pile in the app.
    if (monthsNeeded > MAX_PROJECTED_MONTHS) return "FINISHED BY NEVER. CONSIDER RETIRING SOME."

    val target = (now.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.MONTH, monthsNeeded.toInt())
    }
    return when {
        monthsNeeded < 6 -> {
            val monthName = target.getDisplayName(
                java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault()
            )
            "FINISHED BY $monthName"
        }
        monthsNeeded > 120 -> "FINISHED BY ${target.get(java.util.Calendar.YEAR)}. CONSIDER RETIRING SOME."
        else -> "FINISHED BY ${target.get(java.util.Calendar.YEAR)}"
    }
}

/** ~83 years out. Past this the projection isn't wrong so much as meaningless. */
private const val MAX_PROJECTED_MONTHS = 1000f
