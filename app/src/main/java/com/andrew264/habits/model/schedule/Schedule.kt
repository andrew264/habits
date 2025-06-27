package com.andrew264.habits.model.schedule

import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val id: String,
    val name: String,
    val groups: List<ScheduleGroup>
)