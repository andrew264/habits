package com.andrew264.habits.domain.repository

import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.model.UserPresenceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserPresenceHistoryRepository {
    val userPresenceState: StateFlow<UserPresenceState>

    fun updateUserPresenceState(newState: UserPresenceState)
    fun getPresenceHistoryFlow(startTime: Long): Flow<List<UserPresenceEvent>>
    fun getPresenceHistoryInRangeFlow(
        startTime: Long,
        endTime: Long
    ): Flow<List<UserPresenceEvent>>

    fun getAllPresenceHistoryFlow(): Flow<List<UserPresenceEvent>>
    suspend fun getLatestEventBefore(timestamp: Long): UserPresenceEvent?
}