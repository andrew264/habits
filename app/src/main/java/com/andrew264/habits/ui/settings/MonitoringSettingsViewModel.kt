package com.andrew264.habits.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.usecase.StartPresenceMonitoringUseCase
import com.andrew264.habits.domain.usecase.StopPresenceMonitoringUseCase
import com.andrew264.habits.util.AccessibilityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonitoringSettingsUiState(
    val settings: PersistentSettings = PersistentSettings(null, false, false, false, 2500, false, 60, 15, null),
    val isAccessibilityServiceEnabled: Boolean = false
)

sealed interface MonitoringSettingsEvent {
    object RequestActivityPermission : MonitoringSettingsEvent
    object ShowAccessibilityDialog : MonitoringSettingsEvent
}

@HiltViewModel
class MonitoringSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val startPresenceMonitoringUseCase: StartPresenceMonitoringUseCase,
    private val stopPresenceMonitoringUseCase: StopPresenceMonitoringUseCase
) : ViewModel() {

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    private val _events = MutableSharedFlow<MonitoringSettingsEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<MonitoringSettingsUiState> = combine(
        settingsRepository.settingsFlow,
        _isAccessibilityEnabled
    ) { settings, isAccessibilityEnabled ->
        MonitoringSettingsUiState(
            settings = settings,
            isAccessibilityServiceEnabled = isAccessibilityEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonitoringSettingsUiState()
    )

    init {
        updateAccessibilityStatus()
    }

    fun onBedtimeTrackingToggled(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                _events.emit(MonitoringSettingsEvent.RequestActivityPermission)
            } else {
                // If turning off, just do it.
                stopPresenceMonitoringUseCase.execute()
                settingsRepository.updateBedtimeTrackingEnabled(false)
            }
        }
    }

    fun onActivityPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                settingsRepository.updateBedtimeTrackingEnabled(true)
                startPresenceMonitoringUseCase.execute()
            } else {
                // The toggle will remain off as the setting was never updated.
                // Optionally show a snackbar here if desired.
            }
        }
    }

    fun onAppUsageTrackingToggled(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                if (AccessibilityUtils.isAccessibilityServiceEnabled(context)) {
                    settingsRepository.updateAppUsageTrackingEnabled(true)
                } else {
                    _events.emit(MonitoringSettingsEvent.ShowAccessibilityDialog)
                }
            } else {
                settingsRepository.updateAppUsageTrackingEnabled(false)
            }
        }
    }

    fun onWaterTrackingToggled(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWaterTrackingEnabled(enable)
        }
    }

    fun updateAccessibilityStatus() {
        _isAccessibilityEnabled.value = AccessibilityUtils.isAccessibilityServiceEnabled(context)
    }
}