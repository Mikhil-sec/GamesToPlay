package com.mikhilnaika.continueapp.core.data

import android.content.Context
import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.entity.GameEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads app/src/main/assets/seed_games.json into Room on first launch, so PILE/DISCOVER
 * have useful data with no network and no live IGDB credentials — docs/08-GAME-DATA.md
 * calls this "mandatory, not optional" after RAWG's outage. Regenerate the asset with
 * tools/generate_seed.mjs once IGDB access is confirmed (docs/09-PENDING-INPUTS.md).
 */
@Serializable
private data class SeedGame(
    val id: Long,
    val slug: String,
    val name: String,
    val coverUrl: String? = null,
    val backgroundUrl: String? = null,
    val released: String? = null,
    val metacritic: Int? = null,
    val rating: Float? = null,
    val playtimeHoursHastily: Int? = null,
    val playtimeHoursNormally: Int? = null,
    val playtimeHoursCompletely: Int? = null,
    val isEstimatedPlaytime: Boolean = false,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val cachedAt: Long = 0L,
)

class SeedLoader(
    private val context: Context,
    private val gameDao: GameDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** No-ops if `games` already has rows — this only ever seeds an empty database. */
    suspend fun loadIfEmpty() {
        if (gameDao.count() > 0) return

        val text = context.assets.open(SEED_ASSET_NAME).use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
        val seedGames = json.decodeFromString<List<SeedGame>>(text)
        val now = System.currentTimeMillis()

        gameDao.upsertAll(
            seedGames.map { seed ->
                GameEntity(
                    id = seed.id,
                    slug = seed.slug,
                    name = seed.name,
                    coverUrl = seed.coverUrl,
                    backgroundUrl = seed.backgroundUrl,
                    released = seed.released,
                    metacritic = seed.metacritic,
                    rating = seed.rating,
                    playtimeHoursHastily = seed.playtimeHoursHastily,
                    playtimeHoursNormally = seed.playtimeHoursNormally,
                    playtimeHoursCompletely = seed.playtimeHoursCompletely,
                    genresJson = json.encodeToString(seed.genres),
                    tagsJson = json.encodeToString(seed.tags),
                    platformsJson = json.encodeToString(seed.platforms),
                    cachedAt = now,
                )
            }
        )
    }

    companion object {
        const val SEED_ASSET_NAME = "seed_games.json"
    }
}
