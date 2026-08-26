package com.mikhilnaika.continueapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every figure below is a **real IGDB `game_time_to_beats` row**, read off the production
 * Worker on 2026-08-23 (`/games/search?q=…`). Keep them verbatim: the point of this test is
 * that the plausibility ceiling separates the genuinely-long games from the polluted ones on
 * the actual data, not on invented numbers.
 */
class PlaytimeTest {

    @Test
    fun `minecraft falls back off its polluted normally figure`() {
        // The closed-test report: shown as 956 HRS, which no source outside IGDB agrees with.
        assertEquals(98, Playtime.estimateHours(hastily = 98, normally = 956, completely = 20417))
        assertEquals("98 HRS", Playtime.label(hastily = 98, normally = 956, completely = 20417))
    }

    @Test
    fun `ordinary games are untouched`() {
        assertEquals(9, Playtime.estimateHours(4, 9, 28)) // Portal 2
        assertEquals(90, Playtime.estimateHours(50, 90, 208)) // Stardew Valley
        assertEquals(109, Playtime.estimateHours(25, 109, 201)) // Skyrim
        assertEquals(70, Playtime.estimateHours(39, 70, 160)) // Hades
    }

    @Test
    fun `a polluted completionist figure does not discredit a sound normally figure`() {
        // Baldur's Gate III: `completely` is nonsense at 5650h, `normally` at 132h is right.
        assertEquals(132, Playtime.estimateHours(128, 132, 5650))
        // Counter-Strike, same shape.
        assertEquals(15, Playtime.estimateHours(9, 15, 761))
    }

    @Test
    fun `a game with nothing believable reads as endless rather than as a number`() {
        assertNull(Playtime.estimateHours(hastily = 4000, normally = 9000, completely = 40000))
        assertEquals("ENDLESS", Playtime.label(hastily = 4000, normally = 9000, completely = 40000))
    }

    @Test
    fun `missing data reads as endless, never as zero hours`() {
        // A brand-new IGDB entry has no time-to-beat rows at all. "0 HRS" would be a lie that
        // also drags the pile's FINISHED BY estimate down.
        assertNull(Playtime.estimateHours(null, null, null))
        assertEquals("ENDLESS", Playtime.label(null, null, null))
        assertNull(Playtime.estimateHours(0, 0, 0))
    }

    @Test
    fun `partial data still yields the best believable figure`() {
        assertEquals(63, Playtime.estimateHours(hastily = 28, normally = 63, completely = null))
        assertEquals(28, Playtime.estimateHours(hastily = 28, normally = null, completely = null))
        assertEquals(45, Playtime.estimateHours(hastily = null, normally = null, completely = 45))
    }

    @Test
    fun `the ceiling sits clear of the longest real campaign in the sample`() {
        assertTrue(Playtime.MAX_PLAUSIBLE_HOURS > 132) // Baldur's Gate III, believable
        assertTrue(Playtime.MAX_PLAUSIBLE_HOURS < 956) // Minecraft, not
    }
}
