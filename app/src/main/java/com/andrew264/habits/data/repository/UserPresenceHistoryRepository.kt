package com.andrew264.habits.data.repository

import com.andrew264.habits.data.dao.UserPresenceEventDao
import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.state.UserPresenceState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPresenceHistoryRepository @Inject constructor(
    private val eventDao: UserPresenceEventDao
) {
    suspend fun addPresenceEvent(
        state: UserPresenceState,
        timestamp: Long
    ) {
        val event = UserPresenceEvent(timestamp = timestamp, state = state.name)
        eventDao.insert(event)
    }

    fun getPresenceHistoryFlow(startTime: Long): Flow<List<UserPresenceEvent>> {
        return eventDao.getEventsFromFlow(startTime)
    }

    fun getPresenceHistoryInRangeFlow(
        startTime: Long,
        endTime: Long
    ): Flow<List<UserPresenceEvent>> {
        return eventDao.getEventsInRangeFlow(startTime, endTime)
    }

    fun getAllPresenceHistoryFlow(): Flow<List<UserPresenceEvent>> {
        return eventDao.getAllEventsFlow()
    }

    suspend fun getLatestEventBefore(timestamp: Long): UserPresenceEvent? {
        return eventDao.getLatestEventBefore(timestamp)
    }
}