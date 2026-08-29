package com.mikhilnaika.continueapp.feature.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The diversity meter is the one number on STATS that isn't a count, so it's the one that can
 * be quietly wrong. These pin the properties the label on screen actually claims.
 */
class PileStatsTest {

    @Test
    fun `one category only is zero percent varied`() {
        assertEquals(0, PileStats.diversityPercent(listOf(12)))
        assertEquals(0, PileStats.diversityPercent(emptyList()))
    }

    @Test
    fun `an even split across categories is fully varied`() {
        assertEquals(100, PileStats.diversityPercent(listOf(5, 5, 5, 5)))
        assertEquals(100, PileStats.diversityPercent(listOf(1, 1)))
    }

    @Test
    fun `nineteen shooters and one puzzle game is not diverse`() {
        // The whole reason this is entropy and not "how many categories are present" — a plain
        // count would call this a two-category pile and score it 100.
        val lopsided = PileStats.diversityPercent(listOf(19, 1))
        assertTrue("Expected a low score, got $lopsided", lopsided < 35)
    }

    @Test
    fun `evening out a lopsided mix always raises the score`() {
        assertTrue(
            PileStats.diversityPercent(listOf(10, 10)) > PileStats.diversityPercent(listOf(18, 2))
        )
        assertTrue(
            PileStats.diversityPercent(listOf(8, 6, 6)) > PileStats.diversityPercent(listOf(18, 1, 1))
        )
    }

    @Test
    fun `zero counts are ignored rather than dragging the score down`() {
        assertEquals(
            PileStats.diversityPercent(listOf(5, 5)),
            PileStats.diversityPercent(listOf(5, 5, 0, 0)),
        )
    }

    @Test
    fun `the score is always a percentage`() {
        val samples = listOf(
            listOf(1), listOf(1, 1), listOf(100, 1), listOf(3, 3, 3, 3, 3, 3, 3, 3),
            listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        )
        samples.forEach { counts ->
            val score = PileStats.diversityPercent(counts)
            assertTrue("$counts -> $score", score in 0..100)
        }
    }

    @Test
    fun `the scope list offers everything plus each pile state`() {
        assertEquals("EVERYTHING", StatsScope.ALL.label)
        assertEquals(1 + PileStats.STATE_LABELS.size, StatsScope.options.size)
        assertTrue(StatsScope.options.any { it.label == "CLEARED" })
    }
}
