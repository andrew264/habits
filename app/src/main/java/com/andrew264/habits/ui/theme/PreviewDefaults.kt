package com.andrew264.habits.ui.theme

import com.andrew264.habits.domain.model.PersistentSettings

/**
 * Creates a default [PersistentSettings] object for use in Composable Previews.
 * This provides a single source of truth for preview data, making previews
 * easier to maintain when the data class changes.
 *
 * @return A [PersistentSettings] object with sensible defaults.
 */
fun createPreviewPersistentSettings(
    selectedScheduleId: String? = null,
    isBedtimeTrackingEnabled: Boolean = true,
    isAppUsageTrackingEnabled: Boolean = true,
    usageLimitNotificationsEnabled: Boolean = true,
    isAppBlockingEnabled: Boolean = false,
    isWaterTrackingEnabled: Boolean = true,
    waterDailyTargetMl: Int = 2500,
    isWaterReminderEnabled: Boolean = true,
    waterReminderIntervalMinutes: Int = 60,
    waterReminderSnoozeMinutes: Int = 15,
    waterReminderScheduleId: String? = null
): PersistentSettings {
    return PersistentSettings(
        selectedScheduleId = selectedScheduleId,
        isBedtimeTrackingEnabled = isBedtimeTrackingEnabled,
        isAppUsageTrackingEnabled = isAppUsageTrackingEnabled,
        usageLimitNotificationsEnabled = usageLimitNotificationsEnabled,
        isAppBlockingEnabled = isAppBlockingEnabled,
        isWaterTrackingEnabled = isWaterTrackingEnabled,
        waterDailyTargetMl = waterDailyTargetMl,
        isWaterReminderEnabled = isWaterReminderEnabled,
        waterReminderIntervalMinutes = waterReminderIntervalMinutes,
        waterReminderSnoozeMinutes = waterReminderSnoozeMinutes,
        waterReminderScheduleId = waterReminderScheduleId
    )
}