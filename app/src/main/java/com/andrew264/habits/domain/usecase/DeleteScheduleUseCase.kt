package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.model.schedule.Schedule
import javax.inject.Inject

/**
 * Encapsulates the business logic for deleting a schedule.
 */
class DeleteScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    suspend fun execute(schedule: Schedule) {
        // Deleting the parent schedule will cascade delete all children thanks to onDelete = CASCADE
        scheduleRepository.deleteSchedule(schedule)
    }
}