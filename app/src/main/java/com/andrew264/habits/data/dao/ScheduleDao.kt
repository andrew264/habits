package com.andrew264.habits.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.andrew264.habits.data.entity.schedule.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Upsert
    suspend fun upsert(schedule: ScheduleEntity)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun getScheduleById(id: String): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules ORDER BY name ASC")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>
}