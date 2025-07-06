package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WaterRepository
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.ui.water.home.WaterHomeUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Gathers and processes data from multiple sources to build the [WaterHomeUiState].
 */
class GetWaterHomeUiStateUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository
) {
    fun execute(): Flow<WaterHomeUiState> {
        val todaysIntakeFlow = waterRepository.getTodaysIntakeFlow()
        val allSchedulesFlow = scheduleRepository.getAllSchedules()
            .map { dbSchedules ->
                listOf(DefaultSchedules.defaultSleepSchedule) + dbSchedules
            }

        return combine(
            settingsRepository.settingsFlow,
            todaysIntakeFlow,
            allSchedulesFlow
        ) { settings, todaysLog, allSchedules ->
            val todaysIntakeMl = todaysLog.sumOf { it.amountMl }
            val progress = if (settings.waterDailyTargetMl > 0) {
                (todaysIntakeMl.toFloat() / settings.waterDailyTargetMl.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            WaterHomeUiState(
                settings = settings,
                allSchedules = allSchedules,
                todaysIntakeMl = todaysIntakeMl,
                todaysLog = todaysLog,
                progress = progress
            )
        }
    }
}