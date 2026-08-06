package com.mikhilnaika.continueapp.core.util

import com.mikhilnaika.continueapp.core.data.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** docs/08-GAME-DATA.md "Mood mapping" table. */
class MoodMapperTest {

    @Test
    fun `simulator genre maps to cozy`() {
        val moods = MoodMapper.moodsFor(listOf("Simulator"), emptyList(), 2020)
        assertTrue(moods.contains(Mood.COZY))
    }

    @Test
    fun `horror theme excludes cozy even with puzzle genre`() {
        val moods = MoodMapper.moodsFor(listOf("Puzzle"), listOf("Horror"), 2020)
        assertTrue(!moods.contains(Mood.COZY))
    }

    @Test
    fun `shooter genre maps to chaos`() {
        val moods = MoodMapper.moodsFor(listOf("Shooter"), emptyList(), 2022)
        assertEquals(setOf(Mood.CHAOS), moods)
    }

    @Test
    fun `role-playing genre maps to story`() {
        val moods = MoodMapper.moodsFor(listOf("Role-playing (RPG)"), emptyList(), 2022)
        assertTrue(moods.contains(Mood.STORY))
    }

    @Test
    fun `strategy genre maps to brain`() {
        val moods = MoodMapper.moodsFor(listOf("Strategy"), emptyList(), 2022)
        assertTrue(moods.contains(Mood.BRAIN))
    }

    @Test
    fun `pre-2010 release year maps to nostalgia`() {
        val moods = MoodMapper.moodsFor(emptyList(), emptyList(), 2005)
        assertEquals(setOf(Mood.NOSTALGIA), moods)
    }

    @Test
    fun `a game can match multiple moods at once`() {
        val moods = MoodMapper.moodsFor(listOf("Role-playing (RPG)", "Strategy"), emptyList(), 2022)
        assertTrue(moods.containsAll(setOf(Mood.STORY, Mood.BRAIN)))
    }

    @Test
    fun `no genres tags or year yields no moods`() {
        val moods = MoodMapper.moodsFor(emptyList(), emptyList(), null)
        assertEquals(emptySet<Mood>(), moods)
    }
}
