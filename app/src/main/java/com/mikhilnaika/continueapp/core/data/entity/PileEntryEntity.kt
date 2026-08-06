package com.mikhilnaika.continueapp.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mikhilnaika.continueapp.core.data.AddSource
import com.mikhilnaika.continueapp.core.data.PileState

/** User data — precious, backed up. docs/05-TECH-ARCHITECTURE.md. */
@Entity(tableName = "pile_entries")
data class PileEntryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val gameId: Long,
    val state: PileState,
    val addedAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val droppedAt: Long? = null,
    val ownedPlatform: String? = null,
    val source: AddSource,
    val hoursPlayed: Float? = null,
    val pinnedUntil: Long? = null,
    val snoozedUntil: Long? = null,
    val lastDrawnAt: Long? = null,
    val notes: String? = null,
)
