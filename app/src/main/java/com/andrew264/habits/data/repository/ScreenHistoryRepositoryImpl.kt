package com.andrew264.habits.data.repository

import com.andrew264.habits.data.dao.ScreenEventDao
import com.andrew264.habits.data.entity.ScreenEventEntity
import com.andrew264.habits.domain.model.ScreenEvent
import com.andrew264.habits.domain.repository.ScreenHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenHistoryRepositoryImpl @Inject constructor(
    private val screenEventDao: ScreenEventDao
) : ScreenHistoryRepository {
    override suspend fun addScreenEvent(
        eventType: String,
        timestamp: Long
    ) {
        screenEventDao.insert(
            ScreenEventEntity(
                timestamp = timestamp,
                eventType = eventType
            )
        )
    }

    override fun getScreenEventsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<ScreenEvent>> {
        return screenEventDao.getEventsInRange(startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}

private fun ScreenEventEntity.toDomainModel(): ScreenEvent {
    return ScreenEvent(
        timestamp = this.timestamp,
        eventType = this.eventType
    )
}