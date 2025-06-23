package com.andrew264.habits.presentation.setBedtimeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.manager.UserPresenceController
import com.andrew264.habits.service.UserPresenceService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SetBedtimeViewModel @Inject constructor(
    private val userPresenceController: UserPresenceController
) : ViewModel() {

    val currentBedtimeHour: StateFlow<Int?> = UserPresenceService.manualSleepSchedule
        .map { it.bedtimeHour }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPresenceService.manualSleepSchedule.value.bedtimeHour
        )

    val currentBedtimeMinute: StateFlow<Int?> = UserPresenceService.manualSleepSchedule
        .map { it.bedtimeMinute }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPresenceService.manualSleepSchedule.value.bedtimeMinute
        )

    val currentWakeUpHour: StateFlow<Int?> = UserPresenceService.manualSleepSchedule
        .map { it.wakeUpHour }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPresenceService.manualSleepSchedule.value.wakeUpHour
        )

    val currentWakeUpMinute: StateFlow<Int?> = UserPresenceService.manualSleepSchedule
        .map { it.wakeUpMinute }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPresenceService.manualSleepSchedule.value.wakeUpMinute
        )

    fun setBedtime(hour: Int, minute: Int) {
        userPresenceController.setManualBedtime(hour, minute)
    }

    fun clearBedtime() {
        userPresenceController.clearManualBedtime()
    }

    fun setWakeUpTime(hour: Int, minute: Int) {
        userPresenceController.setManualWakeUpTime(hour, minute)
    }

    fun clearWakeUpTime() {
        userPresenceController.clearManualWakeUpTime()
    }
}