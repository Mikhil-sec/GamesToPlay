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
}
