package com.mikhilnaika.continueapp.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mikhilnaika.continueapp.core.data.entity.RankingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RankingDao {
    @Query("SELECT * FROM rankings ORDER BY position ASC")
    fun observeAll(): Flow<List<RankingEntity>>

    @Query("SELECT * FROM rankings ORDER BY position ASC")
    suspend fun getAll(): List<RankingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ranking: RankingEntity)

    @Query("UPDATE rankings SET position = position + 1 WHERE position >= :fromPosition")
    suspend fun shiftDown(fromPosition: Int)

    /**
     * Same reason as `StackDao.removeFromAllStacks` — no foreign key ties a ranking to its pile
     * entry, so a game removed from the pile would otherwise keep its slot in RANK forever with
     * nothing behind it. Leaves the surrounding positions alone: they're a total order, and the
     * gap it opens is invisible because RANK reads them by `ORDER BY position`, not by value.
     */
    @Query("DELETE FROM rankings WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)
}
