package com.andrew264.habits.ui.water.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.data.entity.WaterIntakeEntry
import com.andrew264.habits.domain.usecase.GetWaterHomeUiStateUseCase
import com.andrew264.habits.domain.usecase.LogWaterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    getWaterHomeUiStateUseCase: GetWaterHomeUiStateUseCase,
    private val logWaterUseCase: LogWaterUseCase
) : ViewModel() {

    val uiState: StateFlow<WaterHomeUiState> = getWaterHomeUiStateUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterHomeUiState()
        )

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            logWaterUseCase.execute(amountMl)
        }
    }
}