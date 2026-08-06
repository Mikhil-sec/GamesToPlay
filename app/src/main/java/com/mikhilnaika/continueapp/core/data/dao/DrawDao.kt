package com.mikhilnaika.continueapp.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mikhilnaika.continueapp.core.data.entity.DrawEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {
    @Query("SELECT * FROM draw_history ORDER BY drawnAt DESC")
    fun observeAll(): Flow<List<DrawEntity>>

    @Query("SELECT * FROM draw_history WHERE drawnAt >= :sinceEpochMs ORDER BY drawnAt DESC")
    suspend fun since(sinceEpochMs: Long): List<DrawEntity>

    @Insert
    suspend fun insert(draw: DrawEntity): Long
}
