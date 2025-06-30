package com.andrew264.habits.repository

data class PersistentSettings(
    val isServiceActive: Boolean,
    val selectedScheduleId: String?,

    // Water Tracking Settings
    val isWaterTrackingEnabled: Boolean,
    val waterDailyTargetMl: Int,
    val isWaterReminderEnabled: Boolean,
    val waterReminderIntervalMinutes: Int,
    val waterReminderSnoozeMinutes: Int,
    val waterReminderScheduleId: String?
)