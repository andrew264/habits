package com.andrew264.habits.model.schedule

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleGroup(
    val id: String,
    val name: String,
    val days: Set<DayOfWeek>,
    val timeRanges: List<TimeRange>
)