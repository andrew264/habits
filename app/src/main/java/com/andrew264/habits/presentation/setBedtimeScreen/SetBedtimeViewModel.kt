package com.andrew264.habits.presentation.setBedtimeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.manager.UserPresenceController
import com.andrew264.habits.service.UserPresenceService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SetBedtimeViewModel @Inject constructor(
    private val userPresenceController: UserPresenceController
) : ViewModel() {

    val currentBedtimeHour: StateFlow<Int?> = UserPresenceService.manualBedtimeHour
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPresenceService.manualBedtimeHour.value
        )

    val currentBedtimeMinute: StateFlow<Int?> = UserPresenceService.manualBedtimeMinute
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPresenceService.manualBedtimeMinute.value
        )

    val currentWakeUpHour: StateFlow<Int?> = UserPresenceService.manualWakeUpHour
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPresenceService.manualWakeUpHour.value
        )

    val currentWakeUpMinute: StateFlow<Int?> = UserPresenceService.manualWakeUpMinute
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPresenceService.manualWakeUpMinute.value
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