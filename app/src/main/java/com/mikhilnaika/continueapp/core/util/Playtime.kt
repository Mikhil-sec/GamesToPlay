package com.mikhilnaika.continueapp.core.util

import com.mikhilnaika.continueapp.core.data.dao.DrawCandidateRow
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame

/**
 * Turns IGDB's three `game_time_to_beats` figures into the one number the app is willing to
 * print — or into "no believable number at all".
 *
 * **Why this exists.** A closed-test tester reported Minecraft as "900 hours to complete" when
 * every other source says 50-200. It isn't a mapping bug: IGDB really does return
 * `hastily 98 / normally 956 / completely 20417` for Minecraft: Java Edition, and the app was
 * printing `normally` verbatim. The three fields are independently crowdsourced aggregates
 * (roughly HowLongToBeat's Main / Main+Extras / Completionist), and on a sandbox or
 * live-service game the later two collect *lifetime* playtime rather than time-to-an-ending.
 * Verified against the production Worker on 2026-08-23:
 *
 * | game                  | hastily | normally | completely |
 * |-----------------------|---------|----------|------------|
 * | Portal 2              |       4 |        9 |         28 |
 * | Stardew Valley        |      50 |       90 |        208 |
 * | Skyrim                |      25 |      109 |        201 |
 * | Baldur's Gate III     |     128 |      132 |       5650 |
 * | Counter-Strike        |       9 |       15 |        761 |
 * | Minecraft: Java Ed.   |      98 |      956 |      20417 |
 *
 * The pollution is per-field, not per-game — Baldur's Gate 3's `completely` is nonsense while
 * its `normally` is spot on — so the fix is a plausibility ladder over the three values rather
 * than a blanket "distrust this game". Minecraft falls through to `hastily` and prints 98 HRS,
 * which is both defensible (that *is* roughly a run to the Ender Dragon) and inside the range
 * the tester expected.
 *
 * Deliberately computed at read time from the three columns already in Room rather than
 * stamped into the schema by the Worker: it needs no migration, it fixes every row already
 * cached on every tester's device, and it keeps working with the phone offline.
 */
object Playtime {

    /**
     * Above this, a "time to beat" figure is describing a hobby rather than an ending.
     *
     * 300 sits clear above the longest believable main+extras run in the sample above
     * (Baldur's Gate 3 at 132) and well below Minecraft's 956, so it separates the two classes
     * without adjudicating anything in between.
     */
    const val MAX_PLAUSIBLE_HOURS = 300

    private fun Int?.plausible(): Boolean = this != null && this > 0 && this <= MAX_PLAUSIBLE_HOURS

    /**
     * The hours to show for a game, or null when none of IGDB's three figures is believable.
     *
     * Preference order is `normally` (IGDB's balanced estimate, and what the app has always
     * shown), then `hastily`, then `completely` — each one skipped if it fails the plausibility
     * check above.
     */
    fun estimateHours(hastily: Int?, normally: Int?, completely: Int?): Int? = when {
        normally.plausible() -> normally
        hastily.plausible() -> hastily
        completely.plausible() -> completely
        else -> null
    }

    /**
     * The metadata-line label: `"42 HRS"`, or `"ENDLESS"` when nothing believable survived.
     *
     * "ENDLESS" rather than "UNKNOWN" because that's what the data is actually telling us — a
     * game whose every figure blew past the ceiling is one people don't finish — and because a
     * backlog app admitting a game has no ending is more useful than one quietly inventing a
     * deadline for it.
     */
    fun label(hastily: Int?, normally: Int?, completely: Int?): String =
        estimateHours(hastily, normally, completely)?.let { "$it HRS" } ?: "ENDLESS"
}

/** The believable hours for a pile entry, or null. See [Playtime]. */
val PileEntryWithGame.estimatedHours: Int?
    get() = Playtime.estimateHours(playtimeHoursHastily, playtimeHoursNormally, playtimeHoursCompletely)

/** `"42 HRS"` / `"ENDLESS"` for a pile entry. See [Playtime]. */
val PileEntryWithGame.playtimeLabel: String
    get() = Playtime.label(playtimeHoursHastily, playtimeHoursNormally, playtimeHoursCompletely)

/** The believable hours for a draw candidate, or null. See [Playtime]. */
val DrawCandidateRow.estimatedHours: Int?
    get() = Playtime.estimateHours(playtimeHoursHastily, playtimeHoursNormally, playtimeHoursCompletely)
