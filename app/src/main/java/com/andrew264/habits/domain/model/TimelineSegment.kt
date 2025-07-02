package com.andrew264.habits.domain.model

import com.andrew264.habits.model.UserPresenceState

data class TimelineSegment(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val state: UserPresenceState,
    val durationMillis: Long
)