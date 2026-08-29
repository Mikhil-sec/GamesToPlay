package com.mikhilnaika.continueapp.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The conversion between Material's date picker and the timestamps this app stores.
 *
 * They are not the same kind of number, and treating them as one is an off-by-one-day bug that
 * only shows up for users in the wrong half of the world. `DatePickerState.selectedDateMillis`
 * is **UTC midnight** of the chosen calendar day; every timestamp in `pile_entries` is a
 * `System.currentTimeMillis()` instant that gets formatted in the *device's* zone. Feed one
 * straight into the other and a user in UTC-5 who picks the 3rd sees "2 Aug" written back.
 *
 * So: a picked day becomes **local noon** of that day — far enough from either midnight that no
 * zone, and no DST transition, can round it onto a neighbouring date.
 */
object CalendarDates {

    /** UTC-midnight (what the picker returns) -> local noon of the same calendar day. */
    fun pickedDayToLocalNoon(utcMidnightMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMidnightMillis
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                12,
                0,
                0,
            )
        }.timeInMillis
    }

    /** A stored local instant -> the UTC-midnight value the picker wants pre-selected. */
    fun localInstantToPickedDay(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** Local midnight tonight — the ceiling for "you can't have finished it tomorrow". */
    fun endOfToday(now: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    /** `"17 Jul 2020"` — the app's one date format, so PILE and the Credits Roll agree. */
    fun format(millis: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
}
