package com.andrew264.habits.domain.repository

import com.andrew264.habits.domain.model.ScreenEvent
import kotlinx.coroutines.flow.Flow

interface ScreenHistoryRepository {
    suspend fun addScreenEvent(
        eventType: String,
        timestamp: Long
    )

    fun getScreenEventsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<ScreenEvent>>
}