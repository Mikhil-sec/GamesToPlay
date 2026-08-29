package com.mikhilnaika.continueapp.feature.pile

import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.util.GameFacet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PileFilteringTest {

    private fun entry(
        id: Long,
        name: String,
        state: PileState = PileState.BACKLOG,
        genres: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        platforms: List<String> = emptyList(),
        hours: Int? = null,
        rating: Float? = null,
        released: String? = null,
        addedAt: Long = id,
    ) = PileEntryWithGame(
        entryId = id,
        gameId = id,
        state = state,
        addedAt = addedAt,
        startedAt = null,
        finishedAt = null,
        ownedPlatform = null,
        hoursPlayed = null,
        name = name,
        coverUrl = null,
        playtimeHoursHastily = null,
        playtimeHoursNormally = hours,
        playtimeHoursCompletely = null,
        genresJson = genres.joinToString(prefix = "[", postfix = "]") { "\"$it\"" },
        tagsJson = tags.joinToString(prefix = "[", postfix = "]") { "\"$it\"" },
        platformsJson = platforms.joinToString(prefix = "[", postfix = "]") { "\"$it\"" },
        rating = rating,
        released = released,
    )

    private val horrorShort = entry(
        1, "Blasphemous",
        genres = listOf("Platform"), tags = listOf("Horror"),
        platforms = listOf("PC"), hours = 12, rating = 84f, released = "2019-09-10",
    )
    private val coopLong = entry(
        2, "It Takes Two",
        genres = listOf("Adventure"), tags = listOf("Co-operative"),
        platforms = listOf("PlayStation 5"), hours = 60, rating = 88f, released = "2021-03-26",
    )
    private val unknownLength = entry(3, "Minecraft", genres = listOf("Simulator"), platforms = listOf("PC"))
    private val all = listOf(horrorShort, coopLong, unknownLength)

    @Test
    fun `no filters keeps everything`() {
        assertEquals(3, PileFiltering.apply(all, PileFilters()).size)
    }

    @Test
    fun `two facets are OR, not AND`() {
        // The whole reason single-select filters were wrong: adding a second chip has to widen
        // the list, never empty it.
        val both = PileFilters(facets = setOf(GameFacet.HORROR, GameFacet.CO_OP))
        assertEquals(2, PileFiltering.apply(all, both).size)
    }

    @Test
    fun `a facet and a platform are AND`() {
        val filters = PileFilters(facets = setOf(GameFacet.HORROR), platforms = setOf("PlayStation 5"))
        assertTrue(PileFiltering.apply(all, filters).isEmpty())
    }

    @Test
    fun `a game with no believable length is excluded by a length filter, not let through`() {
        assertFalse(PileFiltering.fitsLength(unknownLength, LengthBucket.UNDER_5))
        assertFalse(PileFiltering.fitsLength(unknownLength, LengthBucket.OVER_40))
    }

    @Test
    fun `sorting by rating puts unrated games last rather than first`() {
        val sorted = PileFiltering.sort(all, PileSort.RATING)
        assertEquals("It Takes Two", sorted[0].name)
        assertEquals("Minecraft", sorted[2].name)
    }

    @Test
    fun `sorting by release date puts undated games last`() {
        val sorted = PileFiltering.sort(all, PileSort.RELEASE_DATE)
        assertEquals("It Takes Two", sorted[0].name)
        assertEquals("Minecraft", sorted[2].name)
    }

    @Test
    fun `chip set comes from the whole pile so it does not change with the tab`() {
        val cleared = entry(4, "Portal 2", state = PileState.COMPLETED, tags = listOf("Multiplayer"))
        val everything = all + cleared
        val backlogTab = everything.filter { it.state == PileState.BACKLOG }
        val clearedTab = everything.filter { it.state == PileState.COMPLETED }

        val onBacklog = PileFiltering.facetOptions(everything, backlogTab, emptySet()).map { it.value }
        val onCleared = PileFiltering.facetOptions(everything, clearedTab, emptySet()).map { it.value }
        assertEquals(onBacklog.toSet(), onCleared.toSet())
    }

    @Test
    fun `counts describe the tab you are looking at`() {
        val cleared = entry(4, "Portal 2", state = PileState.COMPLETED, tags = listOf("Multiplayer"))
        val everything = all + cleared
        val clearedTab = everything.filter { it.state == PileState.COMPLETED }
        val options = PileFiltering.facetOptions(everything, clearedTab, emptySet())

        assertEquals(1, options.first { it.value == GameFacet.MULTIPLAYER }.countInTab)
        // Present in the pile, absent from this tab — shown as zero rather than disappearing,
        // which is what made a leftover filter look like an empty pile.
        assertEquals(0, options.first { it.value == GameFacet.HORROR }.countInTab)
    }

    @Test
    fun `a selected chip is always offered even when nothing matches it`() {
        // Otherwise the only control that could switch the filter off vanishes with the games.
        val options = PileFiltering.facetOptions(emptyList(), emptyList(), setOf(GameFacet.HORROR))
        assertEquals(listOf(GameFacet.HORROR), options.map { it.value })
        assertTrue(options.single().isEmptyHere)
    }
}
