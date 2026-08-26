package com.mikhilnaika.continueapp.feature.pile

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression cover for the STACK crash (Play Console: `IndexOutOfBoundsException: Index: 7,
 * Size: 1` and `ArrayIndexOutOfBoundsException: length=2; index=2`, both at the caption's
 * `entries[anchor]`).
 *
 * The scroll position is an `Animatable` that survives the list being swapped underneath it —
 * switching PILE tabs, a filter shrinking the results, a game moving out of the current state.
 * The only thing standing between that and a crash is clamping the position against the list
 * being indexed *right now*, which is exactly what [stackAnchor] is for. The cases below are
 * the two crash reports, restated as arithmetic.
 */
class StackAnchorTest {

    @Test
    fun `position past the end of a shrunken list clamps to the last card`() {
        // Scrolled to card 8 of 8 in THE PILE, then switched to a tab holding one game.
        assertEquals(0, stackAnchor(position = 7f, lastIndex = 0))
        // Three-card stack, scrolled to the end, list drops to two.
        assertEquals(1, stackAnchor(position = 2f, lastIndex = 1))
    }

    @Test
    fun `a single-card stack always answers zero`() {
        assertEquals(0, stackAnchor(position = 0f, lastIndex = 0))
        assertEquals(0, stackAnchor(position = 99f, lastIndex = 0))
        assertEquals(0, stackAnchor(position = -4f, lastIndex = 0))
    }

    @Test
    fun `a position before the start clamps to the first card`() {
        // Over-drag downward at the top of the stack.
        assertEquals(0, stackAnchor(position = -1.4f, lastIndex = 5))
    }

    @Test
    fun `mid-scroll positions round to the nearest card`() {
        assertEquals(2, stackAnchor(position = 2.4f, lastIndex = 5))
        assertEquals(3, stackAnchor(position = 2.5f, lastIndex = 5))
        assertEquals(3, stackAnchor(position = 3.49f, lastIndex = 5))
    }

    @Test
    fun `a list that grew is not capped at the old ceiling`() {
        // The other half of the bug: the clamp used to be pinned to the length at first
        // composition, so a pile that filled in after the first frame froze the cards at the
        // stale ceiling while the position — and its per-card haptic — kept running.
        assertEquals(7, stackAnchor(position = 7f, lastIndex = 11))
    }
}
