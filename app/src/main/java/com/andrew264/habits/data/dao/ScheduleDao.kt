package com.andrew264.habits.data.dao

import androidx.room.*
import com.andrew264.habits.data.entity.schedule.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    // --- QUERY ---

    @Transaction
    @Query("SELECT * FROM schedules WHERE schedule_id = :id")
    fun getScheduleWithRelationsById(id: String): Flow<ScheduleWithRelations?>

    @Transaction
    @Query("SELECT * FROM schedules ORDER BY name ASC")
    fun getAllSchedulesWithRelations(): Flow<List<ScheduleWithRelations>>

    // --- UPSERT HELPERS (for use in repository transaction) ---

    @Upsert
    suspend fun upsertScheduleEntity(schedule: ScheduleEntity)

    @Upsert
    suspend fun upsertScheduleGroupEntity(group: ScheduleGroupEntity)

    @Insert
    suspend fun insertScheduleGroupDays(days: List<ScheduleGroupDayEntity>)

    @Insert
    suspend fun insertScheduleTimeRanges(timeRanges: List<ScheduleTimeRangeEntity>)

    @Query("DELETE FROM schedule_groups WHERE schedule_id_fk = :scheduleId")
    suspend fun deleteGroupsForSchedule(scheduleId: String)

    // --- DELETE ---

    @Delete
    suspend fun deleteScheduleEntity(schedule: ScheduleEntity)
}