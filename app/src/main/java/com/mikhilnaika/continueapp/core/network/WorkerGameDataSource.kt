package com.mikhilnaika.continueapp.core.network

import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.entity.GameEntity
import com.mikhilnaika.continueapp.core.network.dto.GameDto
import com.mikhilnaika.continueapp.core.network.dto.ResolveRequest
import com.mikhilnaika.continueapp.core.network.dto.ResolveResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

/**
 * Real [GameDataSource]: calls the Worker, and falls back to the offline seed set (already
 * loaded into Room by [com.mikhilnaika.continueapp.core.data.SeedLoader]) whenever the
 * network is unavailable or the Worker hasn't been deployed yet. This is what makes
 * "offline-first" true for DISCOVER, not just PILE — docs/05-TECH-ARCHITECTURE.md.
 */
class WorkerGameDataSource @Inject constructor(
    private val api: WorkerApi,
    private val gameDao: GameDao,
) : GameDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String): List<GameDto> = runCatching {
        api.searchGames(query).results
    }.getOrElse { fallbackSearch(query) }

    override suspend fun trending(): List<GameDto> = runCatching {
        api.trending().results
    }.getOrElse { fallbackRandom() }

    override suspend fun shortAndSweet(): List<GameDto> = runCatching {
        api.shortAndSweet().results
    }.getOrElse {
        fallbackRandom().filter { (it.playtimeHoursNormally ?: Int.MAX_VALUE) < 8 }
    }

    override suspend fun resolve(text: String?, subject: String?): ResolveResponse = runCatching {
        api.resolve(ResolveRequest(text = text, subject = subject))
    }.getOrElse {
        // Worker unreachable: never dead-end (docs/02-PRODUCT-SPEC.md degradation ladder) —
        // the caller is expected to fall through to the manual search field.
        ResolveResponse(resolvedTitle = null, source = "offline", candidates = emptyList(), needsManualEntry = true)
    }

    private suspend fun fallbackSearch(query: String): List<GameDto> =
        runCatching { gameDao.searchByName(query) }.getOrDefault(emptyList()).map { it.toDto() }

    private suspend fun fallbackRandom(limit: Int = 20): List<GameDto> =
        runCatching { gameDao.searchByName("", limit) }.getOrDefault(emptyList()).map { it.toDto() }

    private fun GameEntity.toDto(): GameDto = GameDto(
        id = id,
        slug = slug,
        name = name,
        coverUrl = coverUrl,
        backgroundUrl = backgroundUrl,
        released = released,
        metacritic = metacritic,
        rating = rating,
        playtimeHoursHastily = playtimeHoursHastily,
        playtimeHoursNormally = playtimeHoursNormally,
        playtimeHoursCompletely = playtimeHoursCompletely,
        genres = runCatching { json.decodeFromString<List<String>>(genresJson) }.getOrDefault(emptyList()),
        tags = runCatching { json.decodeFromString<List<String>>(tagsJson) }.getOrDefault(emptyList()),
        platforms = runCatching { json.decodeFromString<List<String>>(platformsJson) }.getOrDefault(emptyList()),
    )
}

/** Marker so call sites can catch "no connectivity" distinctly if ever needed. */
class WorkerUnreachableException(cause: Throwable) : IOException(cause)
