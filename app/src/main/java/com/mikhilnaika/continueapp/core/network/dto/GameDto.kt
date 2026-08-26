package com.mikhilnaika.continueapp.core.network.dto

import kotlinx.serialization.Serializable

/** Shape returned by the Worker's `games` endpoints (docs/05-TECH-ARCHITECTURE.md). */
@Serializable
data class GameDto(
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
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
)

@Serializable
data class SearchResponse(
    val results: List<GameDto>,
)

@Serializable
data class ResolveCandidateDto(
    val id: Long,
    val name: String,
    val confidence: Float,
    val coverUrl: String? = null,
)

@Serializable
data class ResolveResponse(
    val resolvedTitle: String? = null,
    val source: String,
    val candidates: List<ResolveCandidateDto> = emptyList(),
    val needsManualEntry: Boolean = false,
    /**
     * Cleanest available guess at a *game name*, for prefilling the manual-entry field. Null
     * when the Worker had nothing better than a bare link — an empty field beats one the user
     * has to clear first. Guaranteed never to contain a URL.
     */
    val suggestion: String? = null,
)

@Serializable
data class ResolveRequest(
    val text: String?,
    val subject: String? = null,
)

@Serializable
data class SteamOwnedGameDto(
    val gameId: Long?,
    val steamAppId: Long,
    val name: String,
    val playtimeHours: Float,
)

@Serializable
data class SteamOwnedResponse(
    val games: List<SteamOwnedGameDto>,
)
