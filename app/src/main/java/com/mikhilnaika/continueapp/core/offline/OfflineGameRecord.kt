package com.mikhilnaika.continueapp.core.offline

/**
 * One row of `app/src/main/assets/game_index.tsv.gz` — see `tools/igdb_dump_index.mjs`.
 *
 * Deliberately thin. The index exists to answer "which IGDB id is this caption about",
 * not to carry full game detail — a matched game still goes through the same
 * [com.mikhilnaika.continueapp.core.network.dto.ResolveCandidateDto] shape the network path
 * produces, so the two paths stay interchangeable to every caller.
 */
data class OfflineGameRecord(
    val id: Long,
    val name: String,
    /** IGDB cover `image_id` — a CDN path segment, not a URL. Null when the game has no cover. */
    val coverImageId: String?,
    /** Disambiguates same-named entries (a remaster and its original) — higher wins ties. */
    val ratingCount: Int,
)
