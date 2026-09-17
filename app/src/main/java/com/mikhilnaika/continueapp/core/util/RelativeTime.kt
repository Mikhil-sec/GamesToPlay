package com.mikhilnaika.continueapp.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * "just now", "3 hours ago", "yesterday", "12 Sep" — how FRIENDS says when a pile was shared.
 *
 * Coarse on purpose: a friend's pile is a snapshot, and the question the label answers is "how
 * stale is this?", not "at what minute".
 */
object RelativeTime {

    fun label(thenMillis: Long, nowMillis: Long, locale: Locale = Locale.getDefault()): String {
        val elapsed = (nowMillis - thenMillis).coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
        val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
        val days = TimeUnit.MILLISECONDS.toDays(elapsed)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
            days < 2 -> "yesterday"
            days < 7 -> "$days days ago"
            else -> {
                val pattern = if (days < 300) "d MMM" else "d MMM yyyy"
                SimpleDateFormat(pattern, locale).format(Date(thenMillis))
            }
        }
    }
}
