package com.andrew264.habits.data.repository

import androidx.room.Transaction
import com.andrew264.habits.data.dao.ScheduleDao
import com.andrew264.habits.data.entity.schedule.ScheduleEntity
import com.andrew264.habits.data.entity.schedule.ScheduleGroupDayEntity
import com.andrew264.habits.data.entity.schedule.ScheduleGroupEntity
import com.andrew264.habits.data.entity.schedule.ScheduleTimeRangeEntity
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao
) : ScheduleRepository {
    override fun getSchedule(id: String): Flow<Schedule?> {
        return scheduleDao.getScheduleWithRelationsById(id).map { relation ->
            relation?.toDomainModel()
        }
    }

    override fun getAllSchedules(): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedulesWithRelations().map { relations ->
            relations.map { it.toDomainModel() }
        }
    }

    @Transaction
    override suspend fun saveSchedule(schedule: Schedule) {
        scheduleDao.upsertScheduleEntity(schedule.toEntity())
        // Deleting old groups will cascade to days and time ranges due to ForeignKey constraints
        scheduleDao.deleteGroupsForSchedule(schedule.id)

        schedule.groups.forEach { group ->
            scheduleDao.upsertScheduleGroupEntity(group.toEntity(schedule.id))
            if (group.days.isNotEmpty()) {
                scheduleDao.insertScheduleGroupDays(group.days.map { day ->
                    ScheduleGroupDayEntity(groupId = group.id, dayOfWeek = day)
                })
            }
            if (group.timeRanges.isNotEmpty()) {
                scheduleDao.insertScheduleTimeRanges(group.timeRanges.map { timeRange ->
                    ScheduleTimeRangeEntity(
                        groupId = group.id,
                        fromMinuteOfDay = timeRange.fromMinuteOfDay,
                        toMinuteOfDay = timeRange.toMinuteOfDay
                    )
                })
            }
        }
    }

    override suspend fun deleteSchedule(schedule: Schedule) {
        // Deleting the parent schedule will cascade delete all children thanks to onDelete = CASCADE
        scheduleDao.deleteScheduleEntity(schedule.toEntity())
    }
}

private fun Schedule.toEntity(): ScheduleEntity {
    return ScheduleEntity(
        id = this.id,
        name = this.name,
    )
}

private fun ScheduleGroup.toEntity(scheduleId: String): ScheduleGroupEntity {
    return ScheduleGroupEntity(
        id = this.id,
        scheduleId = scheduleId,
        name = this.name
    )
}