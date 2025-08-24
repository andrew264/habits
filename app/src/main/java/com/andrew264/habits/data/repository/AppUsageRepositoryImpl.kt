package com.andrew264.habits.data.repository

import android.util.Log
import androidx.room.Transaction
import com.andrew264.habits.data.dao.AppUsageEventDao
import com.andrew264.habits.data.entity.AppUsageEventEntity
import com.andrew264.habits.domain.manager.SnoozeManager
import com.andrew264.habits.domain.model.AppUsageEvent
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.repository.WhitelistRepository
import com.andrew264.habits.domain.scheduler.SessionAlarmScheduler
import com.andrew264.habits.domain.usecase.CheckUsageLimitsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepositoryImpl @Inject constructor(
    private val appUsageEventDao: AppUsageEventDao,
    private val checkUsageLimitsUseCase: CheckUsageLimitsUseCase,
    private val whitelistRepository: WhitelistRepository,
    private val sessionAlarmScheduler: SessionAlarmScheduler,
    private val snoozeManager: SnoozeManager
) : AppUsageRepository {

    companion object {
        private const val TAG = "AppUsageRepository"
    }

    @Transaction
    override suspend fun startUsageSession(
        packageName: String,
        timestamp: Long
    ) {
        Log.d(TAG, "startUsageSession for package: $packageName")
        val lastSession = appUsageEventDao.getOngoingEvent()

        // Cancel any pending alarm from the previous app session.
        sessionAlarmScheduler.cancel()

        if (lastSession == null) {
            Log.d(TAG, "No ongoing session. Starting a new one for $packageName.")
            startNewSessionInternal(packageName, timestamp)
            return
        }

        if (lastSession.packageName == packageName) {
            Log.d(TAG, "New package is same as last. Ensuring alarm is set.")
            scheduleSessionAlarm(packageName)
            return
        }

        val endedLastSession = lastSession.copy(endTimestamp = timestamp)
        appUsageEventDao.update(endedLastSession)
        Log.d(TAG, "Ended last session for ${lastSession.packageName}.")
        snoozeManager.clearSnooze(lastSession.packageName)

        startNewSessionInternal(packageName, timestamp)
    }

    private suspend fun startNewSessionInternal(packageName: String, timestamp: Long) {
        Log.d(TAG, "Inserting new session for $packageName.")
        val newEvent = AppUsageEventEntity(packageName = packageName, startTimestamp = timestamp)
        appUsageEventDao.insert(newEvent)

        val whitelistedApps = whitelistRepository.getWhitelistedApps().first()
        if (whitelistedApps.any { it.packageName == packageName }) {
            checkUsageLimitsUseCase.checkSharedDailyLimit(packageName)
            scheduleSessionAlarm(packageName)
        }
    }

    private suspend fun scheduleSessionAlarm(packageName: String) {
        if (snoozeManager.isAppSnoozed(packageName)) {
            Log.d(TAG, "Session alarm for $packageName not scheduled as it is currently snoozed.")
            return
        }
        val app = whitelistRepository.getWhitelistedApps().first().find { it.packageName == packageName }
        app?.sessionLimitMinutes?.let { limit ->
            sessionAlarmScheduler.schedule(packageName, limit)
        }
    }

    override suspend fun endCurrentUsageSession(timestamp: Long) {
        val ongoingEvent = appUsageEventDao.getOngoingEvent()
        if (ongoingEvent != null) {
            if (timestamp > ongoingEvent.startTimestamp) {
                appUsageEventDao.update(ongoingEvent.copy(endTimestamp = timestamp))
                Log.d(TAG, "Ended ongoing session for ${ongoingEvent.packageName} at $timestamp")
            } else {
                appUsageEventDao.update(ongoingEvent.copy(endTimestamp = ongoingEvent.startTimestamp))
                Log.w(TAG, "Ended ongoing session with end time before start time. Invalidating session for ${ongoingEvent.packageName}.")
            }
            sessionAlarmScheduler.cancel()
            snoozeManager.clearSnooze(ongoingEvent.packageName)
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