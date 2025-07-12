package com.andrew264.habits.data.dao

import androidx.room.*
import com.andrew264.habits.data.entity.AppUsageEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageEventDao {
    @Insert
    suspend fun insert(event: AppUsageEventEntity): Long

    @Update
    suspend fun update(event: AppUsageEventEntity)

    @Delete
    suspend fun delete(event: AppUsageEventEntity)

    @Query("SELECT * FROM app_usage_history WHERE end_timestamp IS NULL LIMIT 1")
    suspend fun getOngoingEvent(): AppUsageEventEntity?

    @Query("SELECT * FROM app_usage_history ORDER BY start_timestamp DESC LIMIT 1 OFFSET 1")
    suspend fun getSecondToLastEvent(): AppUsageEventEntity?

    /**
     * Retrieves all events that overlap with the given time range.
     * This includes events that start before and end within, start within and end after,
     * start and end within, and events that start before and end after (enveloping the range).
     */
    @Query(
        "SELECT * FROM app_usage_history WHERE " +
                "start_timestamp < :endTime AND (end_timestamp IS NULL OR end_timestamp > :startTime) " +
                "ORDER BY start_timestamp ASC"
    )
    fun getEventsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<AppUsageEventEntity>>
}