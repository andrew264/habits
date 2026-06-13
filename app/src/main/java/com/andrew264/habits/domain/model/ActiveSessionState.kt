package com.andrew264.habits.domain.model

data class ActiveSessionState(
    val packageName: String,
    val startTimestamp: Long,
    val sessionLimitMinutes: Int
)
