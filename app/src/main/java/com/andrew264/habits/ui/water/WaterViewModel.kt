package com.andrew264.habits.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.usecase.GetWaterUiStateUseCase
import com.andrew264.habits.domain.usecase.LogWaterUseCase
import com.andrew264.habits.domain.usecase.UpdateWaterSettingsUseCase
import com.andrew264.habits.domain.usecase.WaterSettingsUpdate
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterUiState(
    val settings: PersistentSettings = createPreviewPersistentSettings(),
    val allSchedules: List<Schedule> = emptyList(),
    val todaysIntakeMl: Int = 0,
    val progress: Float = 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WaterViewModel @Inject constructor(
    getWaterUiStateUseCase: GetWaterUiStateUseCase,
    private val logWaterUseCase: LogWaterUseCase,
    private val updateWaterSettingsUseCase: UpdateWaterSettingsUseCase
) : ViewModel() {

    private val _showTargetDialog = MutableStateFlow(false)
    val showTargetDialog = _showTargetDialog.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<WaterUiState> = refreshTrigger.flatMapLatest {
        getWaterUiStateUseCase.execute()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WaterUiState()
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
}