package com.andrew264.habits.ui.water.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.analyzer.WaterStatistics
import com.andrew264.habits.domain.analyzer.WaterStatisticsAnalyzer
import com.andrew264.habits.repository.SettingsRepository
import com.andrew264.habits.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
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
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val analyzer: WaterStatisticsAnalyzer
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(StatsTimeRange.WEEK)

    val uiState: StateFlow<WaterStatsUiState> = _selectedRange.flatMapLatest { range ->
        val endDate = LocalDate.now()
        val startDate = when (range) {
            StatsTimeRange.WEEK -> endDate.minusDays(6)
            StatsTimeRange.MONTH -> endDate.minusDays(29)
        }

        combine(
            waterRepository.getIntakeForDateRangeFlow(startDate, endDate),
            settingsRepository.settingsFlow
        ) { entries, settings ->
            val stats = analyzer.analyze(entries, settings.waterDailyTargetMl)
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