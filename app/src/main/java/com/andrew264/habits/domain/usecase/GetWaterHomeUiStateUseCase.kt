package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WaterRepository
import com.andrew264.habits.ui.water.home.WaterHomeUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Gathers and processes data from multiple sources to build the [WaterHomeUiState].
 */
class GetWaterHomeUiStateUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository
) {
    fun execute(): Flow<WaterHomeUiState> {
        val todaysIntakeFlow = waterRepository.getTodaysIntakeFlow()

        return combine(
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
        }
    }
}