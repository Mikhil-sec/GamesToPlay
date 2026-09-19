package com.mikhilnaika.continueapp.core.audio

import androidx.annotation.RawRes
import com.mikhilnaika.continueapp.R

/**
 * A soundtrack for the whole app: one track for each [MusicTrack] slot. Chosen in YOU.
 *
 * Three because taste in music is the least arguable thing about it. The first device test
 * found the arcade pack "very arcade gimmick, lots of high pitch" — right for some people, a
 * reason to switch music off for others. Rather than make the arcade pack blander for everyone,
 * the app offers two other moods and lets people pick.
 *
 * The two extra packs cost [price] coins: a coin sink people *want*, which is what makes the
 * rewarded-ad → coin loop worth anything (docs/04-MONETIZATION.md). ARCADE is free and the
 * default, because the cabinet is the app's identity.
 */
enum class MusicPack(
    /** Persisted — never rename. */
    val id: String,
    val title: String,
    val vibe: String,
    val price: Int,
    @RawRes private val titleTrack: Int,
    @RawRes private val continueTrack: Int,
    @RawRes private val shopTrack: Int,
    @RawRes private val victoryTrack: Int,
) {
    ARCADE(
        id = "arcade",
        title = "ARCADE",
        vibe = "Chiptune straight out of the cabinet. Bright, bouncy, a little loud.",
        price = 0,
        titleTrack = R.raw.music_arcade_title,
        continueTrack = R.raw.music_arcade_continue,
        shopTrack = R.raw.music_arcade_shop,
        victoryTrack = R.raw.music_arcade_victory,
    ),
    AFTER_HOURS(
        id = "afterhours",
        title = "AFTER HOURS",
        vibe = "Lo-fi late night. Electric piano, soft pads, brushes. Calm.",
        price = 5,
        titleTrack = R.raw.music_afterhours_title,
        continueTrack = R.raw.music_afterhours_continue,
        shopTrack = R.raw.music_afterhours_shop,
        victoryTrack = R.raw.music_afterhours_victory,
    ),
    NEON_DRIVE(
        id = "neondrive",
        title = "NEON DRIVE",
        vibe = "80s synthwave for a night drive. Wide synths, big snare.",
        price = 5,
        titleTrack = R.raw.music_neondrive_title,
        continueTrack = R.raw.music_neondrive_continue,
        shopTrack = R.raw.music_neondrive_shop,
        victoryTrack = R.raw.music_neondrive_victory,
    );

    @RawRes
    fun res(track: MusicTrack): Int = when (track) {
        MusicTrack.TITLE -> titleTrack
        MusicTrack.CONTINUE -> continueTrack
        MusicTrack.SHOP -> shopTrack
        MusicTrack.VICTORY -> victoryTrack
    }

    companion object {
        val DEFAULT = ARCADE

        /** An unknown id (a pack from a newer build, a corrupted pref) falls back to the default. */
        fun fromId(id: String?): MusicPack = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
