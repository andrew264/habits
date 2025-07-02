package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Encapsulates the business logic for deleting a schedule.
 * It checks for any assignments before proceeding with the deletion.
 */
class DeleteScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository
) {
    sealed class Result {
        object Success : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun execute(schedule: Schedule): Result {
        if (schedule.id == DefaultSchedules.DEFAULT_SLEEP_SCHEDULE_ID) {
            return Result.Failure("The default schedule cannot be deleted.")
        }

        val currentSettings = settingsRepository.settingsFlow.first()
        val isSleepSchedule = currentSettings.selectedScheduleId == schedule.id
        val isWaterSchedule = currentSettings.waterReminderScheduleId == schedule.id

        if (isSleepSchedule || isWaterSchedule) {
            val usage = when {
                isSleepSchedule && isWaterSchedule -> "sleep tracking and water reminders"
                isSleepSchedule -> "sleep tracking"
                else -> "water reminders"
            }
            return Result.Failure("Cannot delete schedule. It's assigned to $usage.")
        }

        scheduleRepository.deleteSchedule(schedule)
        return Result.Success
    }
}