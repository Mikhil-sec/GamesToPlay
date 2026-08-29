package com.mikhilnaika.continueapp.core.util

import com.mikhilnaika.continueapp.core.network.dto.GameDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two closed-test search reports, restated as assertions.
 *
 * Every fixture below is a real row from the production Worker (2026-08-28), ids included, so
 * a regression here means the same thing the tester saw.
 */
class SearchRankingTest {

    private fun game(id: Long, name: String, released: String?, rating: Float? = null) = GameDto(
        id = id,
        slug = name.lowercase().replace(' ', '-'),
        name = name,
        released = released,
        rating = rating,
    )

    @Test
    fun `punctuation is not part of a search`() {
        assertEquals(SearchRanking.squash("Marvel's Spider-Man 2"), "marvelsspiderman2")
        assertEquals(SearchRanking.squash("spiderman"), "spiderman")
    }

    @Test
    fun `an exact title outranks a game that merely contains it`() {
        // `search "spiderman"` returned this order, headed by a 1984 text adventure.
        val results = listOf(
            game(24384, "Questprobe featuring Spider-Man", "1984-12-31"),
            game(19565, "Marvel's Spider-Man", "2018-09-07"),
            game(247535, "Spider-Man 3", "2004-12-31"),
            game(19114, "Spider-Man", "2002-04-15"),
        )
        val ranked = SearchRanking.rank("spiderman", results)
        assertEquals("Spider-Man", ranked.first().name)
        // "Questprobe featuring Spider-Man" only *contains* it — it goes last.
        assertEquals("Questprobe featuring Spider-Man", ranked.last().name)
    }

    @Test
    fun `a word-boundary hit beats one buried mid-word`() {
        // "Elden Ring" starts a word with the query; "Bring Me Home" only happens to contain
        // the letters. Note a word-boundary *prefix* still counts — "Spider-Man 2" is a hit for
        // "spiderman" — which is why this needs a name where the query starts mid-word.
        assertTrue(
            SearchRanking.score("ring", "Elden Ring") >
                SearchRanking.score("ring", "Bring Me Home")
        )
    }

    @Test
    fun `between two equally-scoring titles, the one the query covers more of wins`() {
        // Both contain "spiderman" at a word boundary, so the tier alone can't separate them —
        // and IGDB lists the 1984 text adventure first.
        val results = listOf(
            game(24384, "Questprobe featuring Spider-Man", "1984-12-31"),
            game(19565, "Marvel's Spider-Man", "2018-09-07"),
        )
        assertEquals("Marvel's Spider-Man", SearchRanking.rank("spiderman", results).first().name)
    }

    @Test
    fun `ranking is stable, so IGDB's order survives inside a tier`() {
        val results = listOf(
            game(19114, "Spider-Man", "2002-04-15"),
            game(3603, "Spider-Man", "2000-08-24"),
            game(4500, "Spider-Man", "1995-02-13"),
        )
        assertEquals(results.map { it.id }, SearchRanking.rank("spider-man", results).map { it.id })
    }

    @Test
    fun `the same id twice is collapsed — a repeated key would throw in the list`() {
        // Guaranteed once a respelled query is merged with the original.
        val results = listOf(
            game(26820, "Blasphemous", "2019-09-10"),
            game(26820, "Blasphemous", "2019-09-10"),
        )
        assertEquals(1, SearchRanking.dedupe(results).size)
    }

    @Test
    fun `same name, same year collapses to the better-rated row`() {
        val results = listOf(
            game(1, "Blasphemous", "2019-09-10", rating = 70f),
            game(2, "Blasphemous", "2019-09-10", rating = 84f),
        )
        val deduped = SearchRanking.dedupe(results)
        assertEquals(1, deduped.size)
        assertEquals(2L, deduped.first().id)
    }

    @Test
    fun `same name, different years are different games and both survive`() {
        // The bug the year check exists to prevent: IGDB carries four distinct Spider-Man games.
        val results = listOf(
            game(19114, "Spider-Man", "2002-04-15"),
            game(3603, "Spider-Man", "2000-08-24"),
            game(4500, "Spider-Man", "1995-02-13"),
        )
        assertEquals(3, SearchRanking.dedupe(results).size)
    }

    @Test
    fun `an undated row is never merged into a dated one`() {
        val results = listOf(
            game(1, "Blasphemous", "2019-09-10"),
            game(2, "Blasphemous", null),
        )
        assertEquals(2, SearchRanking.dedupe(results).size)
    }

    @Test
    fun `a sequel is not deduped against its base game`() {
        val results = listOf(
            game(26820, "Blasphemous", "2019-09-10"),
            game(165390, "Blasphemous II", "2023-08-24"),
        )
        assertEquals(2, SearchRanking.clean("blasphemous", results).size)
        assertEquals("Blasphemous", SearchRanking.clean("blasphemous", results).first().name)
    }

    @Test
    fun `an empty query changes nothing`() {
        val results = listOf(game(1, "Anything", "2020-01-01"))
        assertEquals(results, SearchRanking.clean("", results))
    }
}
