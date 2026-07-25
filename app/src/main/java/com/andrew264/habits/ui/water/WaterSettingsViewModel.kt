package com.andrew264.habits.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.usecase.UpdateWaterSettingsUseCase
import com.andrew264.habits.domain.usecase.WaterSettingsUpdate
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterSettingsUiState(
    val settings: PersistentSettings = createPreviewPersistentSettings()
)

@HiltViewModel
class WaterSettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val updateWaterSettingsUseCase: UpdateWaterSettingsUseCase
) : ViewModel() {

    private val _showTargetDialog = MutableStateFlow(false)
    val showTargetDialog = _showTargetDialog.asStateFlow()

    val uiState: StateFlow<WaterSettingsUiState> = settingsRepository.settingsFlow
        .map { WaterSettingsUiState(settings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterSettingsUiState()
        )

    fun onWaterTrackingToggled(isEnabled: Boolean) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(isWaterTrackingEnabled = isEnabled))
        }
    }

    fun onRemindersToggled(isEnabled: Boolean) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(isReminderEnabled = isEnabled))
        }
    }

    fun onShowTargetDialog() {
        _showTargetDialog.value = true
    }

    fun onDismissTargetDialog() {
        _showTargetDialog.value = false
    }

    fun saveTargetSettings(targetMl: String) {
        viewModelScope.launch {
            val target = targetMl.toIntOrNull() ?: uiState.value.settings.waterDailyTargetMl
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(dailyTargetMl = target))
            onDismissTargetDialog()
        }
    }

    fun onIntervalChanged(minutes: String) {
        viewModelScope.launch {
            minutes.toIntOrNull()?.let {
                updateWaterSettingsUseCase.execute(WaterSettingsUpdate(reminderIntervalMinutes = it))
            }
        }
    }

    fun onSnoozeChanged(minutes: String) {
        viewModelScope.launch {
            minutes.toIntOrNull()?.let {
                updateWaterSettingsUseCase.execute(WaterSettingsUpdate(snoozeMinutes = it))
            }
        }
    }

    fun onStartMinuteChanged(minuteOfDay: Int) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(reminderStartMinuteOfDay = minuteOfDay))
        }
    }

    fun onEndMinuteChanged(minuteOfDay: Int) {
        viewModelScope.launch {
            updateWaterSettingsUseCase.execute(WaterSettingsUpdate(reminderEndMinuteOfDay = minuteOfDay))
        }
    }
}