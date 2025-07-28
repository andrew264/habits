package com.andrew264.habits.ui.common.utils

import com.andrew264.habits.model.schedule.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * A centralized utility object for formatting dates, times, and durations.
 */
object FormatUtils {

    /**
     * Formats a millisecond duration into a human-readable string like "4h 32m" or "15m".
     *
     * @param millis The duration in milliseconds.
     * @return A formatted string representation of the duration.
     */
    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0m"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        if (totalMinutes < 1) return "<1m"
        if (totalMinutes < 60) return "${totalMinutes}m"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
    }

    /**
     * Formats a minute-of-day integer into a 12-hour time string like "9:30 AM" or "8 PM".
     * This version is independent of Context.
     *
     * @param minuteOfDay The time to format, as minutes from midnight (0-1439).
     * @return A formatted time string.
     */
    fun formatTimeFromMinute(minuteOfDay: Int): String {
        if (minuteOfDay < 0 || minuteOfDay >= 24 * 60) return "Invalid Time"
        val time = LocalTime.ofSecondOfDay(minuteOfDay * 60L)
        val pattern = if (time.minute == 0) "h a" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    /**
     * Formats a timestamp using a specific pattern.
     *
     * @param millis The timestamp in milliseconds.
     * @param pattern The pattern to use for formatting (e.g., "ha", "d MMM").
     * @return A formatted string.
     */
    fun formatTimestamp(millis: Long, pattern: String): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    /**
     * Gets a 3-letter, title-cased name for a DayOfWeek, e.g., "Sun".
     *
     * @param day The DayOfWeek to format.
     * @return The short name of the day.
     */
    fun formatDayOfWeekShort(day: DayOfWeek): String {
        return java.time.DayOfWeek.valueOf(day.name)
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    /**
     * Formats a timestamp into a full day name (e.g., "Monday").
     *
     * @param millis The timestamp in milliseconds.
     * @return The full name of the day.
     */
    fun formatDayFullName(millis: Long): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
    }

    /**
     * Formats a timestamp into a short, lowercase hour label for use in charts (e.g., "12a", "8p").
     *
     * @param millis The timestamp in milliseconds.
     * @return A short, formatted hour string.
     */
    fun formatChartHourLabel(millis: Long): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val hour = dateTime.hour
        return when {
            hour == 0 -> "12a"
            hour == 12 -> "12p"
            hour < 12 -> "${hour}a"
            else -> "${hour - 12}p"
        }
    }

    /**
     * Formats a timestamp into a short day-of-the-week label for use in charts (e.g., "Mon", "Tue").
     *
     * @param millis The timestamp in milliseconds.
     * @return A short, formatted day string.
     */
    fun formatChartDayLabel(millis: Long): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return dateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}