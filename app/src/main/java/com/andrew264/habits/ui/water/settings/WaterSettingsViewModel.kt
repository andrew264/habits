package com.andrew264.habits.ui.water.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.usecase.GetWaterSettingsUseCase
import com.andrew264.habits.domain.usecase.UpdateWaterSettingsUseCase
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

data class WaterSettingsUiState(
    val settings: PersistentSettings,
    val allSchedules: List<Schedule>
) {
    companion object {
        val default = WaterSettingsUiState(
            settings = PersistentSettings(false, null, false, 2500, false, 60, 15, null),
            allSchedules = listOf(DefaultSchedules.defaultSleepSchedule)
        )
    }
}

@HiltViewModel
class WaterSettingsViewModel @Inject constructor(
    getWaterSettingsUseCase: GetWaterSettingsUseCase,
    private val updateWaterSettingsUseCase: UpdateWaterSettingsUseCase
) : ViewModel() {

    val uiState: StateFlow<WaterSettingsUiState> = getWaterSettingsUseCase.execute()
        .map { data ->
            WaterSettingsUiState(
                settings = data.settings,
                allSchedules = data.allSchedules
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterSettingsUiState.default
        )

    fun onWaterTrackingEnabledChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(isWaterTrackingEnabled = isEnabled))
        }
    }

    fun onDailyTargetChanged(targetMl: String) {
        viewModelScope.launch {
            val target = targetMl.toIntOrNull() ?: uiState.value.settings.waterDailyTargetMl
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
            val interval = intervalMinutes.toIntOrNull() ?: uiState.value.settings.waterReminderIntervalMinutes
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(reminderIntervalMinutes = interval))
        }
    }

    fun onSnoozeTimeChanged(snoozeMinutes: String) {
        viewModelScope.launch {
            val snooze = snoozeMinutes.toIntOrNull() ?: uiState.value.settings.waterReminderSnoozeMinutes
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(snoozeMinutes = snooze))
        }
    }

    fun onReminderScheduleChanged(schedule: Schedule) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(reminderScheduleId = schedule.id))
        }
    }
}