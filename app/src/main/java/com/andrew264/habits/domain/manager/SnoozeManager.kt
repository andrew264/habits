package com.andrew264.habits.domain.manager

import com.andrew264.habits.domain.scheduler.SessionAlarmScheduler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the snooze state for app usage limits.
 * This is an in-memory manager; snoozes do not persist across app restarts.
 * TODO; should we use the database for this? problem for later me ig
 */
@Singleton
class SnoozeManager @Inject constructor(
    private val sessionAlarmScheduler: SessionAlarmScheduler
) {
    private val snoozedApps = ConcurrentHashMap<String, Long>() // PackageName -> Snooze End Timestamp

    /**
     * Snoozes app blocking for a specific package for a given duration.
     *
     * @param packageName The package name of the app to snooze.
     * @param durationMillis The duration of the snooze in milliseconds.
     */
    fun snoozeApp(packageName: String, durationMillis: Long) {
        val snoozeEndTime = System.currentTimeMillis() + durationMillis
        snoozedApps[packageName] = snoozeEndTime

        // After snoozing, we must schedule a new alarm to check again.
        val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()
        if (durationMinutes > 0) {
            sessionAlarmScheduler.schedule(packageName, durationMinutes)
        }
    }

    /**
     * Checks if a specific app is currently snoozed.
     * This method also cleans up expired snoozes upon checking them.
     *
     * @param packageName The package name of the app to check.
     * @return `true` if the app is currently snoozed, `false` otherwise.
     */
    fun isAppSnoozed(packageName: String): Boolean {
        val snoozeEndTime = snoozedApps[packageName] ?: return false

        val isStillSnoozed = System.currentTimeMillis() < snoozeEndTime

        if (!isStillSnoozed) {
            snoozedApps.remove(packageName)
        }

        return isStillSnoozed
    }

    /**
     * Clears the snooze state for a specific app.
     * This is useful for when a usage session for an app ends.
     *
     * @param packageName The package name of the app to clear the snooze for.
     */
    fun clearSnooze(packageName: String) {
        snoozedApps.remove(packageName)
    }
}