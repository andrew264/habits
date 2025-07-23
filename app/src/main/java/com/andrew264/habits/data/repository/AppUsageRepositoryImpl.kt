package com.andrew264.habits.data.repository

import android.util.Log
import androidx.room.Transaction
import com.andrew264.habits.data.dao.AppUsageEventDao
import com.andrew264.habits.data.entity.AppUsageEventEntity
import com.andrew264.habits.domain.model.AppUsageEvent
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.usecase.CheckUsageLimitsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepositoryImpl @Inject constructor(
    private val appUsageEventDao: AppUsageEventDao,
    private val checkUsageLimitsUseCase: CheckUsageLimitsUseCase
) : AppUsageRepository {

    companion object {
        private const val TAG = "AppUsageRepository"
        private const val FLICKER_THRESHOLD_MS = 60_000
    }

    @Transaction
    override suspend fun startUsageSession(
        packageName: String,
        timestamp: Long
    ) {
        Log.d(TAG, "startUsageSession for package: $packageName")
        val lastSession = appUsageEventDao.getOngoingEvent()

        if (lastSession == null) {
            Log.d(TAG, "No ongoing session. Starting a new one for $packageName.")
            val newEvent = AppUsageEventEntity(packageName = packageName, startTimestamp = timestamp)
            appUsageEventDao.insert(newEvent)
            checkUsageLimitsUseCase.checkDailyLimit(packageName)
            return
        }

        if (lastSession.packageName == packageName) {
            Log.d(TAG, "New package is same as last. No changes needed.")
            return
        }

        val endedLastSession = lastSession.copy(endTimestamp = timestamp)
        appUsageEventDao.update(endedLastSession)
        val lastSessionDuration = endedLastSession.endTimestamp!! - endedLastSession.startTimestamp
        Log.d(TAG, "Ended last session for ${lastSession.packageName}. Duration: ${lastSessionDuration}ms")

        checkUsageLimitsUseCase.checkSessionLimit(lastSession.packageName, lastSessionDuration)

        if (lastSessionDuration < FLICKER_THRESHOLD_MS) {
            Log.d(TAG, "Last session was a short flicker. Checking for merge possibility.")
            val sessionBeforeFlicker = appUsageEventDao.getSecondToLastEvent()

            if (sessionBeforeFlicker != null && sessionBeforeFlicker.packageName == packageName) {
                Log.d(TAG, "Flicker and return detected. Merging session for $packageName.")
                appUsageEventDao.delete(endedLastSession)
                appUsageEventDao.update(sessionBeforeFlicker.copy(endTimestamp = null))
                Log.d(TAG, "Merge complete. Reactivated session for $packageName.")
                return
            } else {
                Log.d(TAG, "Flicker detected, but not a return to previous app. Proceeding normally.")
                checkUsageLimitsUseCase.clearSessionNotified(lastSession.packageName)
            }
        } else {
            checkUsageLimitsUseCase.clearSessionNotified(lastSession.packageName)
        }

        Log.d(TAG, "Inserting new session for $packageName.")
        val newEvent = AppUsageEventEntity(packageName = packageName, startTimestamp = timestamp)
        appUsageEventDao.insert(newEvent)
        checkUsageLimitsUseCase.checkDailyLimit(packageName)
    }

    override suspend fun endCurrentUsageSession(timestamp: Long) {
        val ongoingEvent = appUsageEventDao.getOngoingEvent()
        if (ongoingEvent != null) {
            if (timestamp > ongoingEvent.startTimestamp) {
                appUsageEventDao.update(ongoingEvent.copy(endTimestamp = timestamp))
                Log.d(TAG, "Ended ongoing session for ${ongoingEvent.packageName} at $timestamp")
                val duration = timestamp - ongoingEvent.startTimestamp
                checkUsageLimitsUseCase.checkSessionLimit(ongoingEvent.packageName, duration)
            } else {
                appUsageEventDao.update(ongoingEvent.copy(endTimestamp = ongoingEvent.startTimestamp))
                Log.w(TAG, "Ended ongoing session with end time before start time. Invalidating session for ${ongoingEvent.packageName}.")
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