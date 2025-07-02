package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.model.schedule.Schedule
import javax.inject.Inject

class SaveScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    sealed class Result {
        object Success : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun execute(schedule: Schedule): Result {
        if (schedule.name.isBlank()) {
            return Result.Failure("Schedule name cannot be empty.")
        }
        // Any other validation rules can be added here
        scheduleRepository.saveSchedule(schedule)
        return Result.Success
    }
}