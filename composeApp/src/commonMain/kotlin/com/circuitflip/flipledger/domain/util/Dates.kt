package com.circuitflip.flipledger.domain.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Date helpers shared by forms and financial reporting.
 *
 * Persisted business dates use ISO-8601 (`YYYY-MM-DD`). Unlike display labels such as
 * "Jul 12", ISO dates are unambiguous, sortable, and retain the year.
 */
object Dates {
    fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun todayIso(): String = today().toString()

    fun parseIso(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.trim()) }.getOrNull()

    fun daysBetween(startIso: String, endIso: String = todayIso()): Int? {
        val start = parseIso(startIso) ?: return null
        val end = parseIso(endIso) ?: return null
        return (end.toEpochDays() - start.toEpochDays()).coerceAtLeast(0)
    }

    /** Signed day difference used for expiry reminders; negative means already expired. */
    fun daysUntil(dateIso: String, fromIso: String = todayIso()): Int? {
        val target = parseIso(dateIso) ?: return null
        val from = parseIso(fromIso) ?: return null
        return target.toEpochDays() - from.toEpochDays()
    }

    fun isInMonth(dateIso: String, year: Int, month: Int): Boolean =
        parseIso(dateIso)?.let { it.year == year && it.monthNumber == month } == true

    fun monthIndex(date: LocalDate): Int = date.year * 12 + date.monthNumber - 1

    fun monthIndexFromTimestamp(timestamp: String?): Int? =
        timestamp?.let {
            runCatching {
                val date = Instant.parse(it)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                monthIndex(date)
            }.getOrNull()
        }

    fun monthLabel(year: Int, month: Int): String {
        val names = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        )
        return "${names.getOrElse(month - 1) { "Unknown" }} $year"
    }
}
