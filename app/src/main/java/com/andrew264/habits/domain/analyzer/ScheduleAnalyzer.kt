package com.andrew264.habits.domain.analyzer

import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.common.utils.FormatUtils
import java.time.LocalDateTime

/**
 * A data class to hold the results of the schedule coverage calculation for a full week.
 * @param totalHours The total number of hours the schedule is active for in a week.
 * @param coveragePercentage The percentage of a full week that the schedule is active.
 */
data class ScheduleCoverage(
    val totalHours: Double,
    val coveragePercentage: Double
)

/**
 * A data class to hold the results of the schedule coverage calculation for a single day.
 * @param totalHours The total number of hours the schedule is active for on that day.
 * @param coveragePercentage The percentage of that day (24 hours) that the schedule is active.
 */
data class DailyCoverage(
    val totalHours: Double,
    val coveragePercentage: Double
)


// A key representing the shape of time ranges, ignoring UUIDs and original order.
typealias RangeShape = List<Pair<Int, Int>>

/**
 * Analyzes a list of schedule groups to provide summaries,
 * coverage calculation, and time checking.
 *
 * @param groups The list of schedule groups to analyze.
 */
class ScheduleAnalyzer(groups: List<ScheduleGroup>) {

    private val schedulePerDay: Map<DayOfWeek, List<TimeRange>> = preprocessGroups(groups)

    /**
     * Creates a map of each day to a merged, sorted list of its active time ranges.
     */
    private fun preprocessGroups(groups: List<ScheduleGroup>): Map<DayOfWeek, List<TimeRange>> {
        val rawSchedule = mutableMapOf<DayOfWeek, MutableList<TimeRange>>()
        for (group in groups) {
            for (day in group.days) {
                rawSchedule.getOrPut(day) { mutableListOf() }.addAll(group.timeRanges)
            }
        }

        return DayOfWeek.entries.associateWith { day ->
            rawSchedule[day]?.let { mergeTimeRanges(it) } ?: emptyList()
        }
    }

    /**
     * Merges overlapping or adjacent time ranges into the minimum number of ranges.
     * This does NOT handle overnight ranges, as merging them would be complex and ambiguous.
     * The `isTimeInSchedule` method is responsible for interpreting them correctly.
     */
    private fun mergeTimeRanges(ranges: List<TimeRange>): List<TimeRange> {
        if (ranges.size <= 1) {
            return ranges
        }

        val sortedRanges = ranges.sortedBy { it.fromMinuteOfDay }
        val merged = mutableListOf<TimeRange>()
        var currentMerge = sortedRanges.first()

        for (i in 1 until sortedRanges.size) {
            val next = sortedRanges[i]
            // We only merge non-overnight ranges
            if (currentMerge.toMinuteOfDay >= currentMerge.fromMinuteOfDay && next.fromMinuteOfDay <= currentMerge.toMinuteOfDay) { // Overlap or adjacent
                currentMerge = currentMerge.copy(
                    toMinuteOfDay = maxOf(currentMerge.toMinuteOfDay, next.toMinuteOfDay)
                )
            } else {
                merged.add(currentMerge)
                currentMerge = next
            }
        }
        merged.add(currentMerge)
        return merged
    }

    /**
     * Checks if a given time falls within the schedule.
     * @param dateTime The date and time to check.
     * @return True if the time is within a scheduled range for that day of the week.
     */
    fun isTimeInSchedule(dateTime: LocalDateTime): Boolean {
        val today = DayOfWeek.valueOf(dateTime.dayOfWeek.name)
        val yesterday = DayOfWeek.valueOf(dateTime.minusDays(1).dayOfWeek.name)
        val minuteOfDay = dateTime.hour * 60 + dateTime.minute

        // Check ranges for today
        val rangesForToday = schedulePerDay[today] ?: emptyList()
        for (range in rangesForToday) {
            if (range.toMinuteOfDay >= range.fromMinuteOfDay) {
                // Same-day range
                if (minuteOfDay >= range.fromMinuteOfDay && minuteOfDay < range.toMinuteOfDay) {
                    return true
                }
            } else {
                // Overnight range starts today, check if we are in the "today" part
                if (minuteOfDay >= range.fromMinuteOfDay) {
                    return true
                }
            }
        }

        // Check ranges for yesterday that might spill over into today
        val rangesForYesterday = schedulePerDay[yesterday] ?: emptyList()
        for (range in rangesForYesterday) {
            if (range.toMinuteOfDay < range.fromMinuteOfDay) {
                // Overnight range that started yesterday, check if we are in the "today" part
                if (minuteOfDay < range.toMinuteOfDay) {
                    return true
                }
            }
        }

        return false
    }


    /**
     * Checks if the current system time falls within the schedule.
     */
    fun isCurrentTimeInSchedule(): Boolean = isTimeInSchedule(LocalDateTime.now())

    /**
     * Calculates the total number of hours scheduled per week and the percentage of the week covered.
     * @return A [ScheduleCoverage] object with the results.
     */
    fun calculateCoverage(): ScheduleCoverage {
        val totalMinutesInWeek = 7 * 24 * 60
        val scheduledMinutes = schedulePerDay.values.sumOf { ranges ->
            ranges.sumOf {
                if (it.toMinuteOfDay >= it.fromMinuteOfDay) {
                    it.toMinuteOfDay - it.fromMinuteOfDay
                } else {
                    // Overnight range
                    (24 * 60 - it.fromMinuteOfDay) + it.toMinuteOfDay
                }
            }
        }

        val totalHours = scheduledMinutes / 60.0
        val percentage = (scheduledMinutes.toDouble() / totalMinutesInWeek) * 100.0

        return ScheduleCoverage(totalHours, percentage)
    }

    /**
     * Calculates the coverage information (total hours and percentage) for each day of the week.
     * @return A map where each [DayOfWeek] is associated with its [DailyCoverage].
     */
    fun calculateCoveragePerDay(): Map<DayOfWeek, DailyCoverage> {
        val totalMinutesInDay = 24 * 60.0

        return DayOfWeek.entries.associateWith { day ->
            val ranges = schedulePerDay[day] ?: emptyList()
            val scheduledMinutes = ranges.sumOf {
                if (it.toMinuteOfDay >= it.fromMinuteOfDay) {
                    it.toMinuteOfDay - it.fromMinuteOfDay
                } else {
                    // This is tricky for per-day. A simple approach is to assign the whole duration to the start day.
                    (24 * 60 - it.fromMinuteOfDay) + it.toMinuteOfDay
                }
            }

            if (scheduledMinutes == 0) {
                DailyCoverage(0.0, 0.0)
            } else {
                val totalHours = scheduledMinutes / 60.0
                val percentage = (scheduledMinutes / totalMinutesInDay) * 100.0
                DailyCoverage(totalHours, percentage)
            }
        }
    }

    /**
     * Creates a human-readable summary of the schedule.
     * Example: "Mon-Fri: 9:00 AM - 5:00 PM\nSat: 10:00 AM - 2:00 PM (+1d)"
     * @return A string summarizing the schedule.
     */
    fun createSummary(): String {
        val rangesToDays = mutableMapOf<RangeShape, MutableList<DayOfWeek>>()
        val canonicalRanges = mutableMapOf<RangeShape, List<TimeRange>>()

        for ((day, ranges) in schedulePerDay) {
            if (ranges.isNotEmpty()) {
                val shape: RangeShape = ranges
                    .map { it.fromMinuteOfDay to it.toMinuteOfDay }
                    .sortedBy { it.first }

                rangesToDays.getOrPut(shape) { mutableListOf() }.add(day)

                canonicalRanges.putIfAbsent(shape, ranges.sortedBy { it.fromMinuteOfDay })
            }
        }

        if (rangesToDays.isEmpty()) {
            return "No schedule set."
        }

        // Build a summary string for each group of days with a common schedule.
        return rangesToDays.entries
            .sortedBy { (_, days) -> days.minOfOrNull { it.ordinal } ?: Int.MAX_VALUE } // Sort groups by their first day.
            .joinToString("\n") { (shape, days) ->
                val daysSummary = formatDayGroup(days)
                val timeRanges = canonicalRanges[shape]!! // We know it's there.
                val timeSummary = timeRanges.joinToString(", ") { range ->
                    val overnightIndicator = if (range.toMinuteOfDay < range.fromMinuteOfDay) " (+1d)" else ""
                    "${FormatUtils.formatTimeFromMinute(range.fromMinuteOfDay)} - ${FormatUtils.formatTimeFromMinute(range.toMinuteOfDay)}$overnightIndicator"
                }
                "$daysSummary: $timeSummary"
            }
    }

    /**
     * Formats a list of days into a compact string, grouping consecutive days.
     * e.g., [MON, TUE, WED, FRI] becomes "Mon-Wed, Fri"
     */
    private fun formatDayGroup(days: List<DayOfWeek>): String {
        if (days.isEmpty()) return ""
        if (days.size == 7) return "Every day"

        val sortedDays = days.sortedBy { it.ordinal }
        val dayStrings = mutableListOf<String>()
        var i = 0
        while (i < sortedDays.size) {
            val streakStart = sortedDays[i]
            var j = i
            // Find the end of a consecutive streak of days
            while (j + 1 < sortedDays.size && sortedDays[j + 1].ordinal == sortedDays[j].ordinal + 1) {
                j++
            }
            val streakEnd = sortedDays[j]

            when {
                streakStart == streakEnd -> {
                    // Single day
                    dayStrings.add(FormatUtils.formatDayOfWeekShort(streakStart))
                }

                streakEnd.ordinal == streakStart.ordinal + 1 -> {
                    // Streak of 2, e.g., "Mon, Tue"
                    dayStrings.add(FormatUtils.formatDayOfWeekShort(streakStart))
                    dayStrings.add(FormatUtils.formatDayOfWeekShort(streakEnd))
                }

                else -> {
                    // Streak of 3+, e.g., "Mon-Fri"
                    dayStrings.add("${FormatUtils.formatDayOfWeekShort(streakStart)}-${FormatUtils.formatDayOfWeekShort(streakEnd)}")
                }
            }
            i = j + 1
        }
        return dayStrings.joinToString(", ")
    }
}