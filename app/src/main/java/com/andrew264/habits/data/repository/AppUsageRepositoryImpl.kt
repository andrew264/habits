package com.andrew264.habits.data.repository

import androidx.room.Transaction
import com.andrew264.habits.data.dao.AppUsageEventDao
import com.andrew264.habits.data.entity.AppUsageEventEntity
import com.andrew264.habits.domain.model.AppUsageEvent
import com.andrew264.habits.domain.repository.AppUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepositoryImpl @Inject constructor(
    private val appUsageEventDao: AppUsageEventDao
) : AppUsageRepository {

    @Transaction
    override suspend fun startUsageSession(
        packageName: String,
        timestamp: Long
    ) {
        endCurrentUsageSession(timestamp)
        val newEvent = AppUsageEventEntity(
            packageName = packageName,
            startTimestamp = timestamp,
            endTimestamp = null
        )
        appUsageEventDao.insert(newEvent)
    }

    override suspend fun endCurrentUsageSession(timestamp: Long) {
        val ongoingEvent = appUsageEventDao.getOngoingEvent()
        if (ongoingEvent != null) {
            // Ensure we don't create a session with end time before start time.
            if (timestamp > ongoingEvent.startTimestamp) {
                appUsageEventDao.update(ongoingEvent.copy(endTimestamp = timestamp))
            } else {
                // If the new timestamp is somehow before the start, just delete the invalid event.
                appUsageEventDao.update(ongoingEvent.copy(endTimestamp = ongoingEvent.startTimestamp))
            }
        }
    }

    override fun getUsageEventsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<AppUsageEvent>> {
        return appUsageEventDao.getEventsInRange(startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}

private fun AppUsageEventEntity.toDomainModel(): AppUsageEvent {
    return AppUsageEvent(
        packageName = this.packageName,
        startTimestamp = this.startTimestamp,
        endTimestamp = this.endTimestamp
    )
}