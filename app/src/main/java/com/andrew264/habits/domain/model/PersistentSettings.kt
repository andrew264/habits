package com.andrew264.habits.domain.model

data class PersistentSettings(
    // App Usage Tracking Settings
    val isAppUsageTrackingEnabled: Boolean,
    val usageLimitNotificationsEnabled: Boolean,
    val isAppBlockingEnabled: Boolean,
    val sharedDailyUsageLimitMinutes: Int?,
    val dailyLimitSnoozeUntilTimestamp: Long?,
    val sessionSnoozeTimestamps: Map<String, Long>,
    val notifiedSharedDailyLimitDate: String?,

    // Water Tracking Settings
    val isWaterTrackingEnabled: Boolean,
    val waterDailyTargetMl: Int,
    val isWaterReminderEnabled: Boolean,
    val waterReminderIntervalMinutes: Int,
    val waterReminderSnoozeMinutes: Int,
    val waterReminderStartMinuteOfDay: Int,
    val waterReminderEndMinuteOfDay: Int,
)