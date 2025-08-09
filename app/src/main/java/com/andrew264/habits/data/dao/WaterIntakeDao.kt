package com.andrew264.habits.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrew264.habits.data.entity.WaterIntakeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterIntakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WaterIntakeEntry)

    @Query("SELECT * FROM water_intake_history ORDER BY timestamp DESC")
    fun getAllEntriesFlow(): Flow<List<WaterIntakeEntry>>

    @Query("SELECT * FROM water_intake_history WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getEntriesInRangeFlow(
        startTime: Long,
        endTime: Long
    ): Flow<List<WaterIntakeEntry>>

    @Query("DELETE FROM water_intake_history WHERE timestamp >= :startTime")
    suspend fun deleteEntriesFrom(startTime: Long)

    @Query("DELETE FROM water_intake_history")
    suspend fun deleteAllEntries()
}