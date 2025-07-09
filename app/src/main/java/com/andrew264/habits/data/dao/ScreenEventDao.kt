package com.andrew264.habits.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.andrew264.habits.data.entity.ScreenEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenEventDao {
    @Insert
    suspend fun insert(event: ScreenEventEntity)

    @Query("SELECT * FROM screen_history WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    fun getEventsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<ScreenEventEntity>>
}