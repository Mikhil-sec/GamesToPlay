package com.mikhilnaika.continueapp.feature.rank

import com.mikhilnaika.continueapp.core.data.RankBucket
import com.mikhilnaika.continueapp.core.data.dao.RankPlacement
import com.mikhilnaika.continueapp.core.data.entity.RankingEntity

/** docs/02-PRODUCT-SPEC.md §5 — coarser buckets always outrank finer ones globally. */
val BUCKET_ORDER = listOf(RankBucket.LOVED, RankBucket.LIKED, RankBucket.FINE, RankBucket.NAH)

/** Cap 5 comparisons — binary search places a game precisely within a 32-game bucket. */
const val MAX_COMPARISONS = 5

/**
 * Pure math behind pairwise ranking — docs/02-PRODUCT-SPEC.md §5. The interactive binary
 * search itself lives in [RankViewModel] (it needs to await a tap between comparisons); this
 * object holds the parts that are cheap to unit test in isolation.
 */
object PairwiseRanker {

    /** Games already ranked in [bucket], ordered by their existing global position. */
    fun bucketMembers(all: List<RankingEntity>, bucket: RankBucket): List<RankingEntity> =
        all.filter { it.bucket == bucket }.sortedBy { it.position }

    /**
     * Global insertion position for a new game landing at [indexInBucket] within [bucket]'s
     * existing members. When the bucket is empty, falls back to the boundary before the next
     * (worse) bucket that already has entries, or the end of the list.
     */
    fun globalPosition(all: List<RankingEntity>, bucket: RankBucket, indexInBucket: Int): Int {
        val members = bucketMembers(all, bucket)
        if (members.isNotEmpty()) {
            return if (indexInBucket < members.size) members[indexInBucket].position else members.last().position + 1
        }
        val bucketRank = BUCKET_ORDER.indexOf(bucket)
        val nextWorseEntry = all
            .filter { BUCKET_ORDER.indexOf(it.bucket) > bucketRank }
            .minByOrNull { it.position }
        return nextWorseEntry?.position ?: (all.size + 1)
    }

    /** Binary-search midpoint for the next comparison within an [lo, hi) insertion range. */
    fun nextComparisonIndex(lo: Int, hi: Int): Int = (lo + hi) / 2

    /**
     * The leaderboard after dragging one entry [delta] slots — the manual override for RANK's
     * automatic placement.
     *
     * Pairwise comparison is a good way to *enter* a ranking and a bad way to fix one: five
     * questions place a game approximately, and there was no way at all to say "no, that one's
     * higher". This is that way, and it's the only place in the app that writes a position a
     * comparison didn't decide.
     *
     * **The moved game adopts its new neighbourhood's bucket**, taking it from the entry now
     * directly above it (or below, at the very top). Without that a game dragged past a bucket
     * boundary would leave the buckets interleaved, and every *later* automatic placement would
     * be wrong: [globalPosition] and [bucketMembers] both assume a bucket is one contiguous run.
     * The visible consequence is small and correct — dragging a game above your LOVED games
     * makes it loved.
     *
     * Returns the complete new order, densely numbered by the caller ([RankingDao.reorder]);
     * out-of-range moves clamp rather than throw, because the buttons that call this sit next
     * to a list that can change underneath them.
     */
    fun reorderedPlacements(
        current: List<RankingEntity>,
        fromIndex: Int,
        delta: Int,
    ): List<RankPlacement> {
        val ordered = current.sortedBy { it.position }
        if (fromIndex !in ordered.indices) return ordered.map { RankPlacement(it.gameId, it.bucket) }
        val toIndex = (fromIndex + delta).coerceIn(0, ordered.lastIndex)
        if (toIndex == fromIndex) return ordered.map { RankPlacement(it.gameId, it.bucket) }

        val rearranged = ordered.toMutableList()
        val moved = rearranged.removeAt(fromIndex)
        rearranged.add(toIndex, moved)

        val neighbourBucket = rearranged.getOrNull(toIndex - 1)?.bucket
            ?: rearranged.getOrNull(toIndex + 1)?.bucket
            ?: moved.bucket
        return rearranged.mapIndexed { index, entry ->
            RankPlacement(entry.gameId, if (index == toIndex) neighbourBucket else entry.bucket)
        }
    }

    /** The order with one game taken out, ready to be renumbered densely. */
    fun withoutGame(current: List<RankingEntity>, gameId: Long): List<RankPlacement> =
        current.sortedBy { it.position }
            .filter { it.gameId != gameId }
            .map { RankPlacement(it.gameId, it.bucket) }
}
