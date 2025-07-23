package com.andrew264.habits.ui.water.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.usecase.GetWaterHomeUiStateUseCase
import com.andrew264.habits.domain.usecase.LogWaterUseCase
import com.andrew264.habits.domain.usecase.UpdateWaterSettingsUseCase
import com.andrew264.habits.domain.usecase.WaterSettingsUpdate
import com.andrew264.habits.model.schedule.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterHomeUiState(
    val settings: PersistentSettings = PersistentSettings(selectedScheduleId = null, isBedtimeTrackingEnabled = false, isAppUsageTrackingEnabled = false, usageLimitNotificationsEnabled = false, isWaterTrackingEnabled = false, waterDailyTargetMl = 2500, isWaterReminderEnabled = false, waterReminderIntervalMinutes = 60, waterReminderSnoozeMinutes = 15, waterReminderScheduleId = null),
    val allSchedules: List<Schedule> = emptyList(),
    val todaysIntakeMl: Int = 0,
    val progress: Float = 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WaterHomeViewModel @Inject constructor(
    getWaterHomeUiStateUseCase: GetWaterHomeUiStateUseCase,
    private val logWaterUseCase: LogWaterUseCase,
    private val updateWaterSettingsUseCase: UpdateWaterSettingsUseCase
) : ViewModel() {

    private val _showTargetDialog = MutableStateFlow(false)
    val showTargetDialog = _showTargetDialog.asStateFlow()

    private val _showReminderDialog = MutableStateFlow(false)
    val showReminderDialog = _showReminderDialog.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<WaterHomeUiState> = refreshTrigger.flatMapLatest {
        getWaterHomeUiStateUseCase.execute()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WaterHomeUiState()
    )

    fun refresh() {
        refreshTrigger.value++
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            logWaterUseCase.execute(amountMl)
        }
    }

    fun onShowTargetDialog() {
        _showTargetDialog.value = true
    }

    fun onDismissTargetDialog() {
        _showTargetDialog.value = false
    }

    fun onShowReminderDialog() {
        _showReminderDialog.value = true
    }

    fun onDismissReminderDialog() {
        _showReminderDialog.value = false
    }

    fun saveTargetSettings(
        isEnabled: Boolean,
        targetMl: String
    ) {
        viewModelScope.launch {
            val target = targetMl.toIntOrNull() ?: uiState.value.settings.waterDailyTargetMl
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(isWaterTrackingEnabled = isEnabled, dailyTargetMl = target))
            onDismissTargetDialog()
        }
    }

    fun saveReminderSettings(
        isEnabled: Boolean,
        intervalMinutes: String,
        snoozeMinutes: String,
        schedule: Schedule?
    ) {
        viewModelScope.launch {
            val interval = intervalMinutes.toIntOrNull() ?: uiState.value.settings.waterReminderIntervalMinutes
            val snooze = snoozeMinutes.toIntOrNull() ?: uiState.value.settings.waterReminderSnoozeMinutes

            updateWaterSettingsUseCase.execute(
                WaterSettingsUpdate(
                    isReminderEnabled = isEnabled,
                    reminderIntervalMinutes = interval,
                    snoozeMinutes = snooze,
                    reminderScheduleId = schedule?.id
                )
            )
            onDismissReminderDialog()
        }
    }
}