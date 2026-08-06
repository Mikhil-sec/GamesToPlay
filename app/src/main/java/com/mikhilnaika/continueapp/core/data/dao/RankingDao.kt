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
}
