package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {
    private val isoDatePattern = "yyyy-MM-dd"
    private val yearMonthPattern = "yyyy-MM"
    private val timePattern = "HH:mm"

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        return formatCalendarIso(cal)
    }

    fun currentTimeHm(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    fun currentYearMonth(): String = todayIso().substring(0, 7)

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun parseIsoToCalendar(dateIso: String): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val parts = dateIso.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m - 1)
                cal.set(Calendar.DAY_OF_MONTH, d)
            }
        }
        return cal
    }

    fun formatCalendarIso(cal: Calendar): String {
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun addDays(dateIso: String, deltaDays: Int): String {
        val cal = parseIsoToCalendar(dateIso)
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        return formatCalendarIso(cal)
    }

    fun addMonths(yearMonth: String, deltaMonths: Int): String {
        val parts = yearMonth.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: currentYear()
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, deltaMonths)
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun isoToUtcMillis(dateIso: String): Long {
        val parts = dateIso.split("-")
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        if (parts.size == 3) {
            cal.set(
                parts[0].toIntOrNull() ?: currentYear(),
                (parts[1].toIntOrNull() ?: 1) - 1,
                parts[2].toIntOrNull() ?: 1,
            )
        }
        return cal.timeInMillis
    }

    fun utcMillisToIso(millis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun formatShortDate(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
        return fmt.format(cal.time)
    }

    fun formatMediumDate(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val fmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        return fmt.format(cal.time)
    }

    fun formatCompactDate(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val fmt = SimpleDateFormat("dd/MM/yy", Locale.US)
        return fmt.format(cal.time)
    }

    fun formatDayOfWeekShort(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val fmt = SimpleDateFormat("EEE", Locale.getDefault())
        return fmt.format(cal.time)
    }

    fun dayOfMonthNumber(dateIso: String): String {
        return dateIso.takeLast(2).trimStart('0').ifEmpty { "1" }
    }

    fun formatMonthYear(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: currentYear()
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return fmt.format(cal.time)
    }

    fun formatShortMonth(monthIndex1To12: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, (monthIndex1To12 - 1).coerceIn(0, 11))
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    }

    fun formatYearMonthShort(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        return formatShortMonth(m)
    }

    fun daysInMonth(yearMonth: String): Int {
        val parts = yearMonth.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: currentYear()
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Returns 0 for Monday .. 6 for Sunday for the 1st day of the given yyyy-MM. */
    fun firstWeekdayOffsetMondayZero(yearMonth: String): Int {
        val parts = yearMonth.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: currentYear()
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    /** Returns 1 (Mon) .. 7 (Sun). */
    fun weekdayIndexMondayOne(dateIso: String): Int {
        val cal = parseIsoToCalendar(dateIso)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun parseHourMinute(timeHm: String): Pair<Int, Int> {
        val parts = timeHm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
        return h to m
    }

    fun formatHourMinute(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun recentDateStrip(centerIso: String, daysBefore: Int = 7, daysAfter: Int = 3): List<String> {
        return (-daysBefore..daysAfter).map { offset -> addDays(centerIso, offset) }
    }
}
