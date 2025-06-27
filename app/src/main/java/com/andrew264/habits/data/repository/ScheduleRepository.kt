package com.andrew264.habits.data.repository

import com.andrew264.habits.data.dao.ScheduleDao
import com.andrew264.habits.data.entity.schedule.ScheduleEntity
import com.andrew264.habits.model.schedule.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao
) {
    fun getSchedule(id: String): Flow<Schedule?> {
        return scheduleDao.getScheduleById(id).map { entity ->
            entity?.toDomainModel()
        }
    }

    fun getAllSchedules(): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedules().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun saveSchedule(schedule: Schedule) {
        scheduleDao.upsert(schedule.toEntity())
    }

    suspend fun deleteSchedule(schedule: Schedule) {
        scheduleDao.delete(schedule.toEntity())
    }
}

private fun ScheduleEntity.toDomainModel(): Schedule {
    return Schedule(
        id = this.id,
        name = this.name,
        groups = this.groups
    )
}

private fun Schedule.toEntity(): ScheduleEntity {
    return ScheduleEntity(
        id = this.id,
        name = this.name,
        groups = this.groups
    )
}