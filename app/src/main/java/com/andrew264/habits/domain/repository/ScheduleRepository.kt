package com.andrew264.habits.domain.repository

import com.andrew264.habits.model.schedule.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getSchedule(id: String): Flow<Schedule?>
    fun getAllSchedules(): Flow<List<Schedule>>
    suspend fun saveSchedule(schedule: Schedule)
    suspend fun deleteSchedule(schedule: Schedule)
}