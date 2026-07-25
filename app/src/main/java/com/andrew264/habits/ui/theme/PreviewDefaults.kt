package com.andrew264.habits.ui.theme

import com.andrew264.habits.domain.model.PersistentSettings

/**
 * Creates a default [PersistentSettings] object for use in Composable Previews.
 *
 * @return A [PersistentSettings] object with sensible defaults.
 */
fun createPreviewPersistentSettings(
    isAppUsageTrackingEnabled: Boolean = true,
    usageLimitNotificationsEnabled: Boolean = true,
    isAppBlockingEnabled: Boolean = false,
    sharedDailyUsageLimitMinutes: Int = 60,
    dailyLimitSnoozeUntilTimestamp: Long? = null,
    sessionSnoozeTimestamps: Map<String, Long> = emptyMap(),
    notifiedSharedDailyLimitDate: String? = null,
    isWaterTrackingEnabled: Boolean = true,
    waterDailyTargetMl: Int = 2500,
    isWaterReminderEnabled: Boolean = true,
    waterReminderIntervalMinutes: Int = 60,
    waterReminderSnoozeMinutes: Int = 15,
    waterReminderStartMinuteOfDay: Int = 480,
    waterReminderEndMinuteOfDay: Int = 1200
): PersistentSettings {
    return PersistentSettings(
        isAppUsageTrackingEnabled = isAppUsageTrackingEnabled,
        usageLimitNotificationsEnabled = usageLimitNotificationsEnabled,
        isAppBlockingEnabled = isAppBlockingEnabled,
        sharedDailyUsageLimitMinutes = sharedDailyUsageLimitMinutes,
        dailyLimitSnoozeUntilTimestamp = dailyLimitSnoozeUntilTimestamp,
        sessionSnoozeTimestamps = sessionSnoozeTimestamps,
        notifiedSharedDailyLimitDate = notifiedSharedDailyLimitDate,
        isWaterTrackingEnabled = isWaterTrackingEnabled,
        waterDailyTargetMl = waterDailyTargetMl,
        isWaterReminderEnabled = isWaterReminderEnabled,
        waterReminderIntervalMinutes = waterReminderIntervalMinutes,
        waterReminderSnoozeMinutes = waterReminderSnoozeMinutes,
        waterReminderStartMinuteOfDay = waterReminderStartMinuteOfDay,
        waterReminderEndMinuteOfDay = waterReminderEndMinuteOfDay,
    )
}