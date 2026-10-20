package com.afora.android.ui.leave

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Date helpers for leave screens.
 *
 * Uses java.text/java.util only: minSdk is 24 and java.time needs API 26 or
 * core-library desugaring. Dates are ISO `yyyy-MM-dd` strings on the wire and
 * are always handled in UTC so a picker's UTC-midnight millis never shifts a day.
 */
object LeaveDates {

    private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

    /** `yyyy-MM-dd` for a date-picker value (UTC midnight millis). */
    fun isoFromMillis(millis: Long): String = isoFormat().format(Date(millis))

    /** UTC-midnight millis for an ISO date, or null if it is not a valid date. */
    fun millisFromIso(iso: String): Long? = try {
        isoFormat().parse(iso)?.time
    } catch (e: java.text.ParseException) {
        null
    }

    /** Whole days from [fromIso] to [toIso] counting both ends; null if either is invalid or reversed. */
    fun inclusiveDays(fromIso: String, toIso: String): Int? {
        val from = millisFromIso(fromIso) ?: return null
        val to = millisFromIso(toIso) ?: return null
        if (to < from) return null
        return (TimeUnit.MILLISECONDS.toDays(to - from) + 1).toInt()
    }

    /** `Mar 17, 2025` in the user's locale; falls back to the raw string if unparseable. */
    fun display(iso: String): String {
        val millis = millisFromIso(iso) ?: return iso
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))
    }

    /** `Mar 17, 2025` for one day, `Mar 17 - Mar 18, 2025` when the range is within one year. */
    fun displayRange(fromIso: String, toIso: String): String =
        if (fromIso == toIso) display(fromIso) else "${display(fromIso)} - ${display(toIso)}"

    /** `2 Academic Days` / `1 Academic Day`, or an empty string when the range is invalid. */
    fun daysLabel(fromIso: String, toIso: String): String {
        val days = inclusiveDays(fromIso, toIso) ?: return ""
        return "$days Academic ${if (days == 1) "Day" else "Days"}"
    }

    /**
     * `Sep 15, 10:00` from a server LocalDateTime string such as
     * `2026-09-15T10:00:00`. Shown as an absolute time rather than "2h ago":
     * the server stores timestamps without a zone, so a relative label would
     * be wrong whenever the device and server zones differ.
     */
    fun submittedLabel(createdAtIso: String): String {
        if (createdAtIso.length < 16) return createdAtIso
        val datePart = createdAtIso.substring(0, 10)
        val timePart = createdAtIso.substring(11, 16)
        return "${display(datePart).substringBeforeLast(",")}, $timePart"
    }
}