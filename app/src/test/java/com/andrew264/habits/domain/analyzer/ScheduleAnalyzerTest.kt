package com.andrew264.habits.domain.analyzer

import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.model.schedule.TimeRange
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ScheduleAnalyzerTest {

    // --- Test Data ---
    private val nineToFiveWeekdaysGroup = ScheduleGroup(
        id = "1",
        name = "Weekdays",
        days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        timeRanges = listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60)) // 9 AM to 5 PM
    )

    private val overnightEveryDayGroup = ScheduleGroup(
        id = "2",
        name = "Sleep",
        days = DayOfWeek.entries.toSet(),
        timeRanges = listOf(TimeRange(fromMinuteOfDay = 22 * 60, toMinuteOfDay = 6 * 60)) // 10 PM to 6 AM
    )

    private val weekendFunGroup = ScheduleGroup(
        id = "3",
        name = "Weekend",
        days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        timeRanges = listOf(
            TimeRange(fromMinuteOfDay = 10 * 60, toMinuteOfDay = 14 * 60), // 10 AM - 2 PM
            TimeRange(fromMinuteOfDay = 18 * 60, toMinuteOfDay = 23 * 60)  // 6 PM - 11 PM
        )
    )

    @Test
    fun `isTimeInSchedule should return true for time within a simple range`() {
        val analyzer = ScheduleAnalyzer(listOf(nineToFiveWeekdaysGroup))
        // Wednesday at noon
        val time = LocalDateTime.of(2024, 5, 22, 12, 0)
        assertTrue("Time during a weekday should be in schedule", analyzer.isTimeInSchedule(time))
    }

    @Test
    fun `isTimeInSchedule should return false for time outside a simple range`() {
        val analyzer = ScheduleAnalyzer(listOf(nineToFiveWeekdaysGroup))
        // Wednesday at 8 AM
        val time = LocalDateTime.of(2024, 5, 22, 8, 0)
        assertFalse("Time before schedule starts should be out of schedule", analyzer.isTimeInSchedule(time))
    }

    @Test
    fun `isTimeInSchedule should return false for time on a non-scheduled day`() {
        val analyzer = ScheduleAnalyzer(listOf(nineToFiveWeekdaysGroup))
        // Saturday at noon
        val time = LocalDateTime.of(2024, 5, 25, 12, 0)
        assertFalse("Time on a weekend should be out of a weekday schedule", analyzer.isTimeInSchedule(time))
    }

    @Test
    fun `isTimeInSchedule should return true for time within an overnight range (before midnight)`() {
        val analyzer = ScheduleAnalyzer(listOf(overnightEveryDayGroup))
        // Tuesday at 11 PM
        val time = LocalDateTime.of(2024, 5, 21, 23, 0)
        assertTrue("11 PM should be within the 10 PM - 6 AM overnight range", analyzer.isTimeInSchedule(time))
    }

    @Test
    fun `isTimeInSchedule should return true for time within an overnight range (after midnight)`() {
        val analyzer = ScheduleAnalyzer(listOf(overnightEveryDayGroup))
        // Wednesday at 4 AM (belongs to Tuesday's overnight schedule)
        val time = LocalDateTime.of(2024, 5, 22, 4, 0)
        assertTrue("4 AM should be within the 10 PM - 6 AM overnight range", analyzer.isTimeInSchedule(time))
    }

    @Test
    fun `isTimeInSchedule should return false for time outside an overnight range`() {
        val analyzer = ScheduleAnalyzer(listOf(overnightEveryDayGroup))
        // Wednesday at 10 AM
        val time = LocalDateTime.of(2024, 5, 22, 10, 0)
        assertFalse("10 AM should be outside the 10 PM - 6 AM overnight range", analyzer.isTimeInSchedule(time))
    }

    @Test
    fun `calculateCoverage should correctly sum hours for a simple schedule`() {
        val analyzer = ScheduleAnalyzer(listOf(nineToFiveWeekdaysGroup))
        val coverage = analyzer.calculateCoverage()
        // 5 days * 8 hours/day = 40 hours
        assertEquals(40.0, coverage.totalHours, 0.01)
    }

    @Test
    fun `calculateCoverage should correctly sum hours for an overnight schedule`() {
        val analyzer = ScheduleAnalyzer(listOf(overnightEveryDayGroup))
        val coverage = analyzer.calculateCoverage()
        // 7 days * 8 hours/day (10pm-12am is 2h, 12am-6am is 6h) = 56 hours
        assertEquals(56.0, coverage.totalHours, 0.01)
    }

    @Test
    fun `calculateCoverage should handle multiple groups and ranges`() {
        val analyzer = ScheduleAnalyzer(listOf(nineToFiveWeekdaysGroup, weekendFunGroup))
        val coverage = analyzer.calculateCoverage()
        // Weekdays: 5 days * 8 hours = 40 hours
        // Weekends: 2 days * ((2pm-10am=4h) + (11pm-6pm=5h)) = 2 * 9 = 18 hours
        // Total = 40 + 18 = 58 hours
        assertEquals(58.0, coverage.totalHours, 0.01)
    }

    @Test
    fun `createSummary should format a simple schedule correctly`() {
        val analyzer = ScheduleAnalyzer(listOf(nineToFiveWeekdaysGroup))
        val expected = "Mon-Fri: 9 AM - 5 PM"
        assertEquals(expected, analyzer.createSummary())
    }

    @Test
    fun `createSummary should format an overnight schedule correctly with day wrap indicator`() {
        val analyzer = ScheduleAnalyzer(listOf(overnightEveryDayGroup))
        val expected = "Every day: 10 PM - 6 AM (+1d)"
        assertEquals(expected, analyzer.createSummary())
    }

    @Test
    fun `createSummary should group days with same schedules`() {
        val complexSchedule = listOf(
            ScheduleGroup("1", "M-W", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY), listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60))),
            ScheduleGroup("2", "F", setOf(DayOfWeek.FRIDAY), listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60))),
            ScheduleGroup("3", "Sat", setOf(DayOfWeek.SATURDAY), listOf(TimeRange(fromMinuteOfDay = 10 * 60 + 30, toMinuteOfDay = 14 * 60)))
        )
        val analyzer = ScheduleAnalyzer(complexSchedule)
        val expected = "Mon-Wed, Fri: 9 AM - 5 PM\nSat: 10:30 AM - 2 PM"
        assertEquals(expected, analyzer.createSummary())
    }

    @Test
    fun `isTimeInSchedule should handle merged time ranges correctly`() {
        val overlappingGroup = ScheduleGroup(
            id = "1",
            name = "Overlapping",
            days = setOf(DayOfWeek.MONDAY),
            timeRanges = listOf(
                TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 12 * 60), // 9 AM to 12 PM
                TimeRange(fromMinuteOfDay = 11 * 60, toMinuteOfDay = 14 * 60) // 11 AM to 2 PM
            )
        )
        val analyzer = ScheduleAnalyzer(listOf(overlappingGroup))
        // Monday at 1 PM (should be inside the merged range of 9 AM to 2 PM)
        val time = LocalDateTime.of(2024, 5, 20, 13, 0)
        assertTrue("Time should be in schedule due to merged overlapping ranges", analyzer.isTimeInSchedule(time))
    }
}