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
    /**
     * IGDB themes **and** game modes — see the Worker's `toDto`. Projected alongside
     * `genresJson` because [com.mikhilnaika.continueapp.core.util.GameTaxonomy] needs both to
     * answer "is this a horror game / a multiplayer game", and neither list can do it alone.
     */
    val tagsJson: String,
    val platformsJson: String,
    /**
     * Added purely so PILE's own sort options could stop lying. `PileSort.RATING` was
     * documented in the enum, offered nowhere, and implemented as `entries` — the identity
     * function — with the comment "rating not denormalized onto PileEntryWithGame yet";
     * `RELEASE_DATE` silently sorted by date *added*. Both are columns on `games` that this
     * query was already joined against.
     */
    val rating: Float?,
    val released: String?,
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

/** The two facts a shared pile carries per game. */
data class SnapshotEntryRow(val gameId: Long, val state: PileState)

@Dao
interface PileDao {
    /** Every entry, newest first — the order that decides what survives if a shared pile is truncated. */
    @Query("SELECT gameId, state FROM pile_entries ORDER BY addedAt DESC")
    suspend fun getAllForSnapshot(): List<SnapshotEntryRow>

    @Query(
        """
        SELECT e.entryId, e.gameId, e.state, e.addedAt, e.startedAt, e.finishedAt,
               e.ownedPlatform, e.hoursPlayed,
               g.name, g.coverUrl, g.playtimeHoursHastily, g.playtimeHoursNormally,
               g.playtimeHoursCompletely, g.genresJson, g.tagsJson, g.platformsJson,
               g.rating, g.released
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
               g.playtimeHoursCompletely, g.genresJson, g.tagsJson, g.platformsJson,
               g.rating, g.released
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
               g.playtimeHoursCompletely, g.genresJson, g.tagsJson, g.platformsJson,
               g.rating, g.released
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

    /**
     * Ids of every game in the pile, whatever state it's in — what DISCOVER marks as already
     * added.
     *
     * Deliberately *not* the join in [observeAll]: this has to answer "is this game in the
     * pile?", and an entry whose cached `games` row went missing is still in the pile even
     * though the join would drop it. Using the join here would let DISCOVER offer to re-add a
     * game the user already has.
     */
    @Query("SELECT gameId FROM pile_entries")
    fun observeAllGameIds(): Flow<List<Long>>

    @Query("SELECT * FROM pile_entries WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: Long): PileEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: PileEntryEntity): Long

    @Update
    suspend fun update(entry: PileEntryEntity)

    @Delete
    suspend fun delete(entry: PileEntryEntity)

    /**
     * Take a game out of the pile entirely — the undo for an accidental add.
     *
     * Deliberately not the same thing as RETIRED, which is a *state* meaning "I've decided to
     * let this one go" and is counted as such by PROFILE's trophies. A game added by mistake was
     * never in the pile in any meaningful sense, and had no way out before this.
     */
    @Query("DELETE FROM pile_entries WHERE entryId = :entryId")
    suspend fun deleteById(entryId: Long)

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
