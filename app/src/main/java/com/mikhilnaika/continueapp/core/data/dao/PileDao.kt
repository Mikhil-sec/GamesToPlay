package com.mikhilnaika.continueapp.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.entity.PileEntryEntity
import kotlinx.coroutines.flow.Flow

data class PileEntryWithGame(
    val entryId: Long,
    val gameId: Long,
    val state: PileState,
    val addedAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
    val ownedPlatform: String?,
    val hoursPlayed: Float?,
    val name: String,
    val coverUrl: String?,
    val playtimeHoursHastily: Int?,
    val playtimeHoursNormally: Int?,
    val playtimeHoursCompletely: Int?,
    val genresJson: String,
    val platformsJson: String,
)

/** Everything [com.mikhilnaika.continueapp.feature.draw.DrawSelector] needs to score a candidate. */
data class DrawCandidateRow(
    val entryId: Long,
    val gameId: Long,
    val name: String,
    val coverUrl: String?,
    val released: String?,
    val rating: Float?,
    val addedAt: Long,
    val lastDrawnAt: Long?,
    val snoozedUntil: Long?,
    val playtimeHoursHastily: Int?,
    val playtimeHoursNormally: Int?,
    val playtimeHoursCompletely: Int?,
    val genresJson: String,
    val tagsJson: String,
    val platformsJson: String,
)

@Dao
interface PileDao {
    @Query(
        """
        SELECT e.entryId, e.gameId, e.state, e.addedAt, e.startedAt, e.finishedAt,
               e.ownedPlatform, e.hoursPlayed,
               g.name, g.coverUrl, g.playtimeHoursHastily, g.playtimeHoursNormally,
               g.playtimeHoursCompletely, g.genresJson, g.platformsJson
        FROM pile_entries e
        INNER JOIN games g ON g.id = e.gameId
        WHERE e.state = :state
        ORDER BY e.addedAt DESC
        """
    )
    fun observeByState(state: PileState): Flow<List<PileEntryWithGame>>

    @Query(
        """
        SELECT e.entryId, e.gameId, e.state, e.addedAt, e.startedAt, e.finishedAt,
               e.ownedPlatform, e.hoursPlayed,
               g.name, g.coverUrl, g.playtimeHoursHastily, g.playtimeHoursNormally,
               g.playtimeHoursCompletely, g.genresJson, g.platformsJson
        FROM pile_entries e
        INNER JOIN games g ON g.id = e.gameId
        WHERE e.state = :state
        ORDER BY e.addedAt DESC
        """
    )
    suspend fun getByState(state: PileState): List<PileEntryWithGame>

    @Query(
        """
        SELECT e.entryId, e.gameId, e.state, e.addedAt, e.startedAt, e.finishedAt,
               e.ownedPlatform, e.hoursPlayed,
               g.name, g.coverUrl, g.playtimeHoursHastily, g.playtimeHoursNormally,
               g.playtimeHoursCompletely, g.genresJson, g.platformsJson
        FROM pile_entries e
        INNER JOIN games g ON g.id = e.gameId
        ORDER BY e.addedAt DESC
        """
    )
    fun observeAll(): Flow<List<PileEntryWithGame>>

    @Query("SELECT COUNT(*) FROM pile_entries WHERE state = :state")
    suspend fun countByState(state: PileState): Int

    @Query("SELECT COUNT(*) FROM pile_entries WHERE state = :state")
    fun observeCountByState(state: PileState): Flow<Int>

    @Query("SELECT * FROM pile_entries WHERE gameId = :gameId LIMIT 1")
    suspend fun findByGameId(gameId: Long): PileEntryEntity?

    @Query("SELECT * FROM pile_entries WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: Long): PileEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: PileEntryEntity): Long

    @Update
    suspend fun update(entry: PileEntryEntity)

    @Delete
    suspend fun delete(entry: PileEntryEntity)

    /** docs/02-PRODUCT-SPEC.md §3 — the DRAW candidate pool is the BACKLOG. */
    @Query(
        """
        SELECT e.entryId, e.gameId, g.name, g.coverUrl, g.released, g.rating, e.addedAt,
               e.lastDrawnAt, e.snoozedUntil, g.playtimeHoursHastily, g.playtimeHoursNormally,
               g.playtimeHoursCompletely, g.genresJson, g.tagsJson, g.platformsJson
        FROM pile_entries e
        INNER JOIN games g ON g.id = e.gameId
        WHERE e.state = 'BACKLOG'
        """
    )
    suspend fun getDrawCandidates(): List<DrawCandidateRow>

    @Query("UPDATE pile_entries SET lastDrawnAt = :at WHERE entryId = :entryId")
    suspend fun markDrawn(entryId: Long, at: Long)

    /** Swipe LEFT — "not tonight", 14-day decay per docs/02-PRODUCT-SPEC.md §3. */
    @Query("UPDATE pile_entries SET snoozedUntil = :until WHERE entryId = :entryId")
    suspend fun snooze(entryId: Long, until: Long)

    /** Swipe RIGHT — "save for later", pinned to the top of the pile. */
    @Query("UPDATE pile_entries SET pinnedUntil = :until WHERE entryId = :entryId")
    suspend fun pin(entryId: Long, until: Long)

    /** docs/02-PRODUCT-SPEC.md §4 Credits Roll — "YOUR 43RD CLEAR of 2026". */
    @Query("SELECT COUNT(*) FROM pile_entries WHERE state = 'COMPLETED' AND finishedAt >= :yearStartMillis")
    suspend fun countCompletedSince(yearStartMillis: Long): Int
}
