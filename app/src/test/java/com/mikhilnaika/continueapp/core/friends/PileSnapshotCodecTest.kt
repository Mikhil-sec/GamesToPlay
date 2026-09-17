package com.mikhilnaika.continueapp.core.friends

import com.mikhilnaika.continueapp.core.data.PileState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * A pile link is attacker-shaped input that reaches an exported Activity, so most of this suite
 * is about what the decoder **refuses**: tampering, key substitution, truncation, crafted
 * lengths and duplicate ids. Real ECDSA keys throughout, not a fake signer — a stubbed
 * signature check would prove nothing about the one property that matters.
 */
class PileSnapshotCodecTest {

    private val signer = KeyPairSigner(PileKeys.generate())

    private fun encode(
        entries: List<Pair<Long, PileState>>,
        ranking: List<Long> = emptyList(),
        sequence: Long = 7,
        sharedAt: Long = 1_789_000_000,
        by: PileSigner = signer,
    ) = PileSnapshotCodec.encode(by, sequence, sharedAt, entries, ranking)

    private fun bytes(payload: String) = Base64.getUrlDecoder().decode(payload)
    private fun text(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    @Test
    fun `a pile survives the round trip`() {
        val payload = encode(
            entries = listOf(
                119133L to PileState.PLAYING,
                1942L to PileState.BACKLOG,
                7346L to PileState.BACKLOG,
                26758L to PileState.COMPLETED,
                11L to PileState.WISHLIST,
                999_999_999L to PileState.DROPPED,
            ),
            ranking = listOf(26758L, 1942L, 5L),
        )
        val snapshot = PileSnapshotCodec.decode(payload)
        assertNotNull(snapshot)
        snapshot!!

        assertArrayEquals(signer.publicKey, snapshot.publicKey)
        assertEquals(7L, snapshot.sequence)
        assertEquals(1_789_000_000L, snapshot.sharedAtSeconds)
        assertEquals(listOf(119133L), snapshot.section(PileState.PLAYING).gameIds)
        assertEquals(listOf(1942L, 7346L), snapshot.section(PileState.BACKLOG).gameIds)
        assertEquals(2, snapshot.section(PileState.BACKLOG).total)
        assertEquals(listOf(26758L), snapshot.section(PileState.COMPLETED).gameIds)
        assertEquals(listOf(11L), snapshot.section(PileState.WISHLIST).gameIds)
        assertEquals(listOf(999_999_999L), snapshot.section(PileState.DROPPED).gameIds)
        assertEquals(listOf(26758L, 1942L, 5L), snapshot.ranking)
        assertEquals(6, snapshot.totalGames())
    }

    /**
     * A link minted by `tools/make_pile_link.py` — a separate implementation of the format, in a
     * different language and crypto library. If this stops decoding, the wire format changed,
     * and every link already sitting in someone's chat history just broke with it.
     */
    @Test
    fun `a link from the independent python encoder decodes exactly`() {
        val fixture = "AQQqOyOivKRjnR4j0mPHftXHf1Cum9bA36Rj_0sb3UrJidW4NXNqtkrMxoKdr_HpkVxyRiQtbFkqiG3pps12-snaA8Dyh9UGAgKWD8eTBwICsjnUlwEGBvwHgU-EG_NR3AjW7AIBAYOjBwEB1Q4DhtEBsjmWD0cwRQIhAPjZx897tInHjBG7IudC7GTaQeSI6WR2-5i9n4_fscUQAiAhOHY-J4bI6iw4SExr4hHMGQ1hKDNoEf7knYp0gRMQzQ"
        val snapshot = PileSnapshotCodec.decode(fixture)!!
        assertEquals(3L, snapshot.sequence)
        assertEquals(1_789_000_000L, snapshot.sharedAtSeconds)
        assertEquals(listOf(1942L, 119133L), snapshot.section(PileState.PLAYING).gameIds)
        assertEquals(listOf(7346L, 26758L), snapshot.section(PileState.COMPLETED).gameIds)
        assertEquals(listOf(1020L, 11133L, 14593L, 25076L, 26192L, 72870L), snapshot.section(PileState.BACKLOG).gameIds)
        assertEquals(listOf(119171L), snapshot.section(PileState.WISHLIST).gameIds)
        assertEquals(listOf(1877L), snapshot.section(PileState.DROPPED).gameIds)
        assertEquals(listOf(26758L, 7346L, 1942L), snapshot.ranking)
    }

    @Test
    fun `an empty pile is still a valid link`() {
        val snapshot = PileSnapshotCodec.decode(encode(emptyList()))!!
        assertEquals(0, snapshot.totalGames())
        assertTrue(snapshot.allGameIds().isEmpty())
    }

    @Test
    fun `payload uses only url-safe characters`() {
        val payload = encode((1L..300L).map { it * 997 to PileState.BACKLOG })
        assertTrue(payload.matches(Regex("^[A-Za-z0-9_-]+$")))
    }

    @Test
    fun `unshareable ids are dropped and each game counts once`() {
        // Negative ids are bundled seed rows; nobody else could ever look them up.
        val snapshot = PileSnapshotCodec.decode(
            encode(
                listOf(
                    -5L to PileState.BACKLOG,
                    0L to PileState.BACKLOG,
                    1_000_000_000L to PileState.BACKLOG,
                    42L to PileState.PLAYING,
                    42L to PileState.BACKLOG,
                )
            )
        )!!
        assertEquals(listOf(42L), snapshot.section(PileState.PLAYING).gameIds)
        assertEquals(0, snapshot.section(PileState.BACKLOG).total)
    }

    @Test
    fun `a huge pile is truncated by priority, keeps real totals, and stays linkable`() {
        val entries = (1L..600L).map { it * 1_000 to PileState.BACKLOG } +
            (601L..620L).map { it * 1_000 to PileState.PLAYING } +
            (621L..700L).map { it * 1_000 to PileState.WISHLIST }
        val payload = encode(entries, ranking = (1L..80L).map { it * 1_000 })
        val snapshot = PileSnapshotCodec.decode(payload)!!

        assertEquals(20, snapshot.section(PileState.PLAYING).gameIds.size)
        assertEquals(600, snapshot.section(PileState.BACKLOG).total)
        assertEquals(PileSnapshotCodec.MAX_GAMES - 20, snapshot.section(PileState.BACKLOG).gameIds.size)
        assertEquals(80, snapshot.section(PileState.WISHLIST).total)
        assertTrue(snapshot.section(PileState.WISHLIST).gameIds.isEmpty())
        assertEquals(PileSnapshotCodec.MAX_RANKED, snapshot.ranking.size)
        // Most-recent-first input: the first 380 backlog entries are the ones kept.
        assertEquals((1L..380L).map { it * 1_000 }, snapshot.section(PileState.BACKLOG).gameIds)
        assertTrue("payload was ${payload.length} chars", payload.length < 2_100)
    }

    @Test
    fun `a realistic worst case still fits the decoder's own limit`() {
        // Large, sparse ids cost the most bytes per game.
        val entries = (1L..400L).map { 999_999_999L - it * 2_000_000 to PileState.BACKLOG }
        val payload = encode(entries, ranking = (1L..50L).map { 999_999_999L - it })
        assertTrue(payload.length <= PileSnapshotCodec.MAX_PAYLOAD_CHARS)
        assertNotNull(PileSnapshotCodec.decode(payload))
    }

    @Test
    fun `flipping any single byte breaks the link`() {
        val original = bytes(encode(listOf(1942L to PileState.BACKLOG, 7L to PileState.COMPLETED), listOf(7L)))
        for (i in original.indices) {
            val tampered = original.copyOf().also { it[i] = (it[i].toInt() xor 0x01).toByte() }
            assertNull("byte $i was flipped and still accepted", PileSnapshotCodec.decode(text(tampered)))
        }
    }

    @Test
    fun `swapping in someone else's key breaks the link`() {
        // The impersonation attack: keep Sam's body, claim to be Sam, sign nothing.
        val sam = KeyPairSigner(PileKeys.generate())
        val forged = bytes(encode(listOf(1L to PileState.BACKLOG)))
        sam.publicKey.copyInto(forged, destinationOffset = 1)
        assertNull(PileSnapshotCodec.decode(text(forged)))
    }

    @Test
    fun `a body re-signed by another key decodes as that key, never as the original`() {
        val mallory = KeyPairSigner(PileKeys.generate())
        val snapshot = PileSnapshotCodec.decode(encode(listOf(1L to PileState.BACKLOG), by = mallory))!!
        assertArrayEquals(mallory.publicKey, snapshot.publicKey)
        assertFalse(snapshot.publicKey.contentEquals(signer.publicKey))
    }

    @Test
    fun `every truncation is refused`() {
        // Messengers do cut long links short; a prefix must never import as a smaller pile.
        val payload = encode((1L..40L).map { it to PileState.BACKLOG })
        for (length in 0 until payload.length) {
            assertNull("prefix of length $length accepted", PileSnapshotCodec.decode(payload.take(length)))
        }
    }

    @Test
    fun `trailing bytes are refused`() {
        val payload = encode(listOf(1L to PileState.BACKLOG))
        assertNull(PileSnapshotCodec.decode(text(bytes(payload) + byteArrayOf(0))))
    }

    @Test
    fun `junk, oversize and non-url characters are refused without throwing`() {
        assertNull(PileSnapshotCodec.decode(null))
        assertNull(PileSnapshotCodec.decode(""))
        assertNull(PileSnapshotCodec.decode("not a pile"))
        assertNull(PileSnapshotCodec.decode("abc+/="))
        assertNull(PileSnapshotCodec.decode("A"))
        assertNull(PileSnapshotCodec.decode("A".repeat(PileSnapshotCodec.MAX_PAYLOAD_CHARS + 1)))
        assertNull(PileSnapshotCodec.decode("_".repeat(PileSnapshotCodec.MAX_PAYLOAD_CHARS)))
    }

    // --- Crafted payloads: correctly signed, structurally hostile -------------------------------

    /** Signs an arbitrary body exactly as the codec would, so structure checks are what's tested. */
    private fun craft(body: ByteArrayOutputStream.() -> Unit): String {
        val out = ByteArrayOutputStream()
        out.write(1)
        out.write(signer.publicKey)
        out.body()
        val signed = out.toByteArray()
        val signature = signer.sign("CONTINUE?/pile-link/v1\n".toByteArray() + signed)
        out.write(signature.size)
        out.write(signature)
        return text(out.toByteArray())
    }

    private fun ByteArrayOutputStream.varint(value: Long) {
        var v = value
        while (v >= 0x80) {
            write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        write(v.toInt())
    }

    private fun ByteArrayOutputStream.header() {
        varint(1)
        varint(1_789_000_000)
    }

    private fun ByteArrayOutputStream.emptySections(count: Int) = repeat(count) {
        varint(0)
        varint(0)
    }

    @Test
    fun `the crafting helper matches the real format`() {
        // Guards the tests below: if this fails, they're testing a different format.
        val payload = craft {
            header()
            emptySections(5)
            varint(0)
        }
        assertNotNull(PileSnapshotCodec.decode(payload))
    }

    @Test
    fun `a signature from a different domain is refused`() {
        val out = ByteArrayOutputStream()
        out.write(1)
        out.write(signer.publicKey)
        out.header()
        out.emptySections(5)
        out.varint(0)
        val signature = signer.sign(out.toByteArray()) // no domain prefix
        out.write(signature.size)
        out.write(signature)
        assertNull(PileSnapshotCodec.decode(text(out.toByteArray())))
    }

    @Test
    fun `duplicate ids are refused, within and across states`() {
        val sameState = craft {
            header()
            varint(2); varint(2); varint(5); varint(0) // 5, then delta 0 = 5 again
            emptySections(4)
            varint(0)
        }
        assertNull(PileSnapshotCodec.decode(sameState))

        val acrossStates = craft {
            header()
            varint(1); varint(1); varint(5)
            varint(1); varint(1); varint(5)
            emptySections(3)
            varint(0)
        }
        assertNull(PileSnapshotCodec.decode(acrossStates))
    }

    @Test
    fun `a count larger than its total is refused`() {
        val payload = craft {
            header()
            varint(1); varint(2); varint(5); varint(1)
            emptySections(4)
            varint(0)
        }
        assertNull(PileSnapshotCodec.decode(payload))
    }

    @Test
    fun `more games than the cap is refused even when signed`() {
        val payload = craft {
            header()
            val n = PileSnapshotCodec.MAX_GAMES + 1L
            varint(n); varint(n)
            repeat(n.toInt()) { varint(1) }
            emptySections(4)
            varint(0)
        }
        assertNull(PileSnapshotCodec.decode(payload))
    }

    @Test
    fun `absurd totals and ids past the bound are refused`() {
        val hugeTotal = craft {
            header()
            varint(10_000_000); varint(0)
            emptySections(4)
            varint(0)
        }
        assertNull(PileSnapshotCodec.decode(hugeTotal))

        val bigId = craft {
            header()
            varint(1); varint(1); varint(1_000_000_000)
            emptySections(4)
            varint(0)
        }
        assertNull(PileSnapshotCodec.decode(bigId))
    }

    @Test
    fun `a ranking that is too long or repeats itself is refused`() {
        val tooLong = craft {
            header()
            emptySections(5)
            varint(PileSnapshotCodec.MAX_RANKED + 1L)
            repeat(PileSnapshotCodec.MAX_RANKED + 1) { varint(it + 1L) }
        }
        assertNull(PileSnapshotCodec.decode(tooLong))

        val repeated = craft {
            header()
            emptySections(5)
            varint(2); varint(9); varint(9)
        }
        assertNull(PileSnapshotCodec.decode(repeated))
    }

    @Test
    fun `an over-long varint is refused`() {
        val payload = craft {
            write(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01))
            varint(0)
            emptySections(5)
            varint(0)
        }
        assertNull(PileSnapshotCodec.decode(payload))
    }

    @Test
    fun `an unknown version is refused`() {
        val good = bytes(craft {
            header()
            emptySections(5)
            varint(0)
        })
        good[0] = 2
        assertNull(PileSnapshotCodec.decode(text(good)))
    }

    @Test
    fun `points off the curve are rejected`() {
        assertTrue(PileKeys.isOnCurve(signer.publicKey))
        val off = signer.publicKey.copyOf().also { it[64] = (it[64].toInt() xor 1).toByte() }
        assertFalse(PileKeys.isOnCurve(off))
        assertFalse(PileKeys.isOnCurve(ByteArray(65)))
        assertFalse(PileKeys.isOnCurve(byteArrayOf(4)))
    }
}
