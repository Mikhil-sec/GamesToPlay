package com.mikhilnaika.continueapp.core.network

import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.entity.GameEntity
import com.mikhilnaika.continueapp.core.network.dto.GameDto
import com.mikhilnaika.continueapp.core.network.dto.ResolveRequest
import com.mikhilnaika.continueapp.core.network.dto.ResolveResponse
import kotlinx.coroutines.CancellationException
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

    override suspend fun search(query: String): List<GameDto> = fromWorkerOrSeed(
        fromWorker = { api.searchGames(query).results },
        // A genuinely unmatchable query returns empty from Room too, so falling back here
        // can't invent bogus results — it only rescues a Worker that answered with nothing.
        fromSeed = { fallbackSearch(query) },
    )

    // Rails past page 0 deliberately have **no** seed fallback: the seed set is 426 games, so
    // "there is no page 2" is the truth, and inventing one by re-showing page 1 would make
    // SHOW MORE feel broken rather than finished.
    override suspend fun trending(page: Int): List<GameDto> = fromWorkerOrSeed(
        fromWorker = { api.trending(page).results },
        fromSeed = { if (page > 0) emptyList() else fallbackRandom() },
    )

    override suspend fun shortAndSweet(page: Int): List<GameDto> = fromWorkerOrSeed(
        fromWorker = { api.shortAndSweet(page).results },
        fromSeed = {
            if (page > 0) emptyList()
            else fallbackRandom().filter { (it.playtimeHoursNormally ?: Int.MAX_VALUE) < 8 }
        },
    )

    override suspend fun newReleases(page: Int): List<GameDto> = fromWorkerOrSeed(
        fromWorker = { api.newReleases(page).results },
        fromSeed = { if (page > 0) emptyList() else fallbackRandom().sortedByDescending { it.released.orEmpty() } },
    )

    override suspend fun hiddenGems(page: Int): List<GameDto> = fromWorkerOrSeed(
        fromWorker = { api.hiddenGems(page).results },
        fromSeed = { if (page > 0) emptyList() else fallbackRandom().filter { (it.metacritic ?: 0) >= 80 } },
    )

    override suspend fun byGenre(genreName: String, page: Int): List<GameDto> = fromWorkerOrSeed(
        fromWorker = { api.byGenre(genreName, page).results },
        fromSeed = { if (page > 0) emptyList() else fallbackRandom().filter { genreName in it.genres } },
    )

    /**
     * Falls back to the bundled seed set when the Worker throws **or answers with an empty
     * list**.
     *
     * The empty case is the one that actually bit us: a deprecated IGDB filter made the Worker
     * return `{"results":[]}` with a `200 OK`, which `runCatching` treats as a perfectly good
     * answer — so DISCOVER and search showed zero games on a real device while 426 seeded
     * titles sat unused in Room. "Offline-first" has to mean "empty-first" too, because a
     * healthy-looking backend serving nothing is indistinguishable from an outage to the user.
     */
    private suspend fun fromWorkerOrSeed(
        fromWorker: suspend () -> List<GameDto>,
        fromSeed: suspend () -> List<GameDto>,
    ): List<GameDto> {
        val fromNetwork = try {
            fromWorker()
        } catch (cancellation: CancellationException) {
            throw cancellation // never swallow cancellation — it breaks structured concurrency
        } catch (error: Exception) {
            emptyList()
        }
        return fromNetwork.ifEmpty { fromSeed() }
    }

    /**
     * Seed ids are negative by construction (see `tools/generate_seed.mjs`), so they can never
     * be resolved against IGDB — skipping them avoids a guaranteed 404 round trip.
     */
    override suspend fun detail(id: Long): GameDto? {
        if (id <= 0) return null
        return try {
            api.gameDetail(id)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            null
        }
    }

    /**
     * Never falls back to the seed set. The caller ([com.mikhilnaika.continueapp.core.data.GameCacheRepository])
     * refreshes rows it already holds, so answering from local data would be a no-op dressed up
     * as a success — and would stamp `cachedAt` forward, so the row would look fresh and never
     * be retried. Empty means "ask again later".
     */
    override suspend fun byIds(ids: List<Long>): List<GameDto> {
        // Seed ids are negative by construction, so they can never resolve against IGDB.
        val real = ids.filter { it > 0 }
        if (real.isEmpty()) return emptyList()
        return try {
            api.gamesBatch(real.joinToString(",")).results
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            emptyList()
        }
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
