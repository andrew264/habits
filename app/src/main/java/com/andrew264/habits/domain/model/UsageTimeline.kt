package com.andrew264.habits.domain.model

data class UsageTimelineModel(
    val screenOnPeriods: List<ScreenOnPeriod>,
    val viewStart: Long,
    val viewEnd: Long,
    val pickupCount: Int,
    val totalScreenOnTime: Long
)

data class ScreenOnPeriod(
    val startTimestamp: Long,
    val endTimestamp: Long,
    val appSegments: List<AppSegment>
)

data class AppSegment(
    val packageName: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val color: String? // Nullable for non-whitelisted apps
)