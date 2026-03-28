package com.andrew264.habits.domain.repository

import com.andrew264.habits.domain.model.Counter
import com.andrew264.habits.domain.model.CounterLog
import kotlinx.coroutines.flow.Flow

interface CounterRepository {
    fun getAllCounters(): Flow<List<Counter>>
    fun getCounterById(id: String): Flow<Counter?>
    suspend fun saveCounter(counter: Counter)
    suspend fun deleteCounter(counter: Counter)

    suspend fun addLog(log: CounterLog)
    suspend fun deleteLog(log: CounterLog)
    fun getLogsForCounter(counterId: String): Flow<List<CounterLog>>
    fun getLogsForCounterInRange(counterId: String, startTime: Long, endTime: Long): Flow<List<CounterLog>>
    fun getLogsForCounterFrom(counterId: String, startTime: Long): Flow<List<CounterLog>>
    fun getAllLogsInRange(startTime: Long, endTime: Long): Flow<List<CounterLog>>
}