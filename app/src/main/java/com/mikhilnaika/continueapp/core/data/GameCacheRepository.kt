package com.mikhilnaika.continueapp.core.data

import com.mikhilnaika.continueapp.core.data.dao.GameDao
import com.mikhilnaika.continueapp.core.data.entity.GameEntity
import com.mikhilnaika.continueapp.core.network.GameDataSource
import com.mikhilnaika.continueapp.core.network.dto.GameDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the `games` table — pure IGDB cache, never user data — worth reading.
 *
 * **Why this had to exist.** Nothing ever re-read a game once it was in the pile, and two paths
 * wrote *deliberately incomplete* rows:
 *
 * - The **share target** wrote a stub: name, cover, and `null` for the background, the release
 *   date, all three playtimes, and empty arrays for genres, themes and platforms. That is the
 *   real answer to the closed-test report that *Ghost of Tsushima has no banner* — IGDB has
 *   full key art for it (verified against production), the app had simply never asked. The same
 *   stub is why a shared-in game showed "ENDLESS" for its length and matched no filter on any
 *   screen. Worse, the write was an unconditional REPLACE, so sharing in a game you already had
 *   *downgraded* a complete row to the stub.
 * - Every row cached before a Worker change keeps whatever the Worker used to return. Games
 *   added before 2026-08-14 have no `backgroundUrl`; games added before 2026-08-28 have no
 *   game modes in `tags`, so the new MULTIPLAYER/CO-OP facets can't see them.
 *
 * One background sweep per launch fixes both, in **one** request for up to 50 games — see
 * [GameDataSource.byIds] and the Worker's `/games/batch`, which is uncached precisely so this
 * costs no KV writes against the 1,000/day budget (docs/12-SECURITY.md).
 */
@Singleton
class GameCacheRepository @Inject constructor(
    private val gameDao: GameDao,
    private val gameDataSource: GameDataSource,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val sweptThisProcess = AtomicBoolean(false)

    /**
     * Process-scoped, so a hydration outlives the ViewModel that asked for it.
     *
     * The share sheet is the reason. It is a transparent Activity that finishes the moment the
     * game is added, taking its `viewModelScope` with it — a fetch launched there is cancelled
     * before it can land. This is a singleton for the life of the process, so there is nothing
     * to cancel and no leak to worry about.
     */
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Refreshes the stalest cached games the user actually has, at most once per process.
     *
     * Capped at [BATCH_LIMIT] — one Worker request — rather than "all of them": a big pile
     * converges over a handful of launches, and no single launch can turn into a fan-out. Runs
     * oldest-`cachedAt`-first so each launch makes progress on a different slice.
     *
     * Silent on failure by design. This is a background improvement to data the app can already
     * display; there is no user-visible operation to report an error against.
     */
    suspend fun refreshStaleGamesOnce() {
        if (!sweptThisProcess.compareAndSet(false, true)) return
        refreshStaleGames()
    }

    /** The sweep itself, callable directly when a screen wants a refresh now. */
    suspend fun refreshStaleGames(limit: Int = BATCH_LIMIT) {
        try {
            val ids = gameDao.staleReferencedGameIds(CACHE_EPOCH_MILLIS, limit)
            if (ids.isEmpty()) return
            val fresh = gameDataSource.byIds(ids)
            if (fresh.isEmpty()) return
            gameDao.upsertAll(fresh.map { it.toEntity() })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Nothing to surface: the pile still renders from what's already cached.
        }
    }

    /**
     * Caches a game the app has full data for.
     *
     * The one write path that should be used whenever a complete [GameDto] is in hand.
     */
    suspend fun cache(game: GameDto) = gameDao.upsert(game.toEntity())

    /**
     * Caches a game the app knows only the name and cover of — the share target's case.
     *
     * Two rules that the old inline write in `ShareTargetViewModel` broke:
     * 1. **Never overwrite a complete row with a stub.** An existing row is left exactly as it
     *    is; sharing in a game you already own must not cost you its genres.
     * 2. **The stub is marked as maximally stale**, so it is first in line for the next sweep
     *    rather than sitting incomplete forever.
     *
     * Deliberately does **no** network work of its own: the share sheet shows its confirmation
     * the moment this returns, and a detail fetch here would put a round trip (up to the 10s
     * read timeout) between the user's tap and the word "added". [hydrateInBackground] is the
     * non-blocking half.
     */
    suspend fun cacheMinimal(id: Long, name: String, coverUrl: String?) {
        if (gameDao.get(id) != null) return
        gameDao.upsert(
            GameEntity(
                id = id,
                slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-"),
                name = name,
                coverUrl = coverUrl,
                backgroundUrl = null,
                released = null,
                metacritic = null,
                rating = null,
                playtimeHoursHastily = null,
                playtimeHoursNormally = null,
                playtimeHoursCompletely = null,
                genresJson = json.encodeToString(emptyList<String>()),
                tagsJson = json.encodeToString(emptyList<String>()),
                platformsJson = json.encodeToString(emptyList<String>()),
                // Deliberately 0, not `now`: this row is known-incomplete, so it must read as
                // maximally stale and be first in line for the next sweep.
                cachedAt = 0L,
            )
        )
    }

    /**
     * Fills in one game's real data without anyone waiting for it.
     *
     * Fire-and-forget on [backgroundScope] on purpose — see that field for why a
     * `viewModelScope` can't do this job on the share path. Worst case it doesn't finish and
     * the next launch's [refreshStaleGamesOnce] picks the row up, because [cacheMinimal] wrote
     * it with `cachedAt = 0`.
     */
    fun hydrateInBackground(gameId: Long) {
        backgroundScope.launch {
            val fresh = try {
                gameDataSource.detail(gameId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            } ?: return@launch
            gameDao.upsert(fresh.toEntity())
        }
    }

    private fun GameDto.toEntity(): GameEntity = GameEntity(
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
        genresJson = json.encodeToString(genres),
        tagsJson = json.encodeToString(tags),
        platformsJson = json.encodeToString(platforms),
        cachedAt = System.currentTimeMillis(),
    )

    companion object {
        /**
         * Rows cached before this are refreshed on sight.
         *
         * Bump it whenever the Worker starts returning a field the app didn't have before — the
         * client-side twin of `CACHE_VERSION` in `worker/src/kv.ts`, and for the same reason:
         * a shape change that only affects *new* rows leaves every existing user permanently on
         * the old shape.
         *
         * 2026-08-28 — `tags` gained IGDB's game modes, which is what MULTIPLAYER, CO-OP and
         * SOLO in [com.mikhilnaika.continueapp.core.util.GameTaxonomy] are matched on.
         */
        const val CACHE_EPOCH_MILLIS = 1_787_875_200_000L

        /** One Worker request's worth. Matches `MAX_BATCH_IDS` in `worker/src/security.ts`. */
        const val BATCH_LIMIT = 50
    }
}
