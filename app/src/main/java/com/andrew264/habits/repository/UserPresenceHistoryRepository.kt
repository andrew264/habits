package com.andrew264.habits.repository

import com.andrew264.habits.data.dao.UserPresenceEventDao
import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.model.UserPresenceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPresenceHistoryRepository @Inject constructor(
    private val eventDao: UserPresenceEventDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _userPresenceStateFlow = MutableStateFlow(UserPresenceState.UNKNOWN)
    val userPresenceState: StateFlow<UserPresenceState> = _userPresenceStateFlow.asStateFlow()

    private val _isServiceActiveFlow = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActiveFlow.asStateFlow()

    fun updateUserPresenceState(newState: UserPresenceState) {
        if (_userPresenceStateFlow.value != newState) {
            _userPresenceStateFlow.value = newState
            repositoryScope.launch {
                addPresenceEvent(newState, System.currentTimeMillis())
            }
        }
    }

    fun updateServiceActiveState(isActive: Boolean) {
        _isServiceActiveFlow.value = isActive
    }

    private suspend fun addPresenceEvent(
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