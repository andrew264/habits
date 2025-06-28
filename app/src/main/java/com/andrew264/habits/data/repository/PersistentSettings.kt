package com.andrew264.habits.data.repository

data class PersistentSettings(
    val isServiceActive: Boolean,
    val selectedScheduleId: String?
)