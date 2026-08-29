package com.mikhilnaika.continueapp.feature.clipboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gate that decides how often the app interrupts you unprompted.
 *
 * docs/02-PRODUCT-SPEC.md §2d: *"a delight when it's right and an irritation when it's wrong, so
 * bias toward silence."* Every rejection below is a real category of thing people have on their
 * clipboard, and each one that slipped through would be an interruption offering to add a
 * nonsense game.
 */
class ClipboardNudgeTest {

    private fun plausible(text: String) = ClipboardNudgeViewModel.isPlausibleGameName(text)

    @Test
    fun `a bare game name is worth a lookup`() {
        assertTrue(plausible("Hollow Knight Silksong"))
        assertTrue(plausible("Elden Ring"))
        assertTrue(plausible("Ico"))
        assertTrue(plausible("Sid Meier's Civilization VI"))
    }

    @Test
    fun `links belong to the share target, which resolves them properly`() {
        assertFalse(plausible("https://store.steampowered.com/app/1030300"))
        assertFalse(plausible("Check this out http://youtu.be/abc"))
        assertFalse(plausible("www.gog.com/game/hades"))
    }

    @Test
    fun `an email or handle is not a game`() {
        assertFalse(plausible("naikamikhil@gmail.com"))
        assertFalse(plausible("@fromsoftware"))
    }

    @Test
    fun `codes and numbers are not games`() {
        // One-time passcodes and order references are the single most-copied thing on a phone.
        assertFalse(plausible("492013"))
        assertFalse(plausible("+44 7700 900123"))
    }

    @Test
    fun `prose is not a game name`() {
        assertFalse(
            plausible("I was thinking we could get dinner around eight if that still works for you")
        )
        assertFalse(plausible("one two three four five six seven eight nine"))
    }

    @Test
    fun `multi-line text is a snippet, not a title`() {
        assertFalse(plausible("Elden Ring\nShadow of the Erdtree"))
    }

    @Test
    fun `nothing and almost-nothing stay silent`() {
        assertFalse(plausible(""))
        assertFalse(plausible("  "))
        assertFalse(plausible("hi"))
    }

    @Test
    fun `a very long string is rejected before any lookup happens`() {
        assertFalse(plausible("A".repeat(500)))
    }
}
