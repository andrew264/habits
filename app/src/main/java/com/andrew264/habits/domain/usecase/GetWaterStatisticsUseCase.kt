package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.analyzer.WaterStatistics
import com.andrew264.habits.domain.analyzer.WaterStatisticsAnalyzer
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WaterRepository
import com.andrew264.habits.ui.water.stats.StatsTimeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Fetches historical water intake data and analyzes it for a given time range.
 */
class GetWaterStatisticsUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val analyzer: WaterStatisticsAnalyzer
) {
    fun execute(range: StatsTimeRange): Flow<WaterStatistics> {
        val endDate = LocalDate.now()
        val startDate = when (range) {
            StatsTimeRange.WEEK -> endDate.minusDays(6)
            StatsTimeRange.MONTH -> endDate.minusDays(29)
        }

        return combine(
            waterRepository.getIntakeForDateRangeFlow(startDate, endDate),
            settingsRepository.settingsFlow
        ) { entries, settings ->
            analyzer.analyze(entries, settings.waterDailyTargetMl)
        }
    }
}