package com.mikhilnaika.continueapp.feature.draw

import com.mikhilnaika.continueapp.core.data.Mood
import com.mikhilnaika.continueapp.core.data.TimeBudget
import com.mikhilnaika.continueapp.core.data.dao.DrawCandidateRow
import com.mikhilnaika.continueapp.core.util.MoodMapper
import com.mikhilnaika.continueapp.core.util.Playtime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

/** One dealt card plus the reasons it was picked, for the "why matched" line — docs/02-PRODUCT-SPEC.md §3. */
data class DrawPick(
    val candidate: DrawCandidateRow,
    val reasons: List<String>,
)

data class DrawResult(
    val picks: List<DrawPick>,
    /** Non-null when a filter was relaxed to reach 3 picks — shown on-screen verbatim. */
    val loosenedMessage: String? = null,
)

/**
 * Pure, unit-tested weighted-sampling engine behind the DRAW lever — docs/02-PRODUCT-SPEC.md
 * §3 "Selection algorithm". No I/O, no Android types, so it's cheap to test exhaustively.
 */
object DrawSelector {
    private const val PICK_COUNT = 3
    private const val SNOOZE_DECAY_MS = 14L * 24 * 60 * 60 * 1000 // 2-week decay
    private val json = Json { ignoreUnknownKeys = true }

    fun select(
        candidates: List<DrawCandidateRow>,
        timeBudget: TimeBudget,
        mood: Mood,
        platforms: Set<String>,
        now: Long = System.currentTimeMillis(),
        random: Random = Random.Default,
    ): DrawResult {
        if (candidates.isEmpty()) return DrawResult(emptyList())

        // Hard filters, relaxed in order (platform first, then time) until 3 qualify. Each
        // relaxation is only kept if it actually grows the pool beyond the previous stage.
        var pool = applyFilters(candidates, timeBudget, platforms, platformStrict = true, timeStrict = true)
        var loosened: String? = null
        if (pool.size < PICK_COUNT && platforms.isNotEmpty()) {
            val platformRelaxed = applyFilters(candidates, timeBudget, platforms, platformStrict = false, timeStrict = true)
            if (platformRelaxed.size > pool.size) {
                pool = platformRelaxed
                loosened = "Loosened to fit — not enough short games on your selected platform."
            }
        }
        if (pool.size < PICK_COUNT) {
            val relaxedTime = applyFilters(candidates, timeBudget, platforms, platformStrict = false, timeStrict = false)
            if (relaxedTime.size > pool.size) {
                pool = relaxedTime
                loosened = "Loosened to fit — only ${pool.size} games matched your time budget."
            }
        }
        if (pool.isEmpty()) return DrawResult(emptyList())

        val picks = weightedSampleDistinct(pool, minOf(PICK_COUNT, pool.size), mood, timeBudget, platforms, now, random)
        val drawPicks = picks.map { candidate ->
            DrawPick(candidate, reasonsFor(candidate, mood, timeBudget, platforms))
        }
        return DrawResult(drawPicks, loosened)
    }

    private fun applyFilters(
        candidates: List<DrawCandidateRow>,
        timeBudget: TimeBudget,
        platforms: Set<String>,
        platformStrict: Boolean,
        timeStrict: Boolean,
    ): List<DrawCandidateRow> = candidates.filter { c ->
        val platformOk = !platformStrict || platforms.isEmpty() || platformsOf(c).any { it in platforms }
        val timeOk = !timeStrict || fitsTimeBudget(c, timeBudget)
        platformOk && timeOk
    }

    private fun fitsTimeBudget(c: DrawCandidateRow, timeBudget: TimeBudget): Boolean {
        val hours = estimatedHours(c) ?: return true // unknown length never disqualifies
        val maxHours = when (timeBudget) {
            TimeBudget.THIRTY_MIN -> 1.0f
            TimeBudget.TWO_HOURS -> 3.0f
            TimeBudget.ALL_NIGHT -> 10.0f
            TimeBudget.A_WHOLE_WEEKEND -> Float.MAX_VALUE
        }
        return hours <= maxHours
    }

    /**
     * DRAW keeps its own preference order — `hastily` first, because "can I finish this
     * tonight?" is asking about the fastest honest route to the credits, not the leisurely one.
     * Every figure is still passed through [Playtime]'s plausibility ceiling first, so a
     * sandbox game's polluted lifetime-playtime number (Minecraft's 956h "normally") can't
     * decide a time budget. Null still means "unknown", which never disqualifies.
     */
    private fun estimatedHours(c: DrawCandidateRow): Float? =
        listOfNotNull(c.playtimeHoursHastily, c.playtimeHoursNormally, c.playtimeHoursCompletely)
            .firstOrNull { it > 0 && it <= Playtime.MAX_PLAUSIBLE_HOURS }
            ?.toFloat()

    private fun weightOf(
        c: DrawCandidateRow,
        mood: Mood,
        timeBudget: TimeBudget,
        platforms: Set<String>,
        now: Long,
    ): Double {
        var weight = 1.0
        if (mood in moodsOf(c)) weight *= 3.0
        if (c.lastDrawnAt == null) weight *= 2.0
        // In-the-pile-longest: linearly scale up to 1.5x over a 180-day horizon.
        val daysInPile = ((now - c.addedAt).coerceAtLeast(0L) / (24.0 * 60 * 60 * 1000))
        weight *= 1.0 + (0.5 * (daysInPile / 180.0).coerceAtMost(1.0))
        if ((c.rating ?: 0f) >= 75f) weight *= 1.3
        val snoozedUntil = c.snoozedUntil
        if (snoozedUntil != null && snoozedUntil > now) {
            weight *= 0.2
        } else if (snoozedUntil != null) {
            // Decays back to full weight over the 2 weeks following snoozedUntil.
            val sinceUnsnoozed = (now - snoozedUntil).coerceAtLeast(0L)
            val decayProgress = (sinceUnsnoozed.toDouble() / SNOOZE_DECAY_MS).coerceIn(0.0, 1.0)
            weight *= 0.2 + (0.8 * decayProgress)
        }
        return weight
    }

    /** Weighted sampling without replacement — cumulative-weight draw, repeated until enough distinct picks. */
    private fun weightedSampleDistinct(
        pool: List<DrawCandidateRow>,
        count: Int,
        mood: Mood,
        timeBudget: TimeBudget,
        platforms: Set<String>,
        now: Long,
        random: Random,
    ): List<DrawCandidateRow> {
        val remaining = pool.toMutableList()
        val picked = mutableListOf<DrawCandidateRow>()
        repeat(count) {
            if (remaining.isEmpty()) return@repeat
            val weights = remaining.map { weightOf(it, mood, timeBudget, platforms, now) }
            val total = weights.sum()
            if (total <= 0.0) {
                picked += remaining.removeAt(0)
                return@repeat
            }
            var roll = random.nextDouble() * total
            var index = 0
            for (i in weights.indices) {
                roll -= weights[i]
                if (roll <= 0.0) {
                    index = i
                    break
                }
                index = i
            }
            picked += remaining.removeAt(index)
        }
        return picked
    }

    private fun reasonsFor(c: DrawCandidateRow, mood: Mood, timeBudget: TimeBudget, platforms: Set<String>): List<String> {
        val reasons = mutableListOf<String>()
        val hours = estimatedHours(c)
        if (hours != null && fitsTimeBudget(c, timeBudget)) {
            reasons += when (timeBudget) {
                TimeBudget.THIRTY_MIN -> "QUICK"
                TimeBudget.TWO_HOURS -> "SHORT"
                TimeBudget.ALL_NIGHT -> "A GOOD NIGHT'S WORTH"
                TimeBudget.A_WHOLE_WEEKEND -> "GO DEEP"
            }
        }
        if (mood in moodsOf(c)) reasons += mood.name
        val matchedPlatform = platformsOf(c).firstOrNull { it in platforms }
        if (matchedPlatform != null) reasons += "ON YOUR ${matchedPlatform.uppercase()}"
        if ((c.rating ?: 0f) >= 75f) reasons += "TOP RATED"
        if (c.lastDrawnAt == null) reasons += "NEW TO YOU"
        if (reasons.isEmpty()) reasons += "IN YOUR PILE"
        return reasons
    }

    private fun moodsOf(c: DrawCandidateRow): Set<Mood> =
        MoodMapper.moodsFor(stringList(c.genresJson), stringList(c.tagsJson), releasedYear(c.released))

    private fun platformsOf(c: DrawCandidateRow): List<String> = stringList(c.platformsJson)

    private fun releasedYear(released: String?): Int? = released?.take(4)?.toIntOrNull()

    private fun stringList(jsonStr: String): List<String> = runCatching {
        json.parseToJsonElement(jsonStr).let { it as? JsonArray }
            ?.map { element -> element.jsonPrimitive.content }
            ?: emptyList()
    }.getOrDefault(emptyList())
}
