package com.andrew264.habits.domain.repository

import com.andrew264.habits.domain.model.ActiveSessionState
import com.andrew264.habits.domain.model.AppUsageEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppUsageRepository {
    val activeSessionFlow: StateFlow<ActiveSessionState?>

    suspend fun startUsageSession(
        packageName: String,
        timestamp: Long
    )

    suspend fun endCurrentUsageSession(timestamp: Long)
    fun getUsageEventsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<AppUsageEvent>>
}