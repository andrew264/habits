package com.andrew264.habits.data.dao

import androidx.room.*
import com.andrew264.habits.data.entity.counter.CounterEntity
import com.andrew264.habits.data.entity.counter.CounterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {

    // --- Counters ---
    @Query("SELECT * FROM counters ORDER BY name ASC")
    fun getAllCounters(): Flow<List<CounterEntity>>

    @Query("SELECT * FROM counters WHERE counter_id = :id LIMIT 1")
    fun getCounterById(id: String): Flow<CounterEntity?>

    @Upsert
    suspend fun upsertCounter(counter: CounterEntity)

    @Delete
    suspend fun deleteCounter(counter: CounterEntity)

    // --- Logs ---
    @Insert
    suspend fun insertLog(log: CounterLogEntity)

    @Delete
    suspend fun deleteLog(log: CounterLogEntity)

    @Query("SELECT * FROM counter_logs WHERE counter_id_fk = :counterId ORDER BY timestamp DESC")
    fun getLogsForCounter(counterId: String): Flow<List<CounterLogEntity>>

    @Query("SELECT * FROM counter_logs WHERE counter_id_fk = :counterId AND timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    fun getLogsForCounterInRange(counterId: String, startTime: Long, endTime: Long): Flow<List<CounterLogEntity>>

    @Query("SELECT * FROM counter_logs WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    fun getAllLogsInRange(startTime: Long, endTime: Long): Flow<List<CounterLogEntity>>

    @Query("DELETE FROM counter_logs WHERE timestamp >= :startTime")
    suspend fun deleteLogsFrom(startTime: Long)

    @Query("DELETE FROM counter_logs")
    suspend fun deleteAllLogs()
}