package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.scheduler.WaterAlarmScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class WaterSettingsUpdate(
    val isWaterTrackingEnabled: Boolean? = null,
    val dailyTargetMl: Int? = null,
    val isReminderEnabled: Boolean? = null,
    val reminderIntervalMinutes: Int? = null,
    val snoozeMinutes: Int? = null,
    val reminderScheduleId: String? = null
)

/**
 * Handles all updates for water settings, including side effects like
 * scheduling or canceling alarms.
 */
class UpdateWaterSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val waterAlarmScheduler: WaterAlarmScheduler
) {
    suspend fun execute(update: WaterSettingsUpdate) {
        val currentSettings = settingsRepository.settingsFlow.first()

        // Apply direct settings updates first
        update.isWaterTrackingEnabled?.let { settingsRepository.updateWaterTrackingEnabled(it) }
        update.dailyTargetMl?.let { settingsRepository.updateWaterDailyTarget(it) }
        update.reminderScheduleId?.let { settingsRepository.updateWaterReminderSchedule(it) }
        update.snoozeMinutes?.let { settingsRepository.updateWaterReminderSnoozeTime(it) }
        update.reminderIntervalMinutes?.let { settingsRepository.updateWaterReminderInterval(it) }
        update.isReminderEnabled?.let { settingsRepository.updateWaterReminderEnabled(it) }

        // Determine the final state after all updates
        val newTrackingEnabled = update.isWaterTrackingEnabled ?: currentSettings.isWaterTrackingEnabled
        val newReminderEnabled = update.isReminderEnabled ?: currentSettings.isWaterReminderEnabled
        val newInterval = update.reminderIntervalMinutes ?: currentSettings.waterReminderIntervalMinutes

        // Handle side effects based on the final state
        if (!newTrackingEnabled) {
            // If the entire feature is off, cancel everything.
            waterAlarmScheduler.cancelReminders()
        } else {
            // If the feature is on, check reminder state.
            if (newReminderEnabled) {
                waterAlarmScheduler.scheduleNextReminder(newInterval.toLong())
            } else {
                waterAlarmScheduler.cancelReminders()
            }
        }
    }
}