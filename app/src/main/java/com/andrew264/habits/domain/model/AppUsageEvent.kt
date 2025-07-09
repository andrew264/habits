package com.andrew264.habits.domain.model

data class AppUsageEvent(
    val packageName: String,
    val startTimestamp: Long,
    val endTimestamp: Long?
)