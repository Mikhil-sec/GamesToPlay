package com.mikhilnaika.continueapp.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mikhilnaika.continueapp.core.data.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE id = :id")
    fun observe(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun get(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE id IN (:ids)")
    fun observeByIds(ids: List<Long>): Flow<List<GameEntity>>

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int

    @Query("SELECT * FROM games WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchByName(query: String, limit: Int = 20): List<GameEntity>

    /**
     * Games the user actually holds whose cached row predates [epochMillis] — the refresh
     * queue for [com.mikhilnaika.continueapp.core.data.GameCacheRepository].
     *
     * Scoped to `pile_entries` on purpose. The `games` table also holds 426 bundled seed rows
     * and everything DISCOVER has ever shown, none of which is worth spending a request on;
     * only a game in someone's pile is one they'll actually look at. Seed ids are negative by
     * construction and can never resolve against IGDB, so they're excluded here rather than
     * being re-requested and re-failing every launch.
     *
     * Oldest first, so successive launches work through a long pile instead of retrying the
     * same slice.
     */
    @Query(
        """
        SELECT g.id FROM games g
        INNER JOIN pile_entries e ON e.gameId = g.id
        WHERE g.id > 0 AND g.cachedAt < :epochMillis
        ORDER BY g.cachedAt ASC
        LIMIT :limit
        """
    )
    suspend fun staleReferencedGameIds(epochMillis: Long, limit: Int): List<Long>

    /**
     * Backfills key art for a game already in the pile.
     *
     * A targeted UPDATE rather than a REPLACE upsert on purpose: rows added before the Worker
     * started returning `backgroundUrl` are otherwise fine, and REPLACE would delete-then-insert
     * the row, cascading to anything referencing it.
     */
    @Query("UPDATE games SET backgroundUrl = :backgroundUrl WHERE id = :id")
    suspend fun updateBackgroundUrl(id: Long, backgroundUrl: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<GameEntity>)
}
