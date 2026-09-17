package com.mikhilnaika.continueapp.core.friends

import com.mikhilnaika.continueapp.core.data.PileState
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * The byte format of a shared pile, and the only code that reads or writes it.
 *
 * ```
 * u8        version (1)
 * 65 bytes  public key, uncompressed P-256 point
 * varint    sequence
 * varint    sharedAt, epoch seconds
 * 5 ×       [SECTION_ORDER]: varint total, varint count, count × varint (delta from previous id)
 * varint    ranking count, then that many varint ids, best first
 * u8        signature length
 * n bytes   DER ECDSA signature over DOMAIN + every byte above the length byte
 * ```
 *
 * **Deliberately no compression.** Sorted ids delta-encode to about two bytes each, which is
 * what a deflate pass would mostly have bought — and not decompressing means there's no
 * decompression bomb to defend against. A 400-game pile is roughly 1.1 KB, ~1,500 characters.
 *
 * **The decoder is a security boundary.** Anything on the internet can hand the app a link, and
 * any app on the device can fire the `continueapp://` form. So [decode] is strict rather than
 * forgiving: every length is bounded before it's used, ids must be in range and strictly
 * ascending, no id may appear in two states, the input must be consumed exactly, and the
 * signature must verify. Anything else is `null` — the sheet shows "that link is damaged"
 * rather than importing a half-understood pile.
 */
object PileSnapshotCodec {

    private const val VERSION: Int = 1

    /**
     * Prepended to the signed bytes and never transmitted. Binds a signature to *this* use, so a
     * signature produced by the same key for anything else can never be replayed as a pile.
     */
    private val DOMAIN = "CONTINUE?/pile-link/v1\n".toByteArray(Charsets.US_ASCII)

    /**
     * Fixed wire order. It is also the priority order when a pile is too big to fit: what
     * someone is playing and has finished says more about them than a wishlist does.
     */
    val SECTION_ORDER: List<PileState> = listOf(
        PileState.PLAYING,
        PileState.COMPLETED,
        PileState.BACKLOG,
        PileState.WISHLIST,
        PileState.DROPPED,
    )

    /** Most games one link may carry, across all five states. Keeps a link under ~2,000 chars. */
    const val MAX_GAMES = 400

    /** Most HIGH SCORES entries one link may carry. */
    const val MAX_RANKED = 50

    /** Same bound as the Worker's `\d{1,9}` routes and `ShareLinks`. */
    const val MAX_GAME_ID = 999_999_999L

    /** A "total" beyond this isn't a pile, it's a crafted number. */
    private const val MAX_SECTION_TOTAL = 100_000

    /** Longest payload text [decode] will look at: [MAX_GAMES] at 5 bytes each is still under it. */
    const val MAX_PAYLOAD_CHARS = 4_096

    private const val MIN_SIGNATURE_BYTES = 8
    private const val MAX_SIGNATURE_BYTES = 72
    private const val MAX_VARINT_BYTES = 5

    private val PAYLOAD_CHARS = Regex("^[A-Za-z0-9_-]+$")

    /**
     * Builds and signs a snapshot of a pile.
     *
     * @param entries every pile entry as (gameId, state), **most recently added first** — that
     *   order decides which games survive when a state has to be truncated.
     * @param ranking HIGH SCORES ids, best first.
     * @return the URL-safe payload for [com.mikhilnaika.continueapp.core.share.ShareLinks.pileLink].
     */
    fun encode(
        signer: PileSigner,
        sequence: Long,
        sharedAtSeconds: Long,
        entries: List<Pair<Long, PileState>>,
        ranking: List<Long>,
    ): String {
        require(sequence >= 0 && sharedAtSeconds >= 0)

        // Games the recipient could never look up — the negative-id bundled seed rows, anything
        // out of range — are left out entirely, totals included, and each game counts once.
        val seen = HashSet<Long>()
        val byState = SECTION_ORDER.associateWith { mutableListOf<Long>() }
        for ((id, state) in entries) {
            if (id !in 1..MAX_GAME_ID || !seen.add(id)) continue
            byState.getValue(state) += id
        }

        var budget = MAX_GAMES
        val out = ByteArrayOutputStream()
        out.write(VERSION)
        out.write(signer.publicKey)
        out.writeVarint(sequence)
        out.writeVarint(sharedAtSeconds)
        for (state in SECTION_ORDER) {
            val all = byState.getValue(state)
            val included = all.take(budget).sorted()
            budget -= included.size
            out.writeVarint(all.size.toLong())
            out.writeVarint(included.size.toLong())
            var previous = 0L
            for (id in included) {
                out.writeVarint(id - previous)
                previous = id
            }
        }

        val ranked = ranking.filter { it in 1..MAX_GAME_ID }.distinct().take(MAX_RANKED)
        out.writeVarint(ranked.size.toLong())
        ranked.forEach { out.writeVarint(it) }

        val body = out.toByteArray()
        val signature = signer.sign(DOMAIN + body)
        check(signature.size in MIN_SIGNATURE_BYTES..MAX_SIGNATURE_BYTES) { "Unexpected signature size" }
        out.write(signature.size)
        out.write(signature)
        return base64(out.toByteArray())
    }

    /** The verified snapshot inside [payload], or null for anything malformed, oversized or forged. */
    fun decode(payload: String?): PileSnapshot? {
        if (payload == null || payload.length > MAX_PAYLOAD_CHARS || !PAYLOAD_CHARS.matches(payload)) return null
        val bytes = try {
            Base64.getUrlDecoder().decode(payload)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return try {
            parse(bytes)
        } catch (_: MalformedPayload) {
            null
        }
    }

    private fun parse(bytes: ByteArray): PileSnapshot? {
        val reader = Reader(bytes)
        if (reader.byte() != VERSION) return null
        val publicKey = reader.bytes(PileKeys.RAW_PUBLIC_KEY_BYTES)
        val sequence = reader.varint()
        val sharedAt = reader.varint()

        val seen = HashSet<Long>()
        var included = 0
        val sections = LinkedHashMap<PileState, PileSnapshot.Section>()
        for (state in SECTION_ORDER) {
            val total = reader.varint()
            val count = reader.varint()
            if (total > MAX_SECTION_TOTAL || count > total) return null
            included += count.toInt()
            if (included > MAX_GAMES) return null
            val ids = ArrayList<Long>(count.toInt())
            var previous = 0L
            repeat(count.toInt()) {
                val delta = reader.varint()
                // Strictly ascending is what makes the delta encoding unambiguous; a zero delta
                // would be a duplicate.
                if (delta < 1) throw MalformedPayload
                val id = previous + delta
                if (id > MAX_GAME_ID || !seen.add(id)) throw MalformedPayload
                ids += id
                previous = id
            }
            sections[state] = PileSnapshot.Section(total.toInt(), ids)
        }

        val rankedCount = reader.varint()
        if (rankedCount > MAX_RANKED) return null
        val rankSeen = HashSet<Long>()
        val ranking = List(rankedCount.toInt()) {
            val id = reader.varint()
            if (id !in 1..MAX_GAME_ID || !rankSeen.add(id)) throw MalformedPayload
            id
        }

        val signedEnd = reader.position
        val signatureLength = reader.byte()
        if (signatureLength !in MIN_SIGNATURE_BYTES..MAX_SIGNATURE_BYTES) return null
        val signature = reader.bytes(signatureLength)
        if (!reader.atEnd()) return null

        val signed = DOMAIN + bytes.copyOfRange(0, signedEnd)
        if (!PileKeys.verify(publicKey, signed, signature)) return null

        return PileSnapshot(publicKey, sequence, sharedAt, sections, ranking)
    }

    fun base64(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private object MalformedPayload : Exception() {
        private fun readResolve(): Any = MalformedPayload
    }

    private class Reader(private val bytes: ByteArray) {
        var position = 0
            private set

        fun atEnd(): Boolean = position == bytes.size

        fun byte(): Int {
            if (position >= bytes.size) throw MalformedPayload
            return bytes[position++].toInt() and 0xFF
        }

        fun bytes(count: Int): ByteArray {
            if (count < 0 || bytes.size - position < count) throw MalformedPayload
            return bytes.copyOfRange(position, position + count).also { position += count }
        }

        /** Unsigned LEB128, at most [MAX_VARINT_BYTES] bytes — 35 bits, far past any real value. */
        fun varint(): Long {
            var result = 0L
            var shift = 0
            repeat(MAX_VARINT_BYTES) {
                val b = byte()
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
            throw MalformedPayload
        }
    }

    private fun ByteArrayOutputStream.writeVarint(value: Long) {
        require(value >= 0 && value < (1L shl (7 * MAX_VARINT_BYTES)))
        var remaining = value
        while (remaining >= 0x80) {
            write(((remaining and 0x7F) or 0x80).toInt())
            remaining = remaining ushr 7
        }
        write(remaining.toInt())
    }
}
