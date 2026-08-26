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

    /**
     * Closed-test report, 2026-08-23: "resident evil requiem matched to resident evil OG".
     * Mirrors the same five assertions in `worker/test/candidates.test.ts` — the offline and
     * online paths have to agree that a strict prefix of a title is not a confident answer.
     */
    @Test
    fun `a strict prefix of the title in the caption never scores as confident`() {
        val caption = "Resident Evil Requiem"
        val prefix = GameNameCandidates.verifyAgainstText(caption, "Resident Evil")
        assertTrue("the prefix does still appear in the caption", prefix > 0f)
        assertTrue("\"Resident Evil\" scored $prefix — must not be confident", prefix < 0.85f)
        assertTrue(GameNameCandidates.verifyAgainstText(caption, "Resident Evil Requiem") > prefix)
    }

    @Test
    fun `a fragment score stays under the early-stop bar so the search continues`() {
        val score = GameNameCandidates.verifyAgainstText("Resident Evil Requiem gameplay is wild", "Resident Evil")
        assertTrue(score < GameNameCandidates.CONFIDENT_ENOUGH)
    }

    @Test
    fun `prose after a title is not mistaken for the title continuing`() {
        assertTrue(GameNameCandidates.verifyAgainstText("Elden Ring is brutal", "Elden Ring") >= 0.85f)
        assertTrue(GameNameCandidates.verifyAgainstText("Playing Hades tonight", "Hades") >= 0.75f)
    }

    @Test
    fun `video-title boilerplate after a title is not the title continuing`() {
        // A separator ends the title...
        assertTrue(
            GameNameCandidates.verifyAgainstText(
                "RESIDENT EVIL REQUIEM - Announcement Trailer", "Resident Evil Requiem",
            ) >= 0.85f,
        )
        // ...and so does a known trailing word, with no separator at all.
        assertTrue(
            GameNameCandidates.verifyAgainstText(
                "Resident Evil Requiem Official Trailer", "Resident Evil Requiem",
            ) >= 0.85f,
        )
    }

    @Test
    fun `a subtitle the caption spells out beats the base game`() {
        val caption = "Elden Ring Shadow of the Erdtree is brutal"
        assertTrue(
            GameNameCandidates.verifyAgainstText(caption, "Elden Ring: Shadow of the Erdtree") >
                GameNameCandidates.verifyAgainstText(caption, "Elden Ring"),
        )
    }

    /**
     * The report was actually about **"Resident Evil 9 Requiem"** — the way the internet writes
     * a game IGDB calls "Resident Evil Requiem". The digit is the whole problem: '9' is not
     * uppercase, so it broke the proper-noun run in half and didn't count as the title carrying
     * on either. Mirrors `worker/test/candidates.test.ts`.
     */
    @Test
    fun `a sequel number does not break its own title into a run`() {
        val candidates = GameNameCandidates.rankedCandidates("Resident Evil 9 Requiem Trailer")
        val whole = candidates.indexOfFirst { it.equals("Resident Evil 9 Requiem", ignoreCase = true) }
        val prefix = candidates.indexOfFirst { it.equals("Resident Evil", ignoreCase = true) }
        assertTrue("the whole title must be a candidate, got: ${candidates.take(5)}", whole >= 0)
        assertTrue("the full title must be tried before its prefix", whole < prefix)
    }

    @Test
    fun `a following sequel number counts as the title continuing`() {
        assertTrue(GameNameCandidates.verifyAgainstText("Resident Evil 9 Requiem", "Resident Evil") < 0.85f)
    }

    @Test
    fun `the sequel outranks the base game rather than tying with it`() {
        val caption = "Resident Evil 4 Remake is amazing"
        assertTrue(
            GameNameCandidates.verifyAgainstText(caption, "Resident Evil 4") >
                GameNameCandidates.verifyAgainstText(caption, "Resident Evil"),
        )
        assertTrue(GameNameCandidates.verifyAgainstText("Final Fantasy 7 Rebirth", "Final Fantasy") < 0.85f)
    }

    @Test
    fun `a name matches through a sequel number the official title omits`() {
        val caption = "Resident Evil 9 Requiem"
        val relaxed = GameNameCandidates.verifyAgainstText(caption, "Resident Evil Requiem")
        assertTrue("the real game must not score 0 over an interior digit", relaxed > 0f)
        assertTrue(relaxed > GameNameCandidates.verifyAgainstText(caption, "Resident Evil"))
        assertTrue("but not confidently — cf. Mass Effect 2 Legendary Edition", relaxed < 0.85f)
    }

    @Test
    fun `a four-digit year is not read as a sequel number`() {
        assertTrue(GameNameCandidates.verifyAgainstText("Elden Ring 2024 gameplay", "Elden Ring") >= 0.85f)
    }

    @Test
    fun `does not throw on pathological input`() {
        for (input in listOf("", "   ", "🤔🤔🤔", "#", "a", null)) {
            GameNameCandidates.rankedCandidates(input)
        }
    }
}
