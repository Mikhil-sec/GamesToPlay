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
    suspend fun trending(): List<GameDto>
    suspend fun shortAndSweet(): List<GameDto>
    suspend fun resolve(text: String?, subject: String?): ResolveResponse

    /** One game by IGDB id. Null when unreachable or unknown — never throws. */
    suspend fun detail(id: Long): GameDto?
}
