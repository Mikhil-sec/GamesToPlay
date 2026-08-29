package com.mikhilnaika.continueapp.core.util

import com.mikhilnaika.continueapp.core.network.dto.GameDto

/**
 * What the search box does to IGDB's answer before it reaches the user.
 *
 * Two closed-test reports, one cause: **IGDB's `search` is a relevance engine tuned for a
 * catalogue, not for a person typing a game they already have in mind.**
 *
 * - *"Spiderman vs spider-man"* — `search "spiderman"` returns four games headed by
 *   *Questprobe featuring Spider-Man* (1984), while `search "spider-man"` returns twenty
 *   headed by the ones anybody means. The punctuation the user didn't type is doing all the
 *   work. [rank] fixes the ordering; the *missing* results need the query itself respelled,
 *   which is `OfflineGameIndex.respell`.
 * - *"Blasphemous appears 2 times"* — IGDB does emit duplicate rows, and the list gave the user
 *   nothing to tell a real duplicate from a real coincidence because it printed only a name.
 *   [dedupe] removes the true duplicates; the release year now shown in each row (see
 *   `DiscoverScreen`) distinguishes the rest, because *Spider-Man* (2000) and *Spider-Man*
 *   (2002) are genuinely two different games and collapsing them would be the worse bug.
 *
 * Pure and free of Android types, so every rule here is exhaustively testable.
 */
object SearchRanking {

    /** `"Marvel's Spider-Man 2"` -> `"marvelsspiderman2"`. Punctuation and spacing are noise. */
    fun squash(text: String): String = buildString(text.length) {
        for (char in text) if (char.isLetterOrDigit()) append(char.lowercaseChar())
    }

    /**
     * Relevance of [name] to [query], higher is better.
     *
     * The tiers are deliberately coarse and far apart: *within* a tier the surviving order is
     * IGDB's own, which beats anything a client-side heuristic could invent. All this does is
     * stop a 1984 text adventure outranking the game whose name was typed exactly.
     */
    fun score(query: String, name: String): Int {
        val q = squash(query)
        val n = squash(name)
        if (q.isEmpty()) return 0
        return when {
            n == q -> 1000
            n.startsWith(q) -> 800
            // A word-boundary hit ("Marvel's **Spider-Man**") beats one buried mid-word.
            startsAtWordBoundary(name, q) -> 600
            n.contains(q) -> 400
            else -> 0
        }
    }

    /**
     * True when [squashedQuery] begins at some word boundary of [name].
     *
     * Words are joined as it walks, because the query is squashed and the name isn't: matching
     * `"spiderman"` against `Marvel's Spider-Man` means starting at "Spider" and pulling in
     * "Man" before there's enough text to compare at all.
     */
    private fun startsAtWordBoundary(name: String, squashedQuery: String): Boolean {
        val words = name.split(*WORD_SEPARATORS).map { squash(it) }.filter { it.isNotEmpty() }
        for (start in words.indices) {
            val joined = StringBuilder()
            var index = start
            while (index < words.size && joined.length < squashedQuery.length) {
                joined.append(words[index])
                index++
            }
            if (joined.startsWith(squashedQuery)) return true
        }
        return false
    }

    private val WORD_SEPARATORS = charArrayOf(' ', '-', ':', '\'', '.', ',', '/', '&', '!', '?')

    /**
     * Re-orders [results] by [score], breaking ties on **how much of the title the query
     * covers**.
     *
     * The tiebreak earns its place on the exact query that prompted all this: *Questprobe
     * featuring Spider-Man* and *Marvel's Spider-Man* both contain "spiderman" at a word
     * boundary, so they score identically, and IGDB happens to list the 1984 text adventure
     * first. A shorter title is one the query accounts for more of, which is the closest thing
     * to "this is the game they meant" available without asking IGDB again.
     *
     * `sortedWith` is stable, so where both keys tie — the four different games called
     * *Spider-Man* — IGDB's own order survives untouched.
     */
    fun rank(query: String, results: List<GameDto>): List<GameDto> =
        results.sortedWith(
            compareByDescending<GameDto> { score(query, it.name) }
                .thenBy { squash(it.name).length }
        )

    /**
     * Drops rows that are the same game twice.
     *
     * Two passes, because there are two kinds of duplicate and only one of them is IGDB's
     * fault:
     * 1. **The same id twice.** Rare from one query but guaranteed once two queries are merged
     *    (a respelled search runs alongside the original). This one is not merely untidy — the
     *    search list is a `LazyColumn` keyed by id, and a repeated key throws.
     * 2. **The same squashed name released the same year.** Year is load-bearing: without it
     *    this would also merge the four genuinely different games called *Spider-Man*. A row
     *    with no release date is never merged into one that has a date, for the same reason.
     *
     * The survivor of a pair is the better-rated one, falling back to whichever came first.
     */
    fun dedupe(results: List<GameDto>): List<GameDto> {
        val byId = LinkedHashMap<Long, GameDto>()
        for (game in results) if (!byId.containsKey(game.id)) byId[game.id] = game

        val winnerPerKey = HashMap<String, Long>()
        for (game in byId.values) {
            val key = mergeKey(game) ?: continue
            val incumbentId = winnerPerKey[key]
            val incumbent = incumbentId?.let { byId[it] }
            if (incumbent == null || (game.rating ?: -1f) > (incumbent.rating ?: -1f)) {
                winnerPerKey[key] = game.id
            }
        }

        val winners = winnerPerKey.values.toHashSet()
        return byId.values.filter { game -> mergeKey(game) == null || game.id in winners }
    }

    /** Null for anything that must never be merged — see [dedupe]. */
    private fun mergeKey(game: GameDto): String? {
        val year = game.released?.take(4)?.takeIf { it.isNotBlank() } ?: return null
        return "${squash(game.name)}@$year"
    }

    /** [dedupe] then [rank] — what DISCOVER applies to every search response. */
    fun clean(query: String, results: List<GameDto>): List<GameDto> = rank(query, dedupe(results))
}
