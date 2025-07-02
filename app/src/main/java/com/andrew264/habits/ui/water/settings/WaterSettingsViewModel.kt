package com.andrew264.habits.ui.water.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.usecase.GetWaterSettingsUseCase
import com.andrew264.habits.domain.usecase.UpdateWaterSettingsUseCase
import com.andrew264.habits.domain.usecase.WaterSettingsData
import com.andrew264.habits.domain.usecase.WaterSettingsUpdate
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterSettingsViewModel @Inject constructor(
    getWaterSettingsUseCase: GetWaterSettingsUseCase,
    private val updateWaterSettingsUseCase: UpdateWaterSettingsUseCase
) : ViewModel() {

    private val waterSettingsData: StateFlow<WaterSettingsData> = getWaterSettingsUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterSettingsData(
                settings = PersistentSettings(false, null, false, 2500, false, 60, 15, null),
                allSchedules = listOf(DefaultSchedules.defaultSleepSchedule)
            )
        )

    val settings: StateFlow<PersistentSettings> = waterSettingsData.map { it.settings }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = waterSettingsData.value.settings
        )

    val allSchedules: StateFlow<List<Schedule>> = waterSettingsData.map { it.allSchedules }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = waterSettingsData.value.allSchedules
        )

    fun onWaterTrackingEnabledChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(isWaterTrackingEnabled = isEnabled))
        }
    }

    fun onDailyTargetChanged(targetMl: String) {
        viewModelScope.launch {
            val target = targetMl.toIntOrNull() ?: settings.value.waterDailyTargetMl
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(dailyTargetMl = target))
        }
    }

    fun onReminderEnabledChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(isReminderEnabled = isEnabled))
        }
    }

    fun onReminderIntervalChanged(intervalMinutes: String) {
        viewModelScope.launch {
            val interval = intervalMinutes.toIntOrNull() ?: settings.value.waterReminderIntervalMinutes
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(reminderIntervalMinutes = interval))
        }
    }

    fun onSnoozeTimeChanged(snoozeMinutes: String) {
        viewModelScope.launch {
            val snooze = snoozeMinutes.toIntOrNull() ?: settings.value.waterReminderSnoozeMinutes
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(snoozeMinutes = snooze))
        }
    }

    fun onReminderScheduleChanged(schedule: Schedule) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(reminderScheduleId = schedule.id))
        }
    }
}