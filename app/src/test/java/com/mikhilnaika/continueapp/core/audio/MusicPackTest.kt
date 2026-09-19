package com.mikhilnaika.continueapp.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPackTest {

    @Test
    fun `a stored id round-trips to its pack`() {
        MusicPack.entries.forEach { assertEquals(it, MusicPack.fromId(it.id)) }
    }

    @Test
    fun `nothing stored, or an id this build doesn't know, plays the default`() {
        // A fresh install, or a pack id written by a newer build then downgraded.
        assertEquals(MusicPack.ARCADE, MusicPack.fromId(null))
        assertEquals(MusicPack.ARCADE, MusicPack.fromId("vaporwave"))
    }

    @Test
    fun `the default pack is free and the others cost coins`() {
        assertEquals(0, MusicPack.DEFAULT.price)
        assertTrue(MusicPack.entries.filter { it != MusicPack.DEFAULT }.all { it.price > 0 })
    }

    @Test
    fun `pack ids are unique, because they are persisted`() {
        assertEquals(MusicPack.entries.size, MusicPack.entries.map { it.id }.toSet().size)
    }
}
