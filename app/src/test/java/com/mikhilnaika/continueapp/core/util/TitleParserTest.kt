package com.mikhilnaika.continueapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture strings modeled on real captured text from the platforms named in
 * docs/02-PRODUCT-SPEC.md §2a: YouTube, YouTube Shorts, TikTok, Steam, Reddit.
 * docs/05-TECH-ARCHITECTURE.md calls this "the one piece of logic where correctness is
 * directly visible to judges" — hence the heavy coverage here rather than a couple of cases.
 */
class TitleParserTest {

    @Test
    fun `blank or null input returns no candidates`() {
        assertEquals(emptyList<String>(), TitleParser.extractCandidates(null))
        assertEquals(emptyList<String>(), TitleParser.extractCandidates(""))
        assertEquals(emptyList<String>(), TitleParser.extractCandidates("   "))
    }

    @Test
    fun `hashtag is the strongest signal and comes first`() {
        val candidates = TitleParser.extractCandidates(
            "this boss took me 3 hours 😭 #eldenring #gaming #fyp"
        )
        assertTrue(candidates.isNotEmpty())
        assertEquals("eldenring", candidates.first())
    }

    @Test
    fun `camelCase hashtags are split into words`() {
        val candidates = TitleParser.extractCandidates("finally beat it #BaldursGate3")
        assertTrue(candidates.contains("baldurs gate3"))
    }

    @Test
    fun `pascal case hashtag with multiple words splits on every boundary`() {
        val candidates = TitleParser.extractCandidates("#EldenRingNightreign is out now")
        assertTrue(candidates.contains("elden ring nightreign"))
    }

    @Test
    fun `youtube style title strips bracketed tags and boilerplate`() {
        val candidates = TitleParser.extractCandidates(
            "Elden Ring [4K] (Official Trailer) REVIEW"
        )
        assertEquals("Elden Ring", candidates.last().trim())
    }

    @Test
    fun `video title separator pipe truncates to the part before it`() {
        val candidates = TitleParser.extractCandidates("Hollow Knight Silksong | Full Walkthrough Part 12")
        assertEquals("Hollow Knight Silksong", candidates.last().trim())
    }

    @Test
    fun `strips urls from shared text`() {
        val candidates = TitleParser.extractCandidates(
            "check this out https://youtube.com/watch?v=abc123 so good"
        )
        assertTrue(candidates.last().none { it == '/' })
        assertTrue(!candidates.last().contains("http"))
    }

    @Test
    fun `strips reddit subreddit prefix and handles`() {
        val candidates = TitleParser.extractCandidates("r/gaming just beat @some_streamer's game Celeste")
        val cleaned = candidates.last()
        assertTrue(!cleaned.contains("r/gaming"))
        assertTrue(!cleaned.contains("@some_streamer"))
    }

    @Test
    fun `strips trailing release year noise`() {
        val candidates = TitleParser.extractCandidates("Baldur's Gate 3 2023 GOTY edition gameplay")
        assertTrue(!candidates.last().contains("2023"))
    }

    @Test
    fun `strips ep and part markers`() {
        val candidates = TitleParser.extractCandidates("Silksong Let's Play EP.4 Part 12")
        val cleaned = candidates.last()
        assertTrue(!cleaned.contains("EP.4", ignoreCase = true))
        assertTrue(!cleaned.contains("Part 12", ignoreCase = true))
    }

    @Test
    fun `tiktok style caption with emoji and hashtags produces clean candidates`() {
        val candidates = TitleParser.extractCandidates(
            "beat the final boss on 1 life 🔥🔥 #hollowknightsilksong #boss #fyp"
        )
        assertEquals("hollowknightsilksong", candidates.first())
        assertTrue(candidates.any { it.contains("beat the final boss") })
    }

    @Test
    fun `steam style un-slugged path text passes through mostly unchanged`() {
        val candidates = TitleParser.extractCandidates("Hades II")
        assertEquals("Hades II", candidates.last())
    }

    @Test
    fun `does not throw on pathological input`() {
        val weird = "#".repeat(50) + "()[]||||" + "😀".repeat(20)
        val candidates = TitleParser.extractCandidates(weird)
        // Should complete without throwing; content is not asserted, just resilience.
        assertTrue(candidates.size >= 0)
    }
}
