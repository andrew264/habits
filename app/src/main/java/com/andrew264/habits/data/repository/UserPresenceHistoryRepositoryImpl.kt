package com.andrew264.habits.data.repository

import com.andrew264.habits.data.dao.UserPresenceEventDao
import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.model.UserPresenceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPresenceHistoryRepositoryImpl @Inject constructor(
    private val eventDao: UserPresenceEventDao,
    private val settingsRepository: SettingsRepository
) : UserPresenceHistoryRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _userPresenceStateFlow = MutableStateFlow(UserPresenceState.UNKNOWN)
    override val userPresenceState: StateFlow<UserPresenceState> = _userPresenceStateFlow.asStateFlow()

    override val isServiceActive: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.isServiceActive }
        .stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), false)

    override fun updateUserPresenceState(newState: UserPresenceState) {
        if (_userPresenceStateFlow.value != newState) {
            _userPresenceStateFlow.value = newState
            repositoryScope.launch {
                addPresenceEvent(newState, System.currentTimeMillis())
            }
        }
    }

    override fun updateServiceActiveState(isActive: Boolean) {
        repositoryScope.launch {
            settingsRepository.updateServiceActiveState(isActive)
        }
    }

    private suspend fun addPresenceEvent(
        state: UserPresenceState,
        timestamp: Long
    ) {
        val event = UserPresenceEvent(timestamp = timestamp, state = state.name)
        eventDao.insert(event)
    }

    override fun getPresenceHistoryFlow(startTime: Long): Flow<List<UserPresenceEvent>> {
        return eventDao.getEventsFromFlow(startTime)
    }

    override fun getPresenceHistoryInRangeFlow(
        startTime: Long,
        endTime: Long
    ): Flow<List<UserPresenceEvent>> {
        return eventDao.getEventsInRangeFlow(startTime, endTime)
    }

    override fun getAllPresenceHistoryFlow(): Flow<List<UserPresenceEvent>> {
        return eventDao.getAllEventsFlow()
    }

    override suspend fun getLatestEventBefore(timestamp: Long): UserPresenceEvent? {
        return eventDao.getLatestEventBefore(timestamp)
    }
}