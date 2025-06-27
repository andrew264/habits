package com.andrew264.habits.model.schedule

import kotlinx.serialization.Serializable

@Serializable
data class TimeRange(
    val fromMinuteOfDay: Int,
    val toMinuteOfDay: Int
)