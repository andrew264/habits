package com.andrew264.habits.presentation.setBedtimeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.data.repository.SettingsRepository
import com.andrew264.habits.model.ManualSleepSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetBedtimeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val manualScheduleState: StateFlow<ManualSleepSchedule> = settingsRepository.settingsFlow
        .map { persistentSettings -> persistentSettings.manualSleepSchedule }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ManualSleepSchedule()
        )

    val currentBedtimeHour: StateFlow<Int?> = manualScheduleState
        .map { it.bedtimeHour }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            manualScheduleState.value.bedtimeHour
        )

    val currentBedtimeMinute: StateFlow<Int?> = manualScheduleState
        .map { it.bedtimeMinute }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            manualScheduleState.value.bedtimeMinute
        )

    val currentWakeUpHour: StateFlow<Int?> = manualScheduleState
        .map { it.wakeUpHour }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            manualScheduleState.value.wakeUpHour
        )

    val currentWakeUpMinute: StateFlow<Int?> = manualScheduleState
        .map { it.wakeUpMinute }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            manualScheduleState.value.wakeUpMinute
        )

    fun setBedtime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateManualBedtime(hour, minute)
        }
    }

    fun clearBedtime() {
        viewModelScope.launch {
            settingsRepository.updateManualBedtime(null, null)
        }
    }

    fun setWakeUpTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateManualWakeUpTime(hour, minute)
        }
    }

    fun clearWakeUpTime() {
        viewModelScope.launch {
            settingsRepository.updateManualWakeUpTime(null, null)
        }
    }
}