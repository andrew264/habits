package com.andrew264.habits.ui.common.utils

import com.andrew264.habits.model.counter.CounterType
import com.andrew264.habits.model.schedule.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object FormatUtils {

    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0m"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        if (totalMinutes < 1) return "<1m"
        if (totalMinutes < 60) return "${totalMinutes}m"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
    }

    fun formatCounterValue(value: Double, type: CounterType): String {
        return when (type) {
            CounterType.NUMBER -> value.roundToInt().toString()
            CounterType.DECIMAL -> {
                val rounded = (value * 10.0).roundToInt() / 10.0
                if (rounded % 1.0 == 0.0) rounded.roundToInt().toString() else rounded.toString()
            }

            CounterType.DURATION -> {
                val millis = (value * 60_000).toLong()
                formatDuration(millis)
            }
        }
    }

    fun formatTimeFromMinute(minuteOfDay: Int): String {
        if (minuteOfDay < 0 || minuteOfDay >= 24 * 60) return "Invalid Time"
        val time = LocalTime.ofSecondOfDay(minuteOfDay * 60L)
        val pattern = if (time.minute == 0) "h a" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    fun formatTimestamp(millis: Long, pattern: String): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    fun formatDayOfWeekShort(day: DayOfWeek): String {
        return java.time.DayOfWeek.valueOf(day.name)
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    fun formatDayFullName(millis: Long): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
    }

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

    fun formatChartDayLabel(millis: Long): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return dateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}