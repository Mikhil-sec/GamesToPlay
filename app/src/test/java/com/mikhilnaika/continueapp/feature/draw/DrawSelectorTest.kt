package com.mikhilnaika.continueapp.feature.draw

import com.mikhilnaika.continueapp.core.data.Mood
import com.mikhilnaika.continueapp.core.data.TimeBudget
import com.mikhilnaika.continueapp.core.data.dao.DrawCandidateRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** docs/02-PRODUCT-SPEC.md §3 "Selection algorithm". */
class DrawSelectorTest {

    private fun candidate(
        entryId: Long,
        name: String,
        hastily: Int? = 2,
        genres: String = "[\"Adventure\"]",
        platforms: String = "[\"Switch\"]",
        rating: Float? = null,
        addedAt: Long = 0L,
        lastDrawnAt: Long? = null,
        snoozedUntil: Long? = null,
    ) = DrawCandidateRow(
        entryId = entryId,
        gameId = entryId,
        name = name,
        coverUrl = null,
        released = "2020-01-01",
        rating = rating,
        addedAt = addedAt,
        lastDrawnAt = lastDrawnAt,
        snoozedUntil = snoozedUntil,
        playtimeHoursHastily = hastily,
        playtimeHoursNormally = hastily,
        playtimeHoursCompletely = hastily,
        genresJson = genres,
        tagsJson = "[]",
        platformsJson = platforms,
    )

    @Test
    fun `empty pile returns empty result`() {
        val result = DrawSelector.select(emptyList(), TimeBudget.TWO_HOURS, Mood.STORY, emptySet())
        assertTrue(result.picks.isEmpty())
    }

    @Test
    fun `always returns 3 distinct games when at least 3 qualify`() {
        val pool = (1..10).map { candidate(it.toLong(), "Game $it") }
        val result = DrawSelector.select(pool, TimeBudget.TWO_HOURS, Mood.STORY, emptySet(), random = Random(42))
        assertEquals(3, result.picks.size)
        assertEquals(3, result.picks.map { it.candidate.entryId }.toSet().size)
    }

    @Test
    fun `hard filter excludes games that dont fit the time budget`() {
        // Three short candidates already satisfy PICK_COUNT, so the long one is never
        // reached for relaxation — this isolates the strict-filter behavior from the
        // "loosen when fewer than 3 qualify" fallback tested separately below.
        val pool = listOf(
            candidate(1, "Short one", hastily = 1),
            candidate(2, "Short two", hastily = 1),
            candidate(3, "Short three", hastily = 1),
            candidate(4, "Long one", hastily = 40),
        )
        val result = DrawSelector.select(pool, TimeBudget.THIRTY_MIN, Mood.STORY, emptySet(), random = Random(1))
        assertTrue(result.picks.none { it.candidate.entryId == 4L })
    }

    @Test
    fun `platform filter is relaxed and reported when fewer than 3 games match`() {
        val pool = listOf(
            candidate(1, "PC game", platforms = "[\"PC\"]"),
            candidate(2, "PC game 2", platforms = "[\"PC\"]"),
            candidate(3, "PC game 3", platforms = "[\"PC\"]"),
        )
        val result = DrawSelector.select(pool, TimeBudget.TWO_HOURS, Mood.STORY, setOf("Switch"), random = Random(1))
        assertEquals(3, result.picks.size)
        assertTrue(result.loosenedMessage != null)
    }

    @Test
    fun `never crashes when fewer than 3 games exist in the whole pile`() {
        val pool = listOf(candidate(1, "Only one"))
        val result = DrawSelector.select(pool, TimeBudget.TWO_HOURS, Mood.STORY, emptySet(), random = Random(1))
        assertEquals(1, result.picks.size)
    }

    @Test
    fun `unknown playtime never disqualifies a candidate`() {
        val pool = listOf(candidate(1, "Unknown length", hastily = null))
        val result = DrawSelector.select(pool, TimeBudget.THIRTY_MIN, Mood.STORY, emptySet(), random = Random(1))
        assertEquals(1, result.picks.size)
    }

    @Test
    fun `mood match is reflected in the reasons for the pick`() {
        val pool = listOf(candidate(1, "Strategy game", genres = "[\"Strategy\"]"))
        val result = DrawSelector.select(pool, TimeBudget.TWO_HOURS, Mood.BRAIN, emptySet(), random = Random(1))
        assertTrue(result.picks.single().reasons.contains("BRAIN"))
    }

    @Test
    fun `snoozed candidate is still eligible but deprioritized`() {
        val now = 1_000_000_000L
        val pool = listOf(
            candidate(1, "Fresh", addedAt = now),
            candidate(2, "Snoozed", addedAt = now, snoozedUntil = now + 1_000),
        )
        // With only 2 candidates both still get picked (never drops below available pool size).
        val result = DrawSelector.select(pool, TimeBudget.TWO_HOURS, Mood.STORY, emptySet(), now = now, random = Random(1))
        assertEquals(2, result.picks.size)
    }
}
