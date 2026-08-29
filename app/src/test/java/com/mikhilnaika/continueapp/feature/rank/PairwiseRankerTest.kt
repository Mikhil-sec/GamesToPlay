package com.mikhilnaika.continueapp.feature.rank

import com.mikhilnaika.continueapp.core.data.RankBucket
import com.mikhilnaika.continueapp.core.data.entity.RankingEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/** docs/02-PRODUCT-SPEC.md §5 "Pairwise placement". */
class PairwiseRankerTest {

    private fun ranking(gameId: Long, bucket: RankBucket, position: Int) =
        RankingEntity(gameId = gameId, bucket = bucket, position = position, rankedAt = 0L)

    @Test
    fun `empty list places the first ranked game at position 1`() {
        val position = PairwiseRanker.globalPosition(emptyList(), RankBucket.LOVED, indexInBucket = 0)
        assertEquals(1, position)
    }

    @Test
    fun `empty bucket inserts before the next worse bucket`() {
        val all = listOf(ranking(1, RankBucket.LIKED, 1), ranking(2, RankBucket.FINE, 2))
        val position = PairwiseRanker.globalPosition(all, RankBucket.LOVED, indexInBucket = 0)
        assertEquals(1, position) // LOVED outranks everything present
    }

    @Test
    fun `empty bucket with nothing worse appends to the end`() {
        val all = listOf(ranking(1, RankBucket.LOVED, 1), ranking(2, RankBucket.LIKED, 2))
        val position = PairwiseRanker.globalPosition(all, RankBucket.NAH, indexInBucket = 0)
        assertEquals(3, position)
    }

    @Test
    fun `inserting within a populated bucket lands before the member at that index`() {
        val all = listOf(ranking(1, RankBucket.LOVED, 1), ranking(2, RankBucket.LOVED, 2), ranking(3, RankBucket.LOVED, 3))
        val position = PairwiseRanker.globalPosition(all, RankBucket.LOVED, indexInBucket = 1)
        assertEquals(2, position)
    }

    @Test
    fun `inserting past the last bucket member lands right after it`() {
        val all = listOf(ranking(1, RankBucket.LOVED, 1), ranking(2, RankBucket.LOVED, 2))
        val position = PairwiseRanker.globalPosition(all, RankBucket.LOVED, indexInBucket = 2)
        assertEquals(3, position)
    }

    @Test
    fun `binary search midpoint narrows correctly`() {
        assertEquals(2, PairwiseRanker.nextComparisonIndex(0, 5))
        assertEquals(0, PairwiseRanker.nextComparisonIndex(0, 0))
    }

    // ---------------------------------------------------------------------------------------
    // Manual reordering — the override RANK never had. Five comparisons place a game
    // approximately, and until now a mis-tap during them was permanent.
    // ---------------------------------------------------------------------------------------

    private val loved = listOf(
        ranking(10, RankBucket.LOVED, 0),
        ranking(11, RankBucket.LOVED, 1),
        ranking(12, RankBucket.LIKED, 2),
        ranking(13, RankBucket.NAH, 3),
    )

    @Test
    fun `moving up swaps with the entry above and renumbers densely`() {
        val moved = PairwiseRanker.reorderedPlacements(loved, fromIndex = 1, delta = -1)
        assertEquals(listOf(11L, 10L, 12L, 13L), moved.map { it.gameId })
    }

    @Test
    fun `moving down swaps with the entry below`() {
        val moved = PairwiseRanker.reorderedPlacements(loved, fromIndex = 0, delta = 1)
        assertEquals(listOf(11L, 10L, 12L, 13L), moved.map { it.gameId })
    }

    @Test
    fun `a game moved past a bucket boundary adopts the bucket it lands in`() {
        // Without this the buckets interleave, and every later automatic placement is wrong:
        // globalPosition and bucketMembers both assume a bucket is one contiguous run.
        val moved = PairwiseRanker.reorderedPlacements(loved, fromIndex = 1, delta = 1)
        assertEquals(listOf(10L, 12L, 11L, 13L), moved.map { it.gameId })
        assertEquals(RankBucket.LIKED, moved.first { it.gameId == 11L }.bucket)
    }

    @Test
    fun `a game dragged to the very top takes the bucket of the entry now below it`() {
        val moved = PairwiseRanker.reorderedPlacements(loved, fromIndex = 3, delta = -3)
        assertEquals(listOf(13L, 10L, 11L, 12L), moved.map { it.gameId })
        assertEquals(RankBucket.LOVED, moved.first { it.gameId == 13L }.bucket)
    }

    @Test
    fun `buckets stay contiguous after a boundary-crossing move`() {
        val moved = PairwiseRanker.reorderedPlacements(loved, fromIndex = 3, delta = -3)
        val order = moved.map { it.bucket }
        // Each bucket appears as one unbroken run.
        assertEquals(order.distinct().size, order.zipWithNext().count { (a, b) -> a != b } + 1)
    }

    @Test
    fun `a move that would run off either end clamps instead of throwing`() {
        assertEquals(
            listOf(10L, 11L, 12L, 13L),
            PairwiseRanker.reorderedPlacements(loved, fromIndex = 0, delta = -5).map { it.gameId },
        )
        assertEquals(
            listOf(10L, 11L, 12L, 13L),
            PairwiseRanker.reorderedPlacements(loved, fromIndex = 3, delta = 9).map { it.gameId },
        )
    }

    @Test
    fun `an index that is not in the list leaves the order untouched`() {
        // The buttons that call this sit next to a list that can change underneath them.
        assertEquals(
            listOf(10L, 11L, 12L, 13L),
            PairwiseRanker.reorderedPlacements(loved, fromIndex = 99, delta = -1).map { it.gameId },
        )
    }

    @Test
    fun `dropping a game leaves the rest in order with their buckets intact`() {
        val remaining = PairwiseRanker.withoutGame(loved, gameId = 11)
        assertEquals(listOf(10L, 12L, 13L), remaining.map { it.gameId })
        assertEquals(RankBucket.LIKED, remaining.first { it.gameId == 12L }.bucket)
    }

    @Test
    fun `reordering works on a list whose positions have gaps`() {
        // Positions genuinely arrive with gaps — deleteByGameId leaves one behind and shiftDown
        // only ever pushes downward — so "swap the two numbers" would be right only by luck.
        val gappy = listOf(
            ranking(1, RankBucket.LOVED, 0),
            ranking(2, RankBucket.LOVED, 7),
            ranking(3, RankBucket.LOVED, 40),
        )
        val moved = PairwiseRanker.reorderedPlacements(gappy, fromIndex = 2, delta = -2)
        assertEquals(listOf(3L, 1L, 2L), moved.map { it.gameId })
    }
}
