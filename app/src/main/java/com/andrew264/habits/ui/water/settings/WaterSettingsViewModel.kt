package com.andrew264.habits.ui.water.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.manager.WaterReminderManager
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.repository.PersistentSettings
import com.andrew264.habits.repository.ScheduleRepository
import com.andrew264.habits.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val waterReminderManager: WaterReminderManager,
    scheduleRepository: ScheduleRepository
) : ViewModel() {

    val settings: StateFlow<PersistentSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersistentSettings(
                isServiceActive = false,
                selectedScheduleId = null,
                isWaterTrackingEnabled = false,
                waterDailyTargetMl = 2500,
                isWaterReminderEnabled = false,
                waterReminderIntervalMinutes = 60,
                waterReminderSnoozeMinutes = 15,
                waterReminderScheduleId = null
            )
        )

    val allSchedules: StateFlow<List<Schedule>> = scheduleRepository.getAllSchedules()
        .map { dbSchedules ->
            listOf(DefaultSchedules.defaultSleepSchedule) + dbSchedules
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(DefaultSchedules.defaultSleepSchedule)
        )

    fun onWaterTrackingEnabledChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWaterTrackingEnabled(isEnabled)
            if (!isEnabled) {
                // If the whole feature is turned off, cancel any pending reminders
                waterReminderManager.cancelReminders()
            }
        }
    }

    fun onDailyTargetChanged(targetMl: String) {
        viewModelScope.launch {
            val target = targetMl.toIntOrNull() ?: settings.value.waterDailyTargetMl
            settingsRepository.updateWaterDailyTarget(target)
        }
    }

    fun onReminderEnabledChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWaterReminderEnabled(isEnabled)
            if (isEnabled) {
                waterReminderManager.scheduleNextReminder(settings.value.waterReminderIntervalMinutes.toLong())
            } else {
                waterReminderManager.cancelReminders()
            }
        }
    }

    fun onReminderIntervalChanged(intervalMinutes: String) {
        viewModelScope.launch {
            val interval = intervalMinutes.toIntOrNull() ?: settings.value.waterReminderIntervalMinutes
            settingsRepository.updateWaterReminderInterval(interval)
            if (settings.value.isWaterReminderEnabled) {
                waterReminderManager.scheduleNextReminder(interval.toLong())
            }
        }
    }

    fun onSnoozeTimeChanged(snoozeMinutes: String) {
        viewModelScope.launch {
            val snooze = snoozeMinutes.toIntOrNull() ?: settings.value.waterReminderSnoozeMinutes
            settingsRepository.updateWaterReminderSnoozeTime(snooze)
        }
    }

    fun onReminderScheduleChanged(schedule: Schedule) {
        viewModelScope.launch {
            settingsRepository.updateWaterReminderSchedule(schedule.id)
        }
    }
}