package com.mikhilnaika.continueapp.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mikhilnaika.continueapp.core.data.Mood
import com.mikhilnaika.continueapp.core.data.TimeBudget

/** docs/02-PRODUCT-SPEC.md §3 — one row per pull of the DRAW lever. */
@Entity(tableName = "draw_history")
data class DrawEntity(
    @PrimaryKey(autoGenerate = true) val drawId: Long = 0,
    val drawnAt: Long,
    val timeBudget: TimeBudget,
    val mood: Mood,
    val platformsJson: String,
    val gameIdsJson: String,
    val acceptedGameId: Long? = null,
)
