package com.mikhilnaika.continueapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kotlin mirror of `worker/test/candidates.test.ts` — same fixture strings, same assertions.
 * The two implementations have to agree, or the offline (`OfflineGameIndex`) and online
 * (`worker/src/resolve/resolveGame.ts`) matching paths would silently rank/score the same
 * caption differently. `PALWORLD`/`LEAGUE` are real captions captured from Mikhil's device
 * 2026-08-12 — kept verbatim, see `docs/10-BUILD-STATUS.md`.
 */
class GameNameCandidatesTest {

    private val palworld = "Each Pal has their own method of transporting items📦 Pocketpair Palworld"
    private val league = "a tale of two bush ganks 🤔 League of Legends"

    private fun rankOf(text: String, wanted: String): Int =
        GameNameCandidates.rankedCandidates(text).indexOfFirst { it.equals(wanted, ignoreCase = true) }

    @Test
    fun `isolates the game name from a caption plus channel name`() {
        val rank = rankOf(palworld, "Palworld")
        assertTrue("Palworld should be a candidate", rank >= 0)
        assertTrue("Palworld ranked $rank, outside the 6-search budget", rank < 6)
    }

    @Test
    fun `keeps a multi-word title together through its lowercase connector`() {
        val rank = rankOf(league, "League of Legends")
        assertTrue("League of Legends should be a candidate", rank >= 0)
        assertTrue("League of Legends ranked $rank, outside the 6-search budget", rank < 6)
    }

    @Test
    fun `prefers the longer proper-noun run over its fragments`() {
        val candidates = GameNameCandidates.rankedCandidates(league)
        assertTrue(candidates.indexOf("League of Legends") < candidates.indexOf("League"))
    }

    @Test
    fun `hashtags outrank anything inferred from prose`() {
        val candidates = GameNameCandidates.rankedCandidates("insane clutch #EldenRing gameplay")
        assertEquals("elden ring", candidates.first())
    }

    @Test
    fun `does not emit bare stopwords as searchable candidates`() {
        for (candidate in GameNameCandidates.rankedCandidates(palworld)) {
            assertTrue(!candidate.equals("each", ignoreCase = true))
            assertTrue(!candidate.equals("their", ignoreCase = true))
        }
    }

    @Test
    fun `verification accepts a name present in the caption`() {
        assertTrue(GameNameCandidates.verifyAgainstText(palworld, "Palworld") > 0.8f)
        assertTrue(GameNameCandidates.verifyAgainstText(league, "League of Legends") > 0.8f)
    }

    @Test
    fun `verification rejects a name absent from the caption`() {
        assertEquals(0f, GameNameCandidates.verifyAgainstText(palworld, "Stardew Valley"), 0f)
        // Guards the risk the short-window strategy introduces: matching "Pal" must not let an
        // unrelated game through just because it shares a substring with a real one.
        assertEquals(0f, GameNameCandidates.verifyAgainstText(palworld, "Pal-world!: More Than Just Pals"), 0f)
    }

    @Test
    fun `verification prefers the more specific of two present names`() {
        val text = "ranked grind on League of Legends today"
        assertTrue(
            GameNameCandidates.verifyAgainstText(text, "League of Legends") >
                GameNameCandidates.verifyAgainstText(text, "League"),
        )
    }

    @Test
    fun `does not throw on pathological input`() {
        for (input in listOf("", "   ", "🤔🤔🤔", "#", "a", null)) {
            GameNameCandidates.rankedCandidates(input)
        }
    }
}
