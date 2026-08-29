package com.mikhilnaika.continueapp.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The facets are the vocabulary PILE, DRAW and STATS all narrow by, so what matters here is the
 * cases a genre-only classification got wrong — which is what closed testing reported.
 *
 * Inputs are the **real** strings the Worker returns, read off production on 2026-08-28.
 */
class GameTaxonomyTest {

    /** Ghost of Tsushima, verbatim from `/games/75235`. */
    private val ghostGenres = listOf("Role-playing (RPG)", "Hack and slash/Beat 'em up", "Adventure")
    private val ghostTags = listOf("Action", "Historical", "Stealth", "Drama", "Open world", "Single player")

    @Test
    fun `a game matches every facet it plausibly is, not one label`() {
        val facets = GameTaxonomy.facetsFor(ghostGenres, ghostTags)
        assertTrue(GameFacet.RPG in facets)
        assertTrue(GameFacet.ADVENTURE in facets)
        assertTrue(GameFacet.ACTION in facets)
        assertTrue(GameFacet.STEALTH in facets)
        assertTrue(GameFacet.OPEN_WORLD in facets)
        assertTrue(GameFacet.STORY_RICH in facets)
        assertTrue(GameFacet.SINGLE_PLAYER in facets)
    }

    @Test
    fun `horror is a theme, not a genre — the reason genre-only chips had no HORROR`() {
        // Blasphemous, verbatim from `/games/26820`.
        val facets = GameTaxonomy.facetsFor(
            genres = listOf("Platform", "Role-playing (RPG)", "Adventure", "Indie"),
            tags = listOf("Action", "Fantasy", "Horror", "Single player"),
        )
        assertTrue(GameFacet.HORROR in facets)
        assertTrue(GameFacet.PLATFORMER in facets)
        assertTrue(GameFacet.INDIE in facets)
    }

    @Test
    fun `multiplayer comes from game modes, which the Worker used to drop entirely`() {
        // GTA V, verbatim from `/games/batch?ids=1020` after the 2026-08-28 Worker deploy.
        val facets = GameTaxonomy.facetsFor(
            genres = listOf("Shooter", "Racing", "Adventure"),
            tags = listOf("Action", "Comedy", "Sandbox", "Open world", "Single player", "Multiplayer", "Co-operative"),
        )
        assertTrue(GameFacet.MULTIPLAYER in facets)
        assertTrue(GameFacet.CO_OP in facets)
        assertTrue(GameFacet.SHOOTER in facets)
        assertTrue(GameFacet.RACING in facets)
    }

    @Test
    fun `a game with no metadata at all matches nothing rather than everything`() {
        // The share-target stub row's shape — it must not silently land in every filter.
        assertTrue(GameTaxonomy.facetsFor(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `facet order follows the enum, not the order IGDB happened to list genres`() {
        val forwards = GameTaxonomy.facetsFor(ghostGenres, ghostTags).toList()
        val backwards = GameTaxonomy.facetsFor(ghostGenres.reversed(), ghostTags.reversed()).toList()
        assertTrue(forwards == backwards)
    }

    @Test
    fun `a single player game is not also multiplayer`() {
        val facets = GameTaxonomy.facetsFor(ghostGenres, ghostTags)
        assertFalse(GameFacet.MULTIPLAYER in facets)
        assertFalse(GameFacet.CO_OP in facets)
    }
}
