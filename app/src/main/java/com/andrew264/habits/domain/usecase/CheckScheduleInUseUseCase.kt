package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.model.schedule.DefaultSchedules
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckScheduleInUseUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    sealed class Result {
        object NotInUse : Result()
        data class InUse(val usageMessage: String) : Result()
        object IsDefault : Result()
    }

    suspend fun execute(scheduleId: String): Result {
        if (scheduleId == DefaultSchedules.DEFAULT_SLEEP_SCHEDULE_ID) {
            return Result.IsDefault
        }

        val currentSettings = settingsRepository.settingsFlow.first()
        val isWaterSchedule = currentSettings.waterReminderScheduleId == scheduleId

        if (isWaterSchedule) {
            return Result.InUse("Cannot delete schedule. It's assigned to water reminders.")
        }

        return Result.NotInUse
    }
}