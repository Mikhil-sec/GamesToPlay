package com.mikhilnaika.continueapp.core.network

import com.mikhilnaika.continueapp.core.network.dto.GameDto
import com.mikhilnaika.continueapp.core.network.dto.ResolveResponse

/**
 * Abstraction over "where game data comes from" — docs/08-GAME-DATA.md's structural lesson
 * from losing RAWG mid-planning. Today the only implementation is [WorkerGameDataSource],
 * which itself falls back to the offline seed set (via Room) when the Worker is unreachable.
 * If the provider behind the Worker changes again, only the Worker needs to change — this
 * interface and everything above it stays the same.
 */
interface GameDataSource {
    suspend fun search(query: String): List<GameDto>

    // `page` is what DISCOVER's SHOW MORE spends. 0 is the first 20 and behaves exactly as
    // before; the Worker clamps how deep a client may go.
    suspend fun trending(page: Int = 0): List<GameDto>
    suspend fun shortAndSweet(page: Int = 0): List<GameDto>
    suspend fun newReleases(page: Int = 0): List<GameDto>
    suspend fun hiddenGems(page: Int = 0): List<GameDto>

    /** [genreName] must be IGDB's own genre name, e.g. "Role-playing (RPG)". */
    suspend fun byGenre(genreName: String, page: Int = 0): List<GameDto>
    suspend fun resolve(text: String?, subject: String?): ResolveResponse

    /** One game by IGDB id. Null when unreachable or unknown — never throws. */
    suspend fun detail(id: Long): GameDto?

    /**
     * Several games by IGDB id in one round trip. Unknown ids are simply absent; an empty list
     * means "couldn't reach the Worker", which callers must treat as "try again later" rather
     * than "these games don't exist".
     */
    suspend fun byIds(ids: List<Long>): List<GameDto>
}
