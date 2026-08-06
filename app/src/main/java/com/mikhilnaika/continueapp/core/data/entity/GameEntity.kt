package com.mikhilnaika.continueapp.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cache of IGDB data (via the Worker), never user data — docs/05-TECH-ARCHITECTURE.md.
 * Three playtime fields, not one: IGDB's game_time_to_beat gives hastily/normally/completely
 * in seconds, converted to hours before storage here for simplicity everywhere else.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Long,
    val slug: String,
    val name: String,
    val coverUrl: String?,
    val backgroundUrl: String?,
    val released: String?,
    val metacritic: Int?,
    val rating: Float?,
    val playtimeHoursHastily: Int?,
    val playtimeHoursNormally: Int?,
    val playtimeHoursCompletely: Int?,
    val genresJson: String,
    val tagsJson: String,
    val platformsJson: String,
    val cachedAt: Long,
)
