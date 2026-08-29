package com.mikhilnaika.continueapp.core.util

/**
 * The app's single vocabulary of "what kind of game is this" — docs/08-GAME-DATA.md.
 *
 * **Why it exists.** PILE built its filter chips straight out of IGDB's raw `genres` strings,
 * DRAW filtered on nothing but platform and its MOOD dial, and STATS didn't exist. So the two
 * screens that both claim to narrow the same pile disagreed about what a category even is, and
 * a closed tester reported exactly that: *"filters not consistent across categories and the
 * draw button"*. They also disagreed with what a person would type — IGDB has no "horror"
 * genre (it's a *theme*), no "FPS" (that's "Shooter"), and no "multiplayer" anywhere the app
 * could see it, because `game_modes` was being dropped by the Worker.
 *
 * A facet is therefore defined over **genres, themes and game modes at once**, which is the
 * only way "HORROR" and "MULTIPLAYER" can sit in the same list as "RPG". The app's `tags`
 * column carries themes + modes together (see the Worker's `toDto`), so `facetsFor` takes the
 * two lists the app already stores and needs no schema change.
 *
 * Matching is lowercase-substring, the same rule [MoodMapper] uses, so IGDB's full names
 * ("Role-playing (RPG)", "Hack and slash/Beat 'em up") and the seed set's shorter ones both
 * hit without either side needing an exact table.
 */
enum class GameFacet(val label: String) {
    ACTION("ACTION"),
    SHOOTER("SHOOTER"),
    RPG("RPG"),
    ADVENTURE("ADVENTURE"),
    HORROR("HORROR"),
    THRILLER("THRILLER"),
    PLATFORMER("PLATFORMER"),
    PUZZLE("PUZZLE"),
    STRATEGY("STRATEGY"),
    FIGHTING("FIGHTING"),
    RACING("RACING"),
    SPORTS("SPORTS"),
    SIMULATION("SIMULATION"),
    SURVIVAL("SURVIVAL"),
    STEALTH("STEALTH"),
    OPEN_WORLD("OPEN WORLD"),
    STORY_RICH("STORY"),
    MULTIPLAYER("MULTIPLAYER"),
    CO_OP("CO-OP"),
    SINGLE_PLAYER("SOLO"),
    INDIE("INDIE"),
    RETRO("RETRO"),
}

object GameTaxonomy {

    /**
     * Genre substrings per facet. Deliberately loose: IGDB writes "Role-playing (RPG)" and
     * "Real Time Strategy (RTS)", the seed set writes "RPG" and "Strategy", and a substring
     * rule covers both without a mapping table that has to be kept in step with IGDB's.
     */
    private val GENRE_RULES: Map<GameFacet, List<String>> = mapOf(
        GameFacet.SHOOTER to listOf("shooter", "fps"),
        GameFacet.RPG to listOf("role-playing", "role playing", "rpg"),
        GameFacet.ADVENTURE to listOf("adventure", "point-and-click", "point and click", "visual novel"),
        GameFacet.PLATFORMER to listOf("platform"),
        GameFacet.PUZZLE to listOf("puzzle"),
        GameFacet.STRATEGY to listOf("strategy", "tactical", "moba", "card & board"),
        GameFacet.FIGHTING to listOf("fighting"),
        GameFacet.RACING to listOf("racing"),
        GameFacet.SPORTS to listOf("sport"),
        GameFacet.SIMULATION to listOf("simulat"),
        GameFacet.INDIE to listOf("indie"),
        GameFacet.ACTION to listOf("hack and slash", "hack-and-slash", "beat 'em up", "arcade", "shooter", "fighting"),
        GameFacet.RETRO to listOf("pinball"),
    )

    /**
     * Theme/mode substrings per facet — the app's `tags` list, which is IGDB themes **plus**
     * game modes. This is where HORROR, OPEN WORLD and MULTIPLAYER live; none of them is a
     * genre in IGDB's vocabulary, which is exactly why filtering on genres alone read as
     * arbitrary to testers.
     */
    private val TAG_RULES: Map<GameFacet, List<String>> = mapOf(
        GameFacet.HORROR to listOf("horror"),
        GameFacet.THRILLER to listOf("thriller", "mystery"),
        GameFacet.ACTION to listOf("action"),
        GameFacet.SURVIVAL to listOf("survival"),
        GameFacet.STEALTH to listOf("stealth"),
        GameFacet.OPEN_WORLD to listOf("open world", "sandbox"),
        GameFacet.STORY_RICH to listOf("drama", "narrative", "story"),
        GameFacet.MULTIPLAYER to listOf("multiplayer", "battle royale", "mmo", "split screen"),
        GameFacet.CO_OP to listOf("co-operative", "cooperative", "co-op"),
        GameFacet.SINGLE_PLAYER to listOf("single player", "single-player"),
        GameFacet.RETRO to listOf("retro", "pixel", "classic"),
        GameFacet.RPG to listOf("fantasy"),
    )

    /**
     * Every facet [genres] and [tags] plausibly match. A game belongs to several — Ghost of
     * Tsushima is ACTION, RPG, ADVENTURE, STEALTH, OPEN WORLD, STORY and SOLO — which is the
     * point: facets are how a pile gets described, not a single-label classification.
     *
     * Order follows [GameFacet]'s declaration order rather than match order, so the same game
     * always produces the same chip sequence no matter what order IGDB listed its genres in.
     */
    fun facetsFor(genres: List<String>, tags: List<String>): Set<GameFacet> {
        val genresLower = genres.map { it.lowercase() }
        val tagsLower = tags.map { it.lowercase() }
        val matched = LinkedHashSet<GameFacet>()
        for (facet in GameFacet.entries) {
            val byGenre = GENRE_RULES[facet]?.let { matchesAny(genresLower, it) } == true
            val byTag = TAG_RULES[facet]?.let { matchesAny(tagsLower, it) } == true
            if (byGenre || byTag) matched += facet
        }
        return matched
    }

    private fun matchesAny(haystack: List<String>, needles: List<String>): Boolean =
        haystack.any { value -> needles.any { value.contains(it) } }
}
