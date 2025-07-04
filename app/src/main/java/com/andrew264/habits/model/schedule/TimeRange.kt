package com.andrew264.habits.model.schedule

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TimeRange(
    val id: String = UUID.randomUUID().toString(),
    val fromMinuteOfDay: Int,
    val toMinuteOfDay: Int
)