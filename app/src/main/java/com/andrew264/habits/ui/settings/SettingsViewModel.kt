package com.andrew264.habits.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.usecase.StartPresenceMonitoringUseCase
import com.andrew264.habits.domain.usecase.StopPresenceMonitoringUseCase
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import com.andrew264.habits.util.AccessibilityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: PersistentSettings = createPreviewPersistentSettings(),
    val isAccessibilityServiceEnabled: Boolean = false
)

sealed interface SettingsEvent {
    object ShowAccessibilityDialog : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    startPresenceMonitoringUseCase: StartPresenceMonitoringUseCase,
    stopPresenceMonitoringUseCase: StopPresenceMonitoringUseCase,
) : ViewModel() {

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        _isAccessibilityEnabled
    ) { settings, isAccessibilityEnabled ->
        SettingsUiState(
            settings = settings,
            isAccessibilityServiceEnabled = isAccessibilityEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        updateAccessibilityStatus()
        // Centralize the service start/stop logic here.
        // This collector reacts to the source of truth (the settings repository).
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map { it.isAppUsageTrackingEnabled }
                .distinctUntilChanged()
                .collect { isEnabled ->
                    if (isEnabled) {
                        startPresenceMonitoringUseCase.execute()
                    } else {
                        stopPresenceMonitoringUseCase.execute()
                    }
                }
        }
    }

    fun onAppUsageTrackingToggled(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                if (AccessibilityUtils.isAccessibilityServiceEnabled(context)) {
                    settingsRepository.updateAppUsageTrackingEnabled(true)
                } else {
                    _events.emit(SettingsEvent.ShowAccessibilityDialog)
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