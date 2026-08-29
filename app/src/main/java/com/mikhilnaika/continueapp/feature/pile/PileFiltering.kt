package com.mikhilnaika.continueapp.feature.pile

import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.util.GameFacet
import com.mikhilnaika.continueapp.core.util.estimatedHours
import com.mikhilnaika.continueapp.core.util.facets
import com.mikhilnaika.continueapp.core.util.platforms

/** Everything the user has narrowed the pile by. One object so it can be one `StateFlow`. */
data class PileFilters(
    val sort: PileSort = PileSort.DATE_ADDED,
    val facets: Set<GameFacet> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val length: LengthBucket? = null,
) {
    val isEmpty: Boolean get() = facets.isEmpty() && platforms.isEmpty() && length == null
}

/**
 * Pure filtering and sorting for PILE — no Android types, no I/O, so every rule below is
 * directly testable (`PileFilteringTest`).
 *
 * **OR inside a group, AND between groups.** Picking HORROR and CO-OP means "either", picking
 * HORROR and PLAYSTATION 5 means "both". That's the convention every store filter uses, and it
 * is the only one where adding a second chip of the same kind can't make the list shrink to
 * nothing — which is what single-select filters did every time a tester tried to widen a search.
 */
object PileFiltering {

    fun matches(entry: PileEntryWithGame, filters: PileFilters): Boolean {
        if (filters.facets.isNotEmpty() && entry.facets.none { it in filters.facets }) return false
        if (filters.platforms.isNotEmpty() && entry.platforms.none { it in filters.platforms }) return false
        val length = filters.length
        if (length != null && !fitsLength(entry, length)) return false
        return true
    }

    /**
     * A game with no believable playtime is **excluded** by a length filter rather than let
     * through. "Under 5 hours" is a promise about the list; an unknown length can't keep it.
     * See [com.mikhilnaika.continueapp.core.util.Playtime] for why a length can be unknown even
     * when IGDB returned three numbers.
     */
    fun fitsLength(entry: PileEntryWithGame, bucket: LengthBucket): Boolean {
        val hours = entry.estimatedHours ?: return false
        val fitsMax = bucket.maxHours == null || hours <= bucket.maxHours
        return hours >= bucket.minHours && fitsMax
    }

    fun apply(entries: List<PileEntryWithGame>, filters: PileFilters): List<PileEntryWithGame> =
        sort(entries.filter { matches(it, filters) }, filters.sort)

    fun sort(entries: List<PileEntryWithGame>, sort: PileSort): List<PileEntryWithGame> = when (sort) {
        PileSort.DATE_ADDED -> entries.sortedByDescending { it.addedAt }
        PileSort.TITLE -> entries.sortedBy { it.name.lowercase() }
        PileSort.LENGTH_SHORT_FIRST -> entries.sortedBy { it.estimatedHours ?: Int.MAX_VALUE }
        // Unrated games sort last rather than first — a missing rating is not a zero score.
        PileSort.RATING -> entries.sortedByDescending { it.rating ?: -1f }
        // ISO dates sort correctly as strings; an unknown date goes to the bottom.
        PileSort.RELEASE_DATE -> entries.sortedByDescending { it.released ?: "" }
        PileSort.PLATFORM -> entries.sortedBy { it.ownedPlatform ?: it.platforms.firstOrNull() ?: "￿" }
    }

    /**
     * The chips to offer, built from **[all]** (every game in the pile, whatever tab it's in)
     * and counted against **[inTab]** (the tab on screen).
     *
     * Splitting the two arguments is the entire fix for "filters not consistent across
     * categories": the *set* of chips comes from the whole pile so it never changes when you
     * switch tab, and the *number* on each chip comes from the tab you're looking at so it
     * still tells you what's actually there.
     *
     * A selected chip is always offered even if nothing in the pile matches it any more —
     * otherwise the only control that could turn the filter off would vanish along with the
     * games it was hiding.
     */
    fun facetOptions(
        all: List<PileEntryWithGame>,
        inTab: List<PileEntryWithGame>,
        selected: Set<GameFacet>,
    ): List<FilterOption<GameFacet>> {
        val present = all.flatMapTo(mutableSetOf()) { it.facets } + selected
        val counts = inTab.flatMap { it.facets }.groupingBy { it }.eachCount()
        return GameFacet.entries
            .filter { it in present }
            .map { FilterOption(it, it.label, counts[it] ?: 0) }
            .sortedWith(compareByDescending<FilterOption<GameFacet>> { it.countInTab }.thenBy { it.label })
    }

    fun platformOptions(
        all: List<PileEntryWithGame>,
        inTab: List<PileEntryWithGame>,
        selected: Set<String>,
    ): List<FilterOption<String>> {
        val present = all.flatMapTo(mutableSetOf()) { it.platforms } + selected
        val counts = inTab.flatMap { it.platforms }.groupingBy { it }.eachCount()
        return present.sorted().map { FilterOption(it, it.uppercase(), counts[it] ?: 0) }
    }

    /** Length buckets are a fixed set, so only the counts vary — but they vary a lot. */
    fun lengthOptions(inTab: List<PileEntryWithGame>): List<FilterOption<LengthBucket>> =
        LengthBucket.entries.map { bucket ->
            FilterOption(bucket, bucket.label, inTab.count { fitsLength(it, bucket) })
        }
}
