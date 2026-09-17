package com.mikhilnaika.continueapp.core.share

import com.mikhilnaika.continueapp.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The friend loop's link contract.
 *
 * [ShareLinks.parseGameId] is reached from an **exported** Activity by two routes any app or
 * any web page can trigger, so "which links do we accept" is a security rule and not a
 * formatting detail. These tests are mostly about what it *refuses*.
 *
 * Reads the host out of [BuildConfig] rather than hardcoding it, so the suite still means
 * something on a checkout whose `local.properties` points somewhere else.
 */
class ShareLinksTest {

    private val host = BuildConfig.SHARE_LINK_HOST

    private fun parseHttps(host: String, vararg segments: String) =
        ShareLinks.parseGameId("https", host, segments.toList())

    @Test
    fun `builds a link the worker route will match`() {
        val link = ShareLinks.gameLink(1942, ShareLinks.Campaign.DARE)
        assertEquals("https://$host/g/1942?c=dare", link)
    }

    @Test
    fun `every campaign has a slug the worker recognises`() {
        // The Worker's CAMPAIGNS map is the other half of this; a slug that isn't in it falls
        // back to neutral wording, silently. Keep the two lists identical by hand.
        val slugs = ShareLinks.Campaign.entries.map { it.slug }.toSet()
        assertEquals(setOf("pick", "dare", "cleared"), slugs)
    }

    @Test
    fun `parses the https link we mint`() {
        assertEquals(1942L, parseHttps(host, "g", "1942"))
    }

    @Test
    fun `parses the custom scheme fallback the landing page fires`() {
        assertEquals(1942L, ShareLinks.parseGameId("continueapp", "g", listOf("1942")))
    }

    @Test
    fun `a link on any other host is not ours`() {
        // The intent filter matches on a path prefix, so without the host check anyone could
        // publish `https://evil.example/g/1` and have our sheet open on it.
        assertNull(parseHttps("evil.example", "g", "1942"))
        assertNull(parseHttps("$host.evil.example", "g", "1942"))
        assertNull(parseHttps("evil.example", "g", "1942"))
    }

    @Test
    fun `host comparison is case insensitive, because dns is`() {
        assertEquals(1942L, parseHttps(host.uppercase(), "g", "1942"))
    }

    @Test
    fun `plain http is refused even on our own host`() {
        assertNull(ShareLinks.parseGameId("http", host, listOf("g", "1942")))
    }

    @Test
    fun `other schemes are refused`() {
        assertNull(ShareLinks.parseGameId("javascript", host, listOf("g", "1942")))
        assertNull(ShareLinks.parseGameId("file", host, listOf("g", "1942")))
        assertNull(ShareLinks.parseGameId(null, host, listOf("g", "1942")))
    }

    @Test
    fun `the custom scheme only answers to its own host`() {
        assertNull(ShareLinks.parseGameId("continueapp", "pile", listOf("1942")))
        assertNull(ShareLinks.parseGameId("continueapp", null, listOf("1942")))
    }

    @Test
    fun `a path that is not exactly g slash id is refused`() {
        assertNull(parseHttps(host, "1942"))
        assertNull(parseHttps(host, "g"))
        assertNull(parseHttps(host, "g", "1942", "extra"))
        assertNull(parseHttps(host, "games", "1942"))
        assertNull(parseHttps(host))
    }

    @Test
    fun `an id that is not a plain positive number is refused`() {
        assertNull(parseHttps(host, "g", "abc"))
        assertNull(parseHttps(host, "g", ""))
        assertNull(parseHttps(host, "g", "-5"), )
        assertNull(parseHttps(host, "g", "0"))
        assertNull(parseHttps(host, "g", " 12 "))
        assertNull(parseHttps(host, "g", "12.5"))
        // Past Long range entirely — `toLongOrNull` returns null rather than throwing, which
        // is the reason it's used instead of `toLong()` in a try/catch.
        assertNull(parseHttps(host, "g", "99999999999999999999999999"))
    }

    @Test
    fun `an id past the worker's own bound is refused before a round trip`() {
        // The Worker routes on `\d{1,9}`, so a 10-digit id could never resolve there. Refusing
        // it here keeps the two ends agreeing and saves a request that was always going to 404.
        assertEquals(999_999_999L, parseHttps(host, "g", "999999999"))
        assertNull(parseHttps(host, "g", "1000000000"))
    }

    @Test
    fun `shared messages always carry a real tappable link`() {
        // The bug this replaces: ACTION_SEND carried an image and nothing else, and the link
        // painted on that image was `continue.app/pile` — a domain that has never existed.
        val pick = ShareLinks.messageFor(ShareLinks.Campaign.PICK, "Hollow Knight", 2879)
        assertTrue(pick.contains("Hollow Knight"))
        assertTrue(pick.contains("https://$host/g/2879?c=pick"))

        val cleared = ShareLinks.messageFor(ShareLinks.Campaign.CLEARED, "Celeste", 7)
        assertTrue(cleared.contains("https://$host/g/7?c=cleared"))

        assertTrue(ShareLinks.messageForPile(412, 87, pileLink = null).contains(ShareLinks.PLAY_STORE_URL))
        assertTrue(ShareLinks.messageForHighScores("Outer Wilds").contains(ShareLinks.PLAY_STORE_URL))
    }

    @Test
    fun `the pile message keeps the numbers it was given`() {
        val message = ShareLinks.messageForPile(412, 87, pileLink = null)
        assertTrue(message.contains("412 hours"))
        assertTrue(message.contains("87 games"))
    }

    @Test
    fun `high scores copy survives having no ranked games yet`() {
        val empty = ShareLinks.messageForHighScores(null)
        assertTrue(empty.contains("top 10"))
        assertTrue(empty.contains(ShareLinks.PLAY_STORE_URL))
    }

    @Test
    fun `the pile message carries the follow link when there is one`() {
        val link = ShareLinks.pileLink("abc_DEF-123")
        assertEquals("https://$host/p#abc_DEF-123", link)
        val message = ShareLinks.messageForPile(412, 87, link)
        assertTrue(message.contains(link))
        assertTrue(!message.contains(ShareLinks.PLAY_STORE_URL))
    }

    @Test
    fun `high scores never carries a pile link`() {
        // Only SHARE YOUR PILE says on screen that the link lists every game.
        assertTrue(!ShareLinks.messageForHighScores("Outer Wilds").contains("/p#"))
    }

    @Test
    fun `parses the pile link we mint, payload from the fragment`() {
        assertEquals("abc", ShareLinks.parsePilePayload("https", host, listOf("p"), "abc"))
    }

    @Test
    fun `parses the pile custom scheme, payload from the path`() {
        assertEquals("abc", ShareLinks.parsePilePayload("continueapp", "p", listOf("abc"), null))
    }

    @Test
    fun `a pile link anywhere else is not ours`() {
        assertNull(ShareLinks.parsePilePayload("https", "evil.example", listOf("p"), "abc"))
        assertNull(ShareLinks.parsePilePayload("https", "$host.evil.example", listOf("p"), "abc"))
        assertNull(ShareLinks.parsePilePayload("http", host, listOf("p"), "abc"))
        assertNull(ShareLinks.parsePilePayload("https", host, listOf("p", "x"), "abc"))
        assertNull(ShareLinks.parsePilePayload("https", host, listOf("g"), "abc"))
        assertNull(ShareLinks.parsePilePayload("continueapp", "g", listOf("abc"), null))
        assertNull(ShareLinks.parsePilePayload("continueapp", "p", listOf("a", "b"), null))
        assertNull(ShareLinks.parsePilePayload("intent", "p", listOf("abc"), null))
    }

    @Test
    fun `an empty or oversized pile payload is refused before decoding`() {
        assertNull(ShareLinks.parsePilePayload("https", host, listOf("p"), null))
        assertNull(ShareLinks.parsePilePayload("https", host, listOf("p"), ""))
        assertNull(ShareLinks.parsePilePayload("https", host, listOf("p"), "a".repeat(4_097)))
        assertEquals(4_096, ShareLinks.parsePilePayload("https", host, listOf("p"), "a".repeat(4_096))?.length)
    }

    @Test
    fun `the play store url points at the release package, not the debug one`() {
        // A debug build carries an `.debug` applicationIdSuffix. Deriving the store URL from
        // BuildConfig.APPLICATION_ID would put a listing that doesn't exist into every share
        // sent from a developer's device.
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.mikhilnaika.continueapp",
            ShareLinks.PLAY_STORE_URL,
        )
    }
}
