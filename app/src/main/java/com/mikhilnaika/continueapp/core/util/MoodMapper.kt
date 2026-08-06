package com.mikhilnaika.continueapp.core.util

import com.mikhilnaika.continueapp.core.data.Mood

/**
 * Maps IGDB genres/themes (and release year for NOSTALGIA) to the DRAW machine's mood dial —
 * docs/08-GAME-DATA.md "Mood mapping" table. Kept as a single editable table per that doc's
 * instruction. `tags` holds IGDB themes (see worker/src/providers/IgdbGameProvider.kt —
 * `tags: game.themes`), not free-form tags.
 *
 * Matching is substring/case-insensitive so seed-data genre strings ("Simulator", "Puzzle")
 * and IGDB's own vocabulary both hit without needing an exact enum on either side.
 */
object MoodMapper {

    private val COZY_GENRES = listOf("simulat", "puzzle", "indie")
    private val COZY_THEMES = listOf("non-fiction", "comedy", "relaxing", "wholesome", "atmospheric")
    private val COZY_EXCLUDE_THEMES = listOf("horror")

    private val CHAOS_GENRES = listOf("shooter", "fighting", "racing", "hack and slash", "hack-and-slash")
    private val CHAOS_THEMES = listOf("multiplayer", "battle royale", "fast-paced", "fast paced")

    private val STORY_GENRES = listOf("adventure", "role-playing", "role playing", "rpg", "visual novel")
    private val STORY_THEMES = listOf("drama", "mystery", "story-rich", "story rich", "narrative")

    private val BRAIN_GENRES = listOf("strategy", "tactical", "puzzle", "point-and-click", "point and click")
    private val BRAIN_THEMES = listOf("stealth", "difficult", "tactical")

    private val NOSTALGIA_THEMES = listOf("retro", "pixel", "classic")
    private const val NOSTALGIA_YEAR_CUTOFF = 2010

    /** Returns every mood this game plausibly matches — a game can satisfy more than one. */
    fun moodsFor(genres: List<String>, tags: List<String>, releasedYear: Int?): Set<Mood> {
        val genresLower = genres.map { it.lowercase() }
        val tagsLower = tags.map { it.lowercase() }
        val moods = mutableSetOf<Mood>()

        val isHorror = tagsLower.any { tag -> COZY_EXCLUDE_THEMES.any { tag.contains(it) } }
        if (!isHorror && (matchesAny(genresLower, COZY_GENRES) || matchesAny(tagsLower, COZY_THEMES))) {
            moods += Mood.COZY
        }
        if (matchesAny(genresLower, CHAOS_GENRES) || matchesAny(tagsLower, CHAOS_THEMES)) {
            moods += Mood.CHAOS
        }
        if (matchesAny(genresLower, STORY_GENRES) || matchesAny(tagsLower, STORY_THEMES)) {
            moods += Mood.STORY
        }
        if (matchesAny(genresLower, BRAIN_GENRES) || matchesAny(tagsLower, BRAIN_THEMES)) {
            moods += Mood.BRAIN
        }
        if ((releasedYear != null && releasedYear < NOSTALGIA_YEAR_CUTOFF) || matchesAny(tagsLower, NOSTALGIA_THEMES)) {
            moods += Mood.NOSTALGIA
        }
        return moods
    }

    private fun matchesAny(haystack: List<String>, needles: List<String>): Boolean =
        haystack.any { value -> needles.any { value.contains(it) } }
}
