package com.andrew264.habits.ui.water.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.analyzer.WaterStatistics
import com.andrew264.habits.domain.usecase.GetWaterStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class StatsTimeRange(val label: String) {
    WEEK("Past 7 Days"),
    MONTH("Past 30 Days")
}

data class WaterStatsUiState(
    val selectedRange: StatsTimeRange = StatsTimeRange.WEEK,
    val stats: WaterStatistics? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WaterStatsViewModel @Inject constructor(
    private val getWaterStatisticsUseCase: GetWaterStatisticsUseCase
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(StatsTimeRange.WEEK)

    val uiState: StateFlow<WaterStatsUiState> = _selectedRange.flatMapLatest { range ->
        getWaterStatisticsUseCase.execute(range).map { stats ->
            WaterStatsUiState(
                selectedRange = range,
                stats = stats,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WaterStatsUiState()
    )

    fun setTimeRange(range: StatsTimeRange) {
        _selectedRange.value = range
    }
}