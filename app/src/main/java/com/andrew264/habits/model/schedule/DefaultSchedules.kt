package com.andrew264.habits.model.schedule

import java.util.UUID

object DefaultSchedules {
    const val DEFAULT_SLEEP_SCHEDULE_ID = "default_sleep_schedule_id"

    val defaultSleepSchedule = Schedule(
        id = DEFAULT_SLEEP_SCHEDULE_ID,
        name = "Default Sleep (10 PM - 6 AM)",
        groups = listOf(
            ScheduleGroup(
                id = UUID.randomUUID().toString(),
                name = "All Days",
                days = DayOfWeek.entries.toSet(),
                timeRanges = listOf(
                    TimeRange(fromMinuteOfDay = 22 * 60, toMinuteOfDay = 6 * 60)
                )
            )
        )
    )
}