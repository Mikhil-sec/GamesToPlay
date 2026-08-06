package com.mikhilnaika.continueapp.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mikhilnaika.continueapp.core.data.entity.StackEntity
import com.mikhilnaika.continueapp.core.data.entity.StackMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StackDao {
    @Query("SELECT * FROM stacks ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<StackEntity>>

    @Query("SELECT COUNT(*) FROM stacks")
    suspend fun count(): Int

    @Insert
    suspend fun insert(stack: StackEntity): Long

    @Delete
    suspend fun delete(stack: StackEntity)

    @Query("SELECT * FROM stack_members WHERE stackId = :stackId ORDER BY sortOrder ASC")
    fun observeMembers(stackId: Long): Flow<List<StackMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMember(member: StackMemberEntity)

    @Query("DELETE FROM stack_members WHERE stackId = :stackId AND gameId = :gameId")
    suspend fun removeMember(stackId: Long, gameId: Long)
}
