package com.mikhilnaika.continueapp.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** docs/02-PRODUCT-SPEC.md §1 — user-created groupings. Free: 2 stacks; Pro: unlimited. */
@Entity(tableName = "stacks")
data class StackEntity(
    @PrimaryKey(autoGenerate = true) val stackId: Long = 0,
    val name: String,
    val emoji: String? = null,
    val sortOrder: Int,
    val createdAt: Long,
)

@Entity(tableName = "stack_members", primaryKeys = ["stackId", "gameId"])
data class StackMemberEntity(
    val stackId: Long,
    val gameId: Long,
    val sortOrder: Int,
)
