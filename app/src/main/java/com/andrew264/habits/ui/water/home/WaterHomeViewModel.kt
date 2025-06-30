package com.andrew264.habits.ui.water.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.data.entity.WaterIntakeEntry
import com.andrew264.habits.repository.SettingsRepository
import com.andrew264.habits.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterHomeUiState(
    val isEnabled: Boolean = false,
    val dailyTargetMl: Int = 2500,
    val todaysIntakeMl: Int = 0,
    val todaysLog: List<WaterIntakeEntry> = emptyList(),
    val progress: Float = 0f
)

@HiltViewModel
class WaterHomeViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val todaysIntakeFlow = waterRepository.getTodaysIntakeFlow()

    val uiState: StateFlow<WaterHomeUiState> = combine(
        settingsRepository.settingsFlow,
        todaysIntakeFlow
    ) { settings, todaysLog ->
        val todaysIntakeMl = todaysLog.sumOf { it.amountMl }
        val progress = if (settings.waterDailyTargetMl > 0) {
            (todaysIntakeMl.toFloat() / settings.waterDailyTargetMl.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        WaterHomeUiState(
            isEnabled = settings.isWaterTrackingEnabled,
            dailyTargetMl = settings.waterDailyTargetMl,
            todaysIntakeMl = todaysIntakeMl,
            todaysLog = todaysLog,
            progress = progress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WaterHomeUiState()
    )

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            waterRepository.logWater(amountMl)
        }
    }
}