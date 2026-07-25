package com.andrew264.habits.domain.usecase

import com.andrew264.habits.data.dao.AppUsageEventDao
import com.andrew264.habits.data.dao.CounterDao
import com.andrew264.habits.data.dao.ScreenEventDao
import com.andrew264.habits.data.dao.WaterIntakeDao
import com.andrew264.habits.domain.repository.AppUsageRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class DeletableDataType {
    WATER,
    USAGE,
    COUNTERS
}

enum class TimeRangeOption(val durationMillis: Long?) {
    LAST_HOUR(TimeUnit.HOURS.toMillis(1)),
    LAST_24_HOURS(TimeUnit.DAYS.toMillis(1)),
    LAST_7_DAYS(TimeUnit.DAYS.toMillis(7)),
    LAST_4_WEEKS(TimeUnit.DAYS.toMillis(28)),
    ALL_TIME(null)
}

class DeleteDataUseCase @Inject constructor(
    private val appUsageEventDao: AppUsageEventDao,
    private val screenEventDao: ScreenEventDao,
    private val waterIntakeDao: WaterIntakeDao,
    private val counterDao: CounterDao,
    private val appUsageRepository: AppUsageRepository
) {
    suspend fun execute(
        dataTypes: Set<DeletableDataType>,
        timeRange: TimeRangeOption
    ) {
        val startTime = timeRange.durationMillis?.let { System.currentTimeMillis() - it }

        if (DeletableDataType.USAGE in dataTypes) {
            appUsageRepository.endCurrentUsageSession(System.currentTimeMillis())
            if (startTime != null) {
                appUsageEventDao.deleteEventsFrom(startTime)
                screenEventDao.deleteEventsFrom(startTime)
            } else {
                appUsageEventDao.deleteAllEvents()
                screenEventDao.deleteAllEvents()
            }
        }

        if (DeletableDataType.WATER in dataTypes) {
            if (startTime != null) {
                waterIntakeDao.deleteEntriesFrom(startTime)
            } else {
                waterIntakeDao.deleteAllEntries()
            }
        }

        if (DeletableDataType.COUNTERS in dataTypes) {
            if (startTime != null) {
                counterDao.deleteLogsFrom(startTime)
            } else {
                counterDao.deleteAllLogs()
            }
        }
    }
}