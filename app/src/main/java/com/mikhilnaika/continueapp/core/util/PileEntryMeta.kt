package com.mikhilnaika.continueapp.core.util

import com.mikhilnaika.continueapp.core.data.dao.DrawCandidateRow
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * The decoded view of a pile row's IGDB metadata.
 *
 * `genresJson` / `tagsJson` / `platformsJson` are JSON string arrays in Room, and three
 * different files were each doing their own `runCatching { decodeFromString<List<String>>(…) }`
 * with three slightly different fallbacks. Now that PILE, DRAW and STATS all have to agree on
 * what a game's facets are — the whole point of [GameTaxonomy] — the decode belongs in one
 * place, next to the taxonomy it feeds.
 *
 * Parsed through `JsonArray` rather than `decodeFromString<List<String>>` so a row holding
 * something unexpected degrades to an empty list instead of throwing on a background thread.
 */
private val json = Json { ignoreUnknownKeys = true }

fun decodeStringList(raw: String): List<String> = runCatching {
    (json.parseToJsonElement(raw) as? JsonArray)
        ?.map { element -> element.jsonPrimitive.content }
        ?: emptyList()
}.getOrDefault(emptyList())

val PileEntryWithGame.genres: List<String> get() = decodeStringList(genresJson)

/** IGDB themes plus game modes — see [com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame.tagsJson]. */
val PileEntryWithGame.tags: List<String> get() = decodeStringList(tagsJson)

val PileEntryWithGame.platforms: List<String> get() = decodeStringList(platformsJson)

/** Every [GameFacet] this game matches — the vocabulary PILE, DRAW and STATS share. */
val PileEntryWithGame.facets: Set<GameFacet> get() = GameTaxonomy.facetsFor(genres, tags)

/** `2020` from `"2020-07-17"`, or null when IGDB has no date for the game. */
val PileEntryWithGame.releaseYear: Int? get() = released?.take(4)?.toIntOrNull()

// --- The same three accessors for a DRAW candidate ---
//
// `DrawCandidateRow` is a different projection of the same two tables, so it needs the same
// decoding. Kept here beside the pile version rather than in feature/draw so the two can never
// disagree about what a game's facets are — which is exactly the inconsistency between PILE's
// filters and DRAW's dials that closed testing reported.

val DrawCandidateRow.genres: List<String> get() = decodeStringList(genresJson)

val DrawCandidateRow.tags: List<String> get() = decodeStringList(tagsJson)

val DrawCandidateRow.platforms: List<String> get() = decodeStringList(platformsJson)

val DrawCandidateRow.facets: Set<GameFacet> get() = GameTaxonomy.facetsFor(genres, tags)

val DrawCandidateRow.releaseYear: Int? get() = released?.take(4)?.toIntOrNull()
