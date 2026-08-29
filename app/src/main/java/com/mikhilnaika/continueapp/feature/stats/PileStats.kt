package com.mikhilnaika.continueapp.feature.stats

import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.util.estimatedHours
import com.mikhilnaika.continueapp.core.util.facets
import com.mikhilnaika.continueapp.core.util.platforms
import com.mikhilnaika.continueapp.core.util.releaseYear
import com.mikhilnaika.continueapp.feature.pile.LengthBucket
import com.mikhilnaika.continueapp.feature.pile.PileFiltering
import kotlin.math.ln
import kotlin.math.roundToInt

/** One bar. [share] is 0..1 of the largest bar in its own group, which is what gets drawn. */
data class StatBar(val label: String, val count: Int, val share: Float)

/** One slice of the state bar at the top of the screen. */
data class StateSlice(val state: PileState, val label: String, val count: Int)

data class StatsSnapshot(
    val totalGames: Int = 0,
    val totalHours: Int = 0,
    val stateSlices: List<StateSlice> = emptyList(),
    val facets: List<StatBar> = emptyList(),
    val platforms: List<StatBar> = emptyList(),
    val lengths: List<StatBar> = emptyList(),
    val decades: List<StatBar> = emptyList(),
    /** 0-100. See [PileStats.diversityPercent]. */
    val diversity: Int = 0,
    val headlineFacet: String? = null,
    val oldestYear: Int? = null,
    val newestYear: Int? = null,
) {
    val isEmpty: Boolean get() = totalGames == 0
}

/**
 * The arithmetic behind STATS — a closed-test request: *"based on the filters, there could be a
 * cool visual stats screen showing the diversity of games sitting in THE PILE and CLEARED"*.
 *
 * It reuses [com.mikhilnaika.continueapp.core.util.GameTaxonomy] rather than inventing a third
 * classification, which is the point: the bars on this screen are the same categories as PILE's
 * filter chips and DRAW's GENRE dial, so a number here can always be reached by tapping.
 *
 * Pure, so `PileStatsTest` can pin the diversity maths without a database.
 */
object PileStats {

    /** Bars past this are a long tail nobody reads; the totals above them stay complete. */
    private const val MAX_BARS = 8

    fun compute(scoped: List<PileEntryWithGame>, allStates: List<PileEntryWithGame>): StatsSnapshot {
        if (scoped.isEmpty()) {
            return StatsSnapshot(stateSlices = stateSlices(allStates))
        }
        val facetCounts = scoped.flatMap { it.facets }.groupingBy { it }.eachCount()
        val years = scoped.mapNotNull { it.releaseYear }

        return StatsSnapshot(
            totalGames = scoped.size,
            totalHours = scoped.sumOf { it.estimatedHours ?: 0 },
            stateSlices = stateSlices(allStates),
            facets = bars(facetCounts.mapKeys { (facet, _) -> facet.label }),
            platforms = bars(scoped.flatMap { it.platforms }.groupingBy { it.uppercase() }.eachCount()),
            lengths = LengthBucket.entries
                .map { bucket -> bucket.label to scoped.count { PileFiltering.fitsLength(it, bucket) } }
                .let { pairs -> barsInOrder(pairs) },
            decades = decadeBars(years),
            diversity = diversityPercent(facetCounts.values.toList()),
            headlineFacet = facetCounts.maxByOrNull { it.value }?.key?.label,
            oldestYear = years.minOrNull(),
            newestYear = years.maxOrNull(),
        )
    }

    private fun stateSlices(all: List<PileEntryWithGame>): List<StateSlice> =
        STATE_LABELS.map { (state, label) -> StateSlice(state, label, all.count { it.state == state }) }

    /** Sorted by count, capped, scaled against the biggest bar so the chart always fills. */
    private fun bars(counts: Map<String, Int>): List<StatBar> {
        val top = counts.entries.sortedByDescending { it.value }.take(MAX_BARS)
        val max = top.firstOrNull()?.value ?: return emptyList()
        return top.map { StatBar(it.key, it.value, it.value.toFloat() / max) }
    }

    /** Same scaling, but keeps the caller's order — for buckets that mean something in sequence. */
    private fun barsInOrder(pairs: List<Pair<String, Int>>): List<StatBar> {
        val max = pairs.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: return emptyList()
        return pairs.map { (label, count) -> StatBar(label, count, count.toFloat() / max) }
    }

    private fun decadeBars(years: List<Int>): List<StatBar> {
        if (years.isEmpty()) return emptyList()
        val counts = years.groupingBy { (it / 10) * 10 }.eachCount()
        val ordered = counts.entries.sortedBy { it.key }.map { "${it.key}s" to it.value }
        return barsInOrder(ordered)
    }

    /**
     * How varied a set of games is, 0-100.
     *
     * Normalised Shannon entropy over the facet counts: 100 means every category present is
     * equally represented, 0 means everything is the same one thing. Entropy rather than "how
     * many categories" because a pile of 19 shooters and one puzzle game is not diverse, and a
     * plain count says it is.
     *
     * A single category is 0 by definition (`ln(1)` is 0, and there is genuinely no variety),
     * which is also what keeps the maths from dividing by zero.
     */
    fun diversityPercent(counts: List<Int>): Int {
        val positive = counts.filter { it > 0 }
        if (positive.size <= 1) return 0
        val total = positive.sum().toDouble()
        val entropy = positive.sumOf { count ->
            val p = count / total
            -p * ln(p)
        }
        return ((entropy / ln(positive.size.toDouble())) * 100).roundToInt().coerceIn(0, 100)
    }

    /** The five states in the order PILE's own tabs use, so the two screens read the same. */
    val STATE_LABELS: List<Pair<PileState, String>> = listOf(
        PileState.BACKLOG to "THE PILE",
        PileState.PLAYING to "PLAYING",
        PileState.COMPLETED to "CLEARED",
        PileState.DROPPED to "RETIRED",
        PileState.WISHLIST to "WANTED",
    )
}

/** What STATS is looking at: one pile state, or everything at once. */
data class StatsScope(val state: PileState?) {
    val label: String
        get() = state?.let { s -> PileStats.STATE_LABELS.first { it.first == s }.second } ?: "EVERYTHING"

    companion object {
        val ALL = StatsScope(null)
        val options: List<StatsScope> = listOf(ALL) + PileStats.STATE_LABELS.map { StatsScope(it.first) }
    }
}
