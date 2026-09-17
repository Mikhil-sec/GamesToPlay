package com.mikhilnaika.continueapp.core.friends

import com.mikhilnaika.continueapp.core.data.GameCacheRepository
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.FriendDao
import com.mikhilnaika.continueapp.core.data.dao.PileDao
import com.mikhilnaika.continueapp.core.data.dao.RankingDao
import com.mikhilnaika.continueapp.core.data.entity.FriendEntity
import com.mikhilnaika.continueapp.core.data.entity.FriendGameEntity
import com.mikhilnaika.continueapp.core.data.entity.FriendRankEntity
import com.mikhilnaika.continueapp.core.offline.OfflineGameIndex
import com.mikhilnaika.continueapp.core.share.ShareLinks
import com.mikhilnaika.continueapp.core.util.IgdbImage
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** What tapping a pile link turned out to mean. */
sealed interface PileLinkOutcome {
    /** Malformed, truncated, oversized, or not signed by the key it claims. */
    data object Invalid : PileLinkOutcome

    /** The user tapped their own link. */
    data object OwnPile : PileLinkOutcome

    /** A key this device hasn't seen. Nothing is saved until the user names them. */
    data class NewFriend(
        val snapshot: PileSnapshot,
        /** Existing friends, for "this is someone I already follow" (a reinstall means a new key). */
        val existingFriends: List<FriendEntity>,
        val atLimit: Boolean,
    ) : PileLinkOutcome

    /** A newer snapshot from a known friend — already applied. */
    data class Updated(val friendId: Long, val name: String, val diff: PileDiff) : PileLinkOutcome

    /** The same snapshot again. */
    data class AlreadyCurrent(val friendId: Long, val name: String) : PileLinkOutcome

    /** An older link from a known friend; the newer copy on this device is kept. */
    data class Older(val friendId: Long, val name: String) : PileLinkOutcome
}

/** What changed between two snapshots of the same friend's pile. */
data class PileDiff(val added: Int, val newlyCleared: Int, val removed: Int) {
    val isEmpty: Boolean get() = added == 0 && newlyCleared == 0 && removed == 0
}

/**
 * FRIENDS — following a friend's pile without accounts. See [PileSnapshot] for the design.
 *
 * Owns the three rules that make link-borne data safe to act on:
 * 1. **Nothing is imported without a tap.** A new key only ever produces [PileLinkOutcome.NewFriend];
 *    the row is written by [addFriend], after the user has named them.
 * 2. **Only the same key may update a friend, and only forwards.** [inspect] applies a snapshot
 *    to an existing friend only when it's signed by that friend's key (the codec has already
 *    verified the signature) *and* its sequence is newer. Replaying an old link does nothing.
 * 3. **Bounded.** At most [MAX_FRIENDS] friends; names are trimmed, stripped of control
 *    characters and capped; each snapshot is capped by the codec.
 */
@Singleton
class FriendRepository @Inject constructor(
    private val friendDao: FriendDao,
    private val pileDao: PileDao,
    private val rankingDao: RankingDao,
    private val identity: PileIdentity,
    private val gameCache: GameCacheRepository,
    private val offlineIndex: OfflineGameIndex,
) {

    val friends = friendDao.observeFriends()
    val covers = friendDao.observeCovers()

    fun observeFriend(id: Long) = friendDao.observeFriend(id)
    fun observeGames(id: Long) = friendDao.observeGames(id)
    fun observeRanks(id: Long) = friendDao.observeRanks(id)

    /**
     * A signed link to this user's whole pile, or null if it couldn't be built.
     *
     * Null is survivable by design — every caller falls back to the store link — because a share
     * card must never fail to share over a problem with the optional half of its message.
     */
    suspend fun buildMyPileLink(nowMillis: Long = System.currentTimeMillis()): String? = try {
        val entries = pileDao.getAllForSnapshot().map { it.gameId to it.state }
        val ranking = rankingDao.getAll().map { it.gameId }
        val payload = identity.nextShare { signer, sequence ->
            PileSnapshotCodec.encode(signer, sequence, nowMillis / 1000, entries, ranking)
        }
        ShareLinks.pileLink(payload)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    /** Decodes a link and works out what it means — applying it, if it's an update. */
    suspend fun inspect(payload: String?, nowMillis: Long = System.currentTimeMillis()): PileLinkOutcome {
        val snapshot = PileSnapshotCodec.decode(payload) ?: return PileLinkOutcome.Invalid
        if (snapshot.publicKeyId == identity.publicKeyId()) return PileLinkOutcome.OwnPile

        val known = friendDao.findByPublicKey(snapshot.publicKeyId)
            ?: return PileLinkOutcome.NewFriend(
                snapshot = snapshot,
                existingFriends = friendDao.getFriends(),
                atLimit = friendDao.countFriends() >= MAX_FRIENDS,
            )

        return when {
            snapshot.sequence > known.sequence -> {
                val diff = diff(friendDao.getGames(known.id), snapshot)
                apply(known, snapshot, nowMillis)
                PileLinkOutcome.Updated(known.id, known.name, diff)
            }
            snapshot.sequence == known.sequence -> PileLinkOutcome.AlreadyCurrent(known.id, known.name)
            else -> PileLinkOutcome.Older(known.id, known.name)
        }
    }

    /** Saves a new friend. Returns their id, or null if [name] is unusable or the list is full. */
    suspend fun addFriend(snapshot: PileSnapshot, name: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val clean = cleanName(name) ?: return null
        if (friendDao.countFriends() >= MAX_FRIENDS) return null
        // Re-checked here, not just in `inspect`: the sheet may have sat open while the same
        // link was opened a second time and saved.
        friendDao.findByPublicKey(snapshot.publicKeyId)?.let { return it.id }

        val entity = entityFor(null, clean, snapshot, nowMillis, addedAt = nowMillis)
        val id = friendDao.insertWithContents(entity, gamesFor(0, snapshot), ranksFor(0, snapshot))
        prepareGames(snapshot)
        return id
    }

    /**
     * "This is someone I already follow" — a friend who reinstalled or changed phones shares with
     * a brand-new key. Adopting it keeps their name and their place in the list.
     *
     * Only ever user-initiated, from the import sheet, with the name of the friend being replaced
     * on screen — never inferred.
     */
    suspend fun replaceFriend(friendId: Long, snapshot: PileSnapshot, nowMillis: Long = System.currentTimeMillis()): Long? {
        val existing = friendDao.getFriend(friendId) ?: return null
        if (friendDao.findByPublicKey(snapshot.publicKeyId) != null) return null
        apply(existing, snapshot, nowMillis)
        return existing.id
    }

    suspend fun rename(friendId: Long, name: String): Boolean {
        val clean = cleanName(name) ?: return false
        friendDao.rename(friendId, clean)
        return true
    }

    suspend fun remove(friendId: Long) = friendDao.deleteFriend(friendId)

    /** Fetches whatever this friend's pile still lacks. Bounded; see [GameCacheRepository.hydrate]. */
    suspend fun hydrate(friendId: Long) {
        val ids = friendDao.unhydratedGameIds(
            friendId = friendId,
            epochMillis = GameCacheRepository.CACHE_EPOCH_MILLIS,
            limit = GameCacheRepository.BATCH_LIMIT * GameCacheRepository.MAX_HYDRATE_BATCHES,
        )
        gameCache.hydrate(ids)
    }

    /** Forgets this device's pile key — see [PileIdentity.reset]. */
    suspend fun resetMyKey() = identity.reset()

    private suspend fun apply(existing: FriendEntity, snapshot: PileSnapshot, nowMillis: Long) {
        val updated = entityFor(existing.id, existing.name, snapshot, nowMillis, addedAt = existing.addedAt)
        friendDao.replaceContents(updated, gamesFor(existing.id, snapshot), ranksFor(existing.id, snapshot))
        prepareGames(snapshot)
    }

    /**
     * Names from the bundled index right away, real rows from the Worker in the background.
     *
     * Background on the repository's process scope because the import sheet finishes the moment
     * the user taps DONE.
     */
    private suspend fun prepareGames(snapshot: PileSnapshot) {
        val ids = snapshot.allGameIds()
        val placeholders = runCatching { offlineIndex.lookup(ids) }.getOrDefault(emptyList()).map {
            GameCacheRepository.Placeholder(it.id, it.name, it.coverImageId?.let(IgdbImage::coverUrl))
        }
        gameCache.cachePlaceholders(placeholders)
        gameCache.hydrateInBackground(ids)
    }

    private fun entityFor(
        id: Long?,
        name: String,
        snapshot: PileSnapshot,
        nowMillis: Long,
        addedAt: Long,
    ) = FriendEntity(
        id = id ?: 0,
        name = name,
        publicKey = snapshot.publicKeyId,
        sequence = snapshot.sequence,
        // A sender's clock can be anywhere. The sequence decides ordering; this is only ever
        // displayed, and "shared in 3 months" would be nonsense.
        sharedAt = minOf(snapshot.sharedAtSeconds * 1000, nowMillis),
        receivedAt = nowMillis,
        addedAt = addedAt,
        backlogTotal = snapshot.section(PileState.BACKLOG).total,
        playingTotal = snapshot.section(PileState.PLAYING).total,
        clearedTotal = snapshot.section(PileState.COMPLETED).total,
        retiredTotal = snapshot.section(PileState.DROPPED).total,
        wantedTotal = snapshot.section(PileState.WISHLIST).total,
    )

    private fun gamesFor(friendId: Long, snapshot: PileSnapshot): List<FriendGameEntity> =
        PileSnapshotCodec.SECTION_ORDER.flatMap { state ->
            snapshot.section(state).gameIds.map { FriendGameEntity(friendId, it, state) }
        }

    private fun ranksFor(friendId: Long, snapshot: PileSnapshot): List<FriendRankEntity> =
        snapshot.ranking.mapIndexed { index, gameId -> FriendRankEntity(friendId, index + 1, gameId) }

    companion object {
        /** A generous social circle, and a hard stop for anyone scripting links at a phone. */
        const val MAX_FRIENDS = 100

        const val MAX_NAME_LENGTH = 24

        private val WHITESPACE = Regex("""\s+""")

        /**
         * A display name, or null if nothing printable is left.
         *
         * Control and format characters are removed — the latter includes bidi overrides, which
         * can make a name render as something other than what was typed.
         */
        fun cleanName(raw: String): String? {
            val printable = StringBuilder()
            raw.codePoints().forEach { cp ->
                val type = Character.getType(cp)
                if (!Character.isISOControl(cp) && type != Character.FORMAT.toInt()) {
                    printable.appendCodePoint(cp)
                }
            }
            val collapsed = printable.toString().trim().replace(WHITESPACE, " ")
            if (collapsed.isEmpty()) return null
            // Cut on a code point boundary — a plain `take` can split an emoji in half.
            val limit = collapsed.offsetByCodePoints(0, minOf(MAX_NAME_LENGTH, collapsed.codePointCount(0, collapsed.length)))
            return collapsed.substring(0, limit).trimEnd()
        }

        internal fun diff(before: List<FriendGameEntity>, after: PileSnapshot): PileDiff {
            val old = before.associate { it.gameId to it.state }
            val new = PileSnapshotCodec.SECTION_ORDER
                .flatMap { state -> after.section(state).gameIds.map { it to state } }
                .toMap()
            return PileDiff(
                added = new.keys.count { it !in old },
                newlyCleared = new.count { (id, state) ->
                    state == PileState.COMPLETED && old[id] != null && old[id] != PileState.COMPLETED
                },
                removed = old.keys.count { it !in new },
            )
        }
    }
}
