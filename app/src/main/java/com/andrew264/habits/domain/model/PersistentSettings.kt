package com.andrew264.habits.domain.model

data class PersistentSettings(
    val selectedScheduleId: String?,
    val isBedtimeTrackingEnabled: Boolean,

    // App Usage Tracking Settings
    val isAppUsageTrackingEnabled: Boolean,
    val usageLimitNotificationsEnabled: Boolean,
    val isAppBlockingEnabled: Boolean,

    // Water Tracking Settings
    val isWaterTrackingEnabled: Boolean,
    val waterDailyTargetMl: Int,
    val isWaterReminderEnabled: Boolean,
    val waterReminderIntervalMinutes: Int,
    val waterReminderSnoozeMinutes: Int,
    val waterReminderScheduleId: String?
)