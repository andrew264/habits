package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class WaterSettingsData(
    val settings: PersistentSettings,
    val allSchedules: List<Schedule>
)

/**
 * Gathers all data needed for the Water Settings screen.
 */
class GetWaterSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository
) {
    fun execute(): Flow<WaterSettingsData> {
        val allSchedulesFlow = scheduleRepository.getAllSchedules()
            .map { dbSchedules ->
                listOf(DefaultSchedules.defaultSleepSchedule) + dbSchedules
            }

        return combine(
            settingsRepository.settingsFlow,
            allSchedulesFlow
        ) { settings, schedules ->
            WaterSettingsData(
                settings = settings,
                allSchedules = schedules
            )
        }
    }
}