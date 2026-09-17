package com.mikhilnaika.continueapp.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.entity.FriendEntity
import com.mikhilnaika.continueapp.core.data.entity.FriendGameEntity
import com.mikhilnaika.continueapp.core.data.entity.FriendRankEntity
import kotlinx.coroutines.flow.Flow

/**
 * A game in a friend's pile, with whatever this device knows about it.
 *
 * A **LEFT** join, unlike the pile's queries: a friend's games arrive as bare ids, and a row whose
 * details haven't been fetched yet still has to show up (as a placeholder) rather than silently
 * vanish from their pile until the network comes back.
 */
data class FriendGameRow(
    val gameId: Long,
    val state: PileState,
    val name: String?,
    val coverUrl: String?,
    val playtimeHoursHastily: Int?,
    val playtimeHoursNormally: Int?,
    val playtimeHoursCompletely: Int?,
    /** Whether this game is also somewhere in *your* pile. */
    val inMyPile: Boolean,
)

data class FriendRankRow(
    val position: Int,
    val gameId: Long,
    val name: String?,
    val coverUrl: String?,
    val inMyPile: Boolean,
)

data class FriendCoverRow(val friendId: Long, val coverUrl: String)

@Dao
abstract class FriendDao {

    @Query("SELECT * FROM friends ORDER BY sharedAt DESC")
    abstract fun observeFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends ORDER BY name COLLATE NOCASE ASC")
    abstract suspend fun getFriends(): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE id = :id")
    abstract fun observeFriend(id: Long): Flow<FriendEntity?>

    @Query("SELECT * FROM friends WHERE id = :id")
    abstract suspend fun getFriend(id: Long): FriendEntity?

    @Query("SELECT * FROM friends WHERE publicKey = :publicKey LIMIT 1")
    abstract suspend fun findByPublicKey(publicKey: String): FriendEntity?

    @Query("SELECT COUNT(*) FROM friends")
    abstract suspend fun countFriends(): Int

    /**
     * Up to a few covers per friend for the FRIENDS list — what they're playing first, then the
     * rest of their pile. Ordered here, trimmed per friend by the caller.
     */
    @Query(
        """
        SELECT fg.friendId AS friendId, g.coverUrl AS coverUrl
        FROM friend_games fg
        INNER JOIN games g ON g.id = fg.gameId
        WHERE g.coverUrl IS NOT NULL AND fg.state IN ('PLAYING', 'BACKLOG')
        ORDER BY fg.friendId, CASE fg.state WHEN 'PLAYING' THEN 0 ELSE 1 END, g.cachedAt DESC
        """
    )
    abstract fun observeCovers(): Flow<List<FriendCoverRow>>

    @Query(
        """
        SELECT fg.gameId AS gameId, fg.state AS state, g.name AS name, g.coverUrl AS coverUrl,
               g.playtimeHoursHastily AS playtimeHoursHastily,
               g.playtimeHoursNormally AS playtimeHoursNormally,
               g.playtimeHoursCompletely AS playtimeHoursCompletely,
               EXISTS(SELECT 1 FROM pile_entries e WHERE e.gameId = fg.gameId) AS inMyPile
        FROM friend_games fg
        LEFT JOIN games g ON g.id = fg.gameId
        WHERE fg.friendId = :friendId
        ORDER BY g.name IS NULL, g.name COLLATE NOCASE ASC
        """
    )
    abstract fun observeGames(friendId: Long): Flow<List<FriendGameRow>>

    @Query(
        """
        SELECT r.position AS position, r.gameId AS gameId, g.name AS name, g.coverUrl AS coverUrl,
               EXISTS(SELECT 1 FROM pile_entries e WHERE e.gameId = r.gameId) AS inMyPile
        FROM friend_ranks r
        LEFT JOIN games g ON g.id = r.gameId
        WHERE r.friendId = :friendId
        ORDER BY r.position ASC
        """
    )
    abstract fun observeRanks(friendId: Long): Flow<List<FriendRankRow>>

    @Query("SELECT * FROM friend_games WHERE friendId = :friendId")
    abstract suspend fun getGames(friendId: Long): List<FriendGameEntity>

    /**
     * Every id a friend's pile names that this device has no usable row for — never fetched, or
     * only a placeholder from the offline index (`cachedAt` older than the refresh epoch).
     */
    @Query(
        """
        SELECT ids.gameId FROM (
            SELECT gameId FROM friend_games WHERE friendId = :friendId
            UNION
            SELECT gameId FROM friend_ranks WHERE friendId = :friendId
        ) ids
        LEFT JOIN games g ON g.id = ids.gameId
        WHERE g.id IS NULL OR g.cachedAt < :epochMillis
        LIMIT :limit
        """
    )
    abstract suspend fun unhydratedGameIds(friendId: Long, epochMillis: Long, limit: Int): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertFriend(friend: FriendEntity): Long

    @Update
    abstract suspend fun updateFriend(friend: FriendEntity)

    @Query("UPDATE friends SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGames(games: List<FriendGameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRanks(ranks: List<FriendRankEntity>)

    @Query("DELETE FROM friend_games WHERE friendId = :friendId")
    abstract suspend fun deleteGames(friendId: Long)

    @Query("DELETE FROM friend_ranks WHERE friendId = :friendId")
    abstract suspend fun deleteRanks(friendId: Long)

    @Query("DELETE FROM friends WHERE id = :friendId")
    abstract suspend fun deleteFriendRow(friendId: Long)

    /** A new friend and their whole pile, or nothing. */
    @Transaction
    open suspend fun insertWithContents(
        friend: FriendEntity,
        games: List<FriendGameEntity>,
        ranks: List<FriendRankEntity>,
    ): Long {
        val id = insertFriend(friend)
        insertGames(games.map { it.copy(friendId = id) })
        insertRanks(ranks.map { it.copy(friendId = id) })
        return id
    }

    /** Swaps a friend's pile for a newer snapshot in one step, so the screen never sees half of each. */
    @Transaction
    open suspend fun replaceContents(
        friend: FriendEntity,
        games: List<FriendGameEntity>,
        ranks: List<FriendRankEntity>,
    ) {
        updateFriend(friend)
        deleteGames(friend.id)
        deleteRanks(friend.id)
        insertGames(games)
        insertRanks(ranks)
    }

    /**
     * No foreign keys on the child tables (they'd need their own indices and an enforced PRAGMA
     * for no gain), so removal is one explicit transaction instead of a cascade.
     */
    @Transaction
    open suspend fun deleteFriend(friendId: Long) {
        deleteGames(friendId)
        deleteRanks(friendId)
        deleteFriendRow(friendId)
    }
}
