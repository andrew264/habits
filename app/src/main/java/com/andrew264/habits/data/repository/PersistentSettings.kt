package com.andrew264.habits.data.repository

import com.andrew264.habits.model.ManualSleepSchedule

data class PersistentSettings(
    val isServiceActive: Boolean,
    val manualSleepSchedule: ManualSleepSchedule
)