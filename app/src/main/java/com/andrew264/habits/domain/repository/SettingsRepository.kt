package com.andrew264.habits.domain.repository

import com.andrew264.habits.domain.model.PersistentSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<PersistentSettings>
    suspend fun updateSelectedScheduleId(scheduleId: String?)
    suspend fun updateBedtimeTrackingEnabled(isEnabled: Boolean)

    // Usage Tracking Feature
    suspend fun updateAppUsageTrackingEnabled(isEnabled: Boolean)
    suspend fun updateUsageLimitNotificationsEnabled(isEnabled: Boolean)
    suspend fun updateAppBlockingEnabled(isEnabled: Boolean)

    // For tracking daily limit notifications
    fun getNotifiedDailyPackages(): Flow<Set<String>>
    suspend fun addNotifiedDailyPackage(packageName: String)

    // Water Tracking Feature
    suspend fun updateWaterTrackingEnabled(isEnabled: Boolean)
    suspend fun updateWaterDailyTarget(targetMl: Int)
    suspend fun updateWaterReminderEnabled(isEnabled: Boolean)
    suspend fun updateWaterReminderInterval(minutes: Int)
    suspend fun updateWaterReminderSnoozeTime(minutes: Int)
    suspend fun updateWaterReminderSchedule(scheduleId: String)
}