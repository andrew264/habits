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
    suspend fun updateSharedDailyUsageLimit(minutes: Int?)
    suspend fun updateDailyLimitSnooze(timestamp: Long?)
    suspend fun updateSessionSnoozeTimestamps(snoozes: Map<String, Long>)

    // For tracking daily limit notifications
    fun getNotifiedSharedDailyLimitDate(): Flow<String?>
    suspend fun setNotifiedSharedDailyLimitDate(date: String)

    // Water Tracking Feature
    suspend fun updateWaterTrackingEnabled(isEnabled: Boolean)
    suspend fun updateWaterDailyTarget(targetMl: Int)
    suspend fun updateWaterReminderEnabled(isEnabled: Boolean)
    suspend fun updateWaterReminderInterval(minutes: Int)
    suspend fun updateWaterReminderSnoozeTime(minutes: Int)
    suspend fun updateWaterReminderSchedule(scheduleId: String)
}