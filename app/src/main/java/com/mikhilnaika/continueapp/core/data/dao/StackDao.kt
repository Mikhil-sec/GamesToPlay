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

    @Query("UPDATE stacks SET name = :name, emoji = :emoji WHERE stackId = :stackId")
    suspend fun rename(stackId: Long, name: String, emoji: String?)

    @Query("SELECT * FROM stack_members WHERE stackId = :stackId ORDER BY sortOrder ASC")
    fun observeMembers(stackId: Long): Flow<List<StackMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMember(member: StackMemberEntity)

    @Query("DELETE FROM stack_members WHERE stackId = :stackId AND gameId = :gameId")
    suspend fun removeMember(stackId: Long, gameId: Long)

    /**
     * Drop a game from every stack it's in. There are no foreign keys between `stack_members`
     * and `pile_entries`, so removing a game from the pile has to sweep this table by hand —
     * otherwise the game keeps appearing inside its stacks with no row left to open.
     */
    @Query("DELETE FROM stack_members WHERE gameId = :gameId")
    suspend fun removeFromAllStacks(gameId: Long)
}
