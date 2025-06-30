package com.andrew264.habits.repository

data class PersistentSettings(
    val isServiceActive: Boolean,
    val selectedScheduleId: String?
)