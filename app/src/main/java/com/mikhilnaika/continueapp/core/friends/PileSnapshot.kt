package com.mikhilnaika.continueapp.core.friends

import com.mikhilnaika.continueapp.core.data.PileState

/**
 * A friend's pile as it travelled inside a link — docs/02-PRODUCT-SPEC.md §6 "FRIENDS".
 *
 * **Why the whole pile rides in the link.** The app has no accounts and the privacy policy
 * promises there is no server-side user data (docs/privacy.html §3, §9). A "follow my pile"
 * feature built the usual way — upload the pile, hand out an id — would break both. So the
 * snapshot is packed into the link's **fragment** (`…/p#<payload>`), the one part of a URL that
 * browsers never send to a server. The Worker serves a static page for `/p` and never sees a
 * single game id; the only copies live on the two phones and in the chat it was sent through.
 *
 * **Why it is signed.** A link is forwarded around group chats, so anyone who received one knows
 * everything in it. Without a signature any of them could mint a link that *overwrites* "Sam's
 * pile" on everyone's phone with whatever they liked. The sender's pile key is an ECDSA P-256
 * key pair generated on their device ([PileIdentity]); the link carries the public half and a
 * signature, and an update is accepted only when it verifies against the key already on file for
 * that friend. The key is random and tied to nothing — not an account, not the device, not the
 * advertising id — and the sender can throw it away at any time from FRIENDS.
 *
 * @property publicKey raw uncompressed P-256 point, 65 bytes (`0x04 || X || Y`).
 * @property sequence monotonically increasing per key. Decides which of two snapshots is newer,
 *   instead of the sender's clock — a phone set a year ahead would otherwise make every later,
 *   correctly-dated share look *older* and be refused forever.
 * @property sharedAtSeconds the sender's clock at share time. Display only.
 * @property sections every pile state, always all five, in [PileSnapshotCodec.SECTION_ORDER].
 * @property ranking HIGH SCORES, best first. May name games that aren't in any section.
 */
class PileSnapshot(
    val publicKey: ByteArray,
    val sequence: Long,
    val sharedAtSeconds: Long,
    val sections: Map<PileState, Section>,
    val ranking: List<Long>,
) {
    /**
     * One pile state.
     *
     * @property total how many games the sender really has in this state.
     * @property gameIds the ones that fit in the link, ascending. Fewer than [total] only for
     *   piles past [PileSnapshotCodec.MAX_GAMES]; the friend's screen says so rather than
     *   pretending the list is complete.
     */
    data class Section(val total: Int, val gameIds: List<Long>)

    /** The key as stored and compared — URL-safe base64, no padding. */
    val publicKeyId: String get() = PileSnapshotCodec.base64(publicKey)

    fun section(state: PileState): Section = sections[state] ?: Section(0, emptyList())

    /** Every game id the snapshot names, sections first, ranking-only games last. */
    fun allGameIds(): List<Long> {
        val ids = LinkedHashSet<Long>()
        PileSnapshotCodec.SECTION_ORDER.forEach { ids += section(it).gameIds }
        ids += ranking
        return ids.toList()
    }

    fun totalGames(): Int = PileSnapshotCodec.SECTION_ORDER.sumOf { section(it).total }
}
