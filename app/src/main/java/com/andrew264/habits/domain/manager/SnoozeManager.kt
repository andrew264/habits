package com.andrew264.habits.domain.manager

import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.scheduler.SessionAlarmScheduler
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the snooze state for app usage limits, both for individual sessions and the shared daily limit.
 */
@Singleton
class SnoozeManager @Inject constructor(
    private val sessionAlarmScheduler: SessionAlarmScheduler,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Snoozes app blocking for a specific package for a given duration.
     *
     * @param packageName The package name of the app to snooze.
     * @param durationMillis The duration of the snooze in milliseconds.
     */
    suspend fun snoozeApp(packageName: String, durationMillis: Long) {
        val snoozeEndTime = System.currentTimeMillis() + durationMillis
        val currentSnoozes = settingsRepository.settingsFlow.first().sessionSnoozeTimestamps.toMutableMap()
        currentSnoozes[packageName] = snoozeEndTime
        settingsRepository.updateSessionSnoozeTimestamps(currentSnoozes)

        // After snoozing, schedule a new alarm to check again.
        val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()
        if (durationMinutes > 0) {
            sessionAlarmScheduler.schedule(packageName, durationMinutes)
        }
    }

    /**
     * Checks if a specific app is currently snoozed for a session limit.
     *
     * @param packageName The package name of the app to check.
     * @return `true` if the app is currently snoozed, `false` otherwise.
     */
    suspend fun isAppSnoozed(packageName: String): Boolean {
        val snoozes = settingsRepository.settingsFlow.first().sessionSnoozeTimestamps
        val snoozeEndTime = snoozes[packageName] ?: return false
        return System.currentTimeMillis() < snoozeEndTime
    }

    /**
     * Clears the snooze state for a specific app's session.
     *
     * @param packageName The package name of the app to clear the snooze for.
     */
    suspend fun clearSnooze(packageName: String) {
        val currentSnoozes = settingsRepository.settingsFlow.first().sessionSnoozeTimestamps.toMutableMap()
        if (currentSnoozes.remove(packageName) != null) {
            settingsRepository.updateSessionSnoozeTimestamps(currentSnoozes)
        }
    }

    /**
     * Snoozes the shared daily usage limit for a given duration.
     * @param durationMillis The duration of the snooze in milliseconds.
     */
    suspend fun snoozeDailyLimit(durationMillis: Long) {
        val snoozeEndTime = System.currentTimeMillis() + durationMillis
        settingsRepository.updateDailyLimitSnooze(snoozeEndTime)
    }

    /**
     * Checks if the shared daily usage limit is currently snoozed.
     * @return `true` if the daily limit is snoozed, `false` otherwise.
     */
    suspend fun isDailyLimitSnoozed(): Boolean {
        val snoozeEndTime = settingsRepository.settingsFlow.first().dailyLimitSnoozeUntilTimestamp ?: return false
        val isSnoozed = System.currentTimeMillis() < snoozeEndTime
        if (!isSnoozed) {
            // Clean up expired snooze
            settingsRepository.updateDailyLimitSnooze(null)
        }
        return isSnoozed
    }
}