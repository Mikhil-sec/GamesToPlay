package com.mikhilnaika.continueapp.feature.rank

import com.mikhilnaika.continueapp.core.data.RankBucket
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
}
