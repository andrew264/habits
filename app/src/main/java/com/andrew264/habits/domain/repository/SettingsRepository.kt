package com.andrew264.habits.domain.repository

import com.andrew264.habits.domain.model.PersistentSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<PersistentSettings>
    suspend fun updateServiceActiveState(isActive: Boolean)
    suspend fun updateSelectedScheduleId(scheduleId: String?)
    suspend fun updateWaterTrackingEnabled(isEnabled: Boolean)
    suspend fun updateWaterDailyTarget(targetMl: Int)
    suspend fun updateWaterReminderEnabled(isEnabled: Boolean)
    suspend fun updateWaterReminderInterval(minutes: Int)
    suspend fun updateWaterReminderSnoozeTime(minutes: Int)
    suspend fun updateWaterReminderSchedule(scheduleId: String)
}