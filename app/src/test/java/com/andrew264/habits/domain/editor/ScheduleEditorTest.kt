package com.andrew264.habits.domain.editor

import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.model.schedule.TimeRange
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScheduleEditorTest {

    private lateinit var editor: ScheduleEditor

    @Before
    fun setUp() {
        editor = ScheduleEditor()
    }

    // --- Test Data ---
    private val timeRange1 = TimeRange(id = "tr1", fromMinuteOfDay = 540, toMinuteOfDay = 1020) // 9 AM - 5 PM
    private val timeRange2 = TimeRange(id = "tr2", fromMinuteOfDay = 1200, toMinuteOfDay = 1320) // 8 PM - 10 PM

    private val weekdaysGroup = ScheduleGroup(
        id = "g1",
        name = "Weekdays",
        days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
        timeRanges = listOf(timeRange1)
    )

    private val weekendGroup = ScheduleGroup(
        id = "g2",
        name = "Weekend",
        days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        timeRanges = listOf(timeRange2)
    )

    private val baseSchedule = Schedule(
        id = "s1",
        name = "Test Schedule",
        groups = listOf(weekdaysGroup, weekendGroup)
    )

    // --- Group Management Tests ---

    @Test
    fun `addGroup should add a new, empty group to the schedule`() {
        val newSchedule = editor.addGroup(baseSchedule)
        assertEquals(3, newSchedule.groups.size)
        assertTrue(newSchedule.groups.any { it.name == "New Group" })
    }

    @Test
    fun `deleteGroup should remove the specified group`() {
        val newSchedule = editor.deleteGroup(baseSchedule, "g1")
        assertEquals(1, newSchedule.groups.size)
        assertFalse(newSchedule.groups.any { it.id == "g1" })
    }

    @Test
    fun `updateGroupName should change the name of the specified group`() {
        val newSchedule = editor.updateGroupName(baseSchedule, "g1", "Work Hours")
        val updatedGroup = newSchedule.groups.find { it.id == "g1" }
        assertEquals("Work Hours", updatedGroup?.name)
    }

    @Test
    fun `toggleDayInGroup should add a day if not present`() {
        val newSchedule = editor.toggleDayInGroup(baseSchedule, "g1", DayOfWeek.THURSDAY)
        val updatedGroup = newSchedule.groups.find { it.id == "g1" }
        assertTrue(DayOfWeek.THURSDAY in updatedGroup!!.days)
    }

    @Test
    fun `toggleDayInGroup should remove a day if present`() {
        val newSchedule = editor.toggleDayInGroup(baseSchedule, "g1", DayOfWeek.MONDAY)
        val updatedGroup = newSchedule.groups.find { it.id == "g1" }
        assertFalse(DayOfWeek.MONDAY in updatedGroup!!.days)
    }

    @Test
    fun `addTimeRangeToGroup should add a new time range to the correct group`() {
        val newTimeRange = TimeRange(fromMinuteOfDay = 360, toMinuteOfDay = 480)
        val newSchedule = editor.addTimeRangeToGroup(baseSchedule, "g2", newTimeRange)
        val updatedGroup = newSchedule.groups.find { it.id == "g2" }
        assertEquals(2, updatedGroup!!.timeRanges.size)
        assertTrue(updatedGroup.timeRanges.any { it.id == newTimeRange.id })
    }

    @Test
    fun `updateTimeRangeInGroup should modify an existing time range`() {
        val updatedTimeRange = timeRange1.copy(toMinuteOfDay = 960) // 9 AM - 4 PM
        val newSchedule = editor.updateTimeRangeInGroup(baseSchedule, "g1", updatedTimeRange)
        val updatedGroup = newSchedule.groups.find { it.id == "g1" }
        assertEquals(960, updatedGroup!!.timeRanges.first().toMinuteOfDay)
    }

    @Test
    fun `deleteTimeRangeFromGroup should remove the specified time range`() {
        val newSchedule = editor.deleteTimeRangeFromGroup(baseSchedule, "g1", timeRange1)
        val updatedGroup = newSchedule.groups.find { it.id == "g1" }
        assertTrue(updatedGroup!!.timeRanges.isEmpty())
    }

    // --- Per-Day View Logic Tests ---

    @Test
    fun `addTimeRangeToDay should create a new group for a day with no existing group`() {
        val newTimeRange = TimeRange(fromMinuteOfDay = 600, toMinuteOfDay = 720)
        val newSchedule = editor.addTimeRangeToDay(baseSchedule, DayOfWeek.FRIDAY, newTimeRange)
        assertEquals(3, newSchedule.groups.size)
        val newGroup = newSchedule.groups.find { it.days == setOf(DayOfWeek.FRIDAY) }
        assertNotNull(newGroup)
        assertEquals(1, newGroup!!.timeRanges.size)
    }

    @Test
    fun `updateTimeRangeInDay in a multi-day group should ungroup the day`() {
        val updatedTimeRange = timeRange1.copy(toMinuteOfDay = 900) // 9 AM - 3 PM
        val result = editor.updateTimeRangeInDay(baseSchedule, DayOfWeek.MONDAY, updatedTimeRange)

        val newSchedule = result.schedule
        assertNotNull("A user message should be returned on ungroup", result.userMessage)
        assertEquals(3, newSchedule.groups.size)

        // Verify original group is modified
        val originalGroup = newSchedule.groups.find { it.id == "g1" }
        assertNotNull(originalGroup)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY), originalGroup!!.days)
        assertEquals(timeRange1, originalGroup.timeRanges.first()) // Unchanged

        // Verify new group is created for the modified day
        val newGroup = newSchedule.groups.find { it.days == setOf(DayOfWeek.MONDAY) }
        assertNotNull(newGroup)
        assertEquals(updatedTimeRange, newGroup!!.timeRanges.first())
    }

    @Test
    fun `updateTimeRangeInDay in a single-day group should not ungroup`() {
        val singleDaySchedule = Schedule(
            "s2", "Single Day", listOf(
                ScheduleGroup("g3", "Thursday", setOf(DayOfWeek.THURSDAY), listOf(timeRange1))
            )
        )
        val updatedTimeRange = timeRange1.copy(fromMinuteOfDay = 600)
        val result = editor.updateTimeRangeInDay(singleDaySchedule, DayOfWeek.THURSDAY, updatedTimeRange)

        val newSchedule = result.schedule
        assertNull("No user message should be returned", result.userMessage)
        assertEquals(1, newSchedule.groups.size)

        val updatedGroup = newSchedule.groups.first()
        assertEquals(600, updatedGroup.timeRanges.first().fromMinuteOfDay)
    }

    @Test
    fun `deleteTimeRangeFromDay in a multi-day group should ungroup the day`() {
        val result = editor.deleteTimeRangeFromDay(baseSchedule, DayOfWeek.TUESDAY, timeRange1)

        val newSchedule = result.schedule
        assertNotNull("A user message should be returned on ungroup", result.userMessage)
        assertEquals(3, newSchedule.groups.size)

        // Verify original group is modified
        val originalGroup = newSchedule.groups.find { it.id == "g1" }
        assertNotNull(originalGroup)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), originalGroup!!.days)
        assertEquals(1, originalGroup.timeRanges.size)

        // Verify new group is created for the day, but with the time range removed
        val newGroup = newSchedule.groups.find { it.name == "Tuesday" }
        assertNotNull(newGroup)
        assertTrue(newGroup!!.timeRanges.isEmpty())
    }

    @Test
    fun `deleteTimeRangeFromDay should remove group if it becomes empty`() {
        val singleDaySchedule = Schedule(
            "s2", "Single Day", listOf(
                ScheduleGroup("g3", "Thursday", setOf(DayOfWeek.THURSDAY), listOf(timeRange1))
            )
        )

        val result = editor.deleteTimeRangeFromDay(singleDaySchedule, DayOfWeek.THURSDAY, timeRange1)
        assertTrue("The group should be removed", result.schedule.groups.isEmpty())
    }
}