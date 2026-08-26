package com.mikhilnaika.continueapp.feature.sharetarget

import com.mikhilnaika.continueapp.core.network.dto.ResolveCandidateDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The degradation ladder (docs/02-PRODUCT-SPEC.md §2a), and TikTok's one deviation from it.
 */
class ShareResolutionStateTest {

    private fun candidate(name: String, confidence: Float) =
        ResolveCandidateDto(id = name.hashCode().toLong(), name = name, confidence = confidence, coverUrl = null)

    @Test
    fun `a confident candidate is offered as one tap, TikTok or not`() {
        val strong = listOf(candidate("Elden Ring", 0.97f))
        for (isTikTok in listOf(false, true)) {
            val state = classify("Elden Ring gameplay", strong, needsManualEntry = false, rawText = null, isTikTok = isTikTok)
            assertEquals(
                "a confident hit is always worth a one-tap add (isTikTok=$isTikTok)",
                ShareResolutionState.Confident(strong[0]),
                state,
            )
        }
    }

    @Test
    fun `weak candidates become a chooser for an ordinary share`() {
        val weak = listOf(candidate("Check-In", 0.425f), candidate("Check Inn", 0.425f))
        val state = classify("Check this out", weak, needsManualEntry = false, rawText = "Check this out")
        assertEquals(ShareResolutionState.Ambiguous(weak), state)
    }

    @Test
    fun `weak candidates from TikTok skip the chooser and go straight to the field`() {
        // The real production response for a TikTok share whose caption held no game name:
        // three unrelated games at 0.425, which a chooser would present as if they were answers.
        val weak = listOf(
            candidate("Check-In", 0.425f),
            candidate("Check Inn", 0.425f),
            candidate("Wai-wai Check!", 0.425f),
        )
        val state = classify("Check this out", weak, needsManualEntry = false, rawText = "Check this out", isTikTok = true)
        assertTrue(state is ShareResolutionState.ManualEntry)
        assertNull("the field must be blank, not seeded with caption noise", (state as ShareResolutionState.ManualEntry).prefillText)
        assertEquals(TIKTOK_NOTE, state.note)
    }

    @Test
    fun `a TikTok share with nothing to match gets the blank field and the explanation`() {
        val state = classify(null, emptyList(), needsManualEntry = true, rawText = "vm tiktok com", isTikTok = true)
        assertEquals(ShareResolutionState.ManualEntry(null, TIKTOK_NOTE), state)
    }

    @Test
    fun `an ordinary unmatched share keeps its cleaned prefill and the generic copy`() {
        val state = classify(null, emptyList(), needsManualEntry = true, rawText = "hollow knight silksong")
        assertEquals(ShareResolutionState.ManualEntry("hollow knight silksong", null), state)
    }
}

/**
 * Host-label matching for TikTok links, which is what decides whether a share takes the rule
 * above. Written as a host check rather than a substring check on purpose — see
 * [looksLikeTikTokLink].
 */
class TikTokLinkTest {

    @Test
    fun `the short and canonical link forms TikTok actually shares are recognised`() {
        assertTrue(looksLikeTikTokLink("https://vm.tiktok.com/ZMSkFqPxY/"))
        assertTrue(looksLikeTikTokLink("https://vt.tiktok.com/ZSabc123/"))
        assertTrue(looksLikeTikTokLink("check this https://www.tiktok.com/@gamer/video/7398123456789012345?is_from_webapp=1"))
        // Share sheets hand over scheme-less links at least as often as full ones.
        assertTrue(looksLikeTikTokLink("vm.tiktok.com/ZMSkFqPxY/"))
        assertTrue(looksLikeTikTokLink("HTTPS://VM.TIKTOK.COM/ZMSKFQPXY/"))
    }

    @Test
    fun `a longer host that merely contains the substring is not TikTok`() {
        assertFalse(looksLikeTikTokLink("https://tiktok.com.example.org/video/1"))
        assertFalse(looksLikeTikTokLink("https://eviltiktok.com/video/1"))
        assertFalse(looksLikeTikTokLink("https://youtu.be/dQw4w9WgXcQ"))
        assertFalse(looksLikeTikTokLink("I saw this on tiktok, great game"))
        assertFalse(looksLikeTikTokLink(null))
    }
}
