package com.mikhilnaika.continueapp.feature.pile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * "FINISHED BY 2029" — the headline of PILE's time budget, and now the thing the hours-per-week
 * slider previews live.
 *
 * Pinned to a fixed `now` so these don't drift with the wall clock.
 */
class FinishByCopyTest {

    /** 1 Jan 2026, so a projection of N months lands somewhere predictable. */
    private fun fixedNow(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2026, Calendar.JANUARY, 1)
    }

    @Test
    fun `an empty pile is already finished`() {
        assertEquals("FINISHED BY NOW", finishByCopy(0, 6f, fixedNow()))
    }

    @Test
    fun `a short pile lands on a month name rather than a year`() {
        // 20h at 6h/week is under 4 weeks — a year would read as absurdly far off.
        val copy = finishByCopy(20, 6f, fixedNow())
        assertTrue(copy, copy.startsWith("FINISHED BY ") && !copy.contains("202"))
    }

    @Test
    fun `a realistic pile lands on a year`() {
        // 400h at 6h/week is about 15 months.
        assertEquals("FINISHED BY 2027", finishByCopy(400, 6f, fixedNow()))
    }

    @Test
    fun `playing more finishes sooner — the whole point of the slider`() {
        val slow = finishByCopy(400, 2f, fixedNow())
        val fast = finishByCopy(400, 20f, fixedNow())
        assertTrue(slow, slow.contains("2029"))
        assertTrue(fast, !fast.contains("202") || fast.contains("2026"))
    }

    @Test
    fun `a hopeless pile is told to retire some`() {
        val copy = finishByCopy(3000, 2f, fixedNow())
        assertTrue(copy, copy.contains("CONSIDER RETIRING SOME"))
    }

    @Test
    fun `an absurd pile never projects a date in the past`() {
        // Regression guard: months are passed to Calendar.add as an Int, and a big enough pile at
        // one hour a week overflows it — which wraps the date *backwards* and would have printed
        // a finish year that has already happened for the most hopeless pile in the app.
        val copy = finishByCopy(Int.MAX_VALUE, 1f, fixedNow())
        assertEquals("FINISHED BY NEVER. CONSIDER RETIRING SOME.", copy)
    }

    @Test
    fun `zero hours a week never divides by zero`() {
        assertEquals("FINISHED BY NOW", finishByCopy(400, 0f, fixedNow()))
    }
}
