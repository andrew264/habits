package com.andrew264.habits.data.repository

import com.andrew264.habits.data.dao.CounterDao
import com.andrew264.habits.data.entity.counter.CounterEntity
import com.andrew264.habits.data.entity.counter.CounterLogEntity
import com.andrew264.habits.domain.model.Counter
import com.andrew264.habits.domain.model.CounterLog
import com.andrew264.habits.domain.repository.CounterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CounterRepositoryImpl @Inject constructor(
    private val counterDao: CounterDao
) : CounterRepository {

    override fun getAllCounters(): Flow<List<Counter>> {
        return counterDao.getAllCounters().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getCounterById(id: String): Flow<Counter?> {
        return counterDao.getCounterById(id).map { it?.toDomainModel() }
    }

    override suspend fun saveCounter(counter: Counter) {
        counterDao.upsertCounter(counter.toEntity())
    }

    override suspend fun deleteCounter(counter: Counter) {
        counterDao.deleteCounter(counter.toEntity())
    }

    override suspend fun addLog(log: CounterLog) {
        counterDao.insertLog(log.toEntity())
    }

    override suspend fun deleteLog(log: CounterLog) {
        counterDao.deleteLog(log.toEntity())
    }

    override fun getLogsForCounter(counterId: String): Flow<List<CounterLog>> {
        return counterDao.getLogsForCounter(counterId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getLogsForCounterInRange(
        counterId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<CounterLog>> {
        return counterDao.getLogsForCounterInRange(counterId, startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllLogsInRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<CounterLog>> {
        return counterDao.getAllLogsInRange(startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}

private fun CounterEntity.toDomainModel(): Counter {
    return Counter(
        id = this.id,
        name = this.name,
        type = this.type,
        aggregationType = this.aggregationType,
        target = this.target,
        colorHex = this.colorHex
    )
}

private fun Counter.toEntity(): CounterEntity {
    return CounterEntity(
        id = this.id,
        name = this.name,
        type = this.type,
        aggregationType = this.aggregationType,
        target = this.target,
        colorHex = this.colorHex
    )
}

private fun CounterLogEntity.toDomainModel(): CounterLog {
    return CounterLog(
        id = this.id,
        counterId = this.counterId,
        timestamp = this.timestamp,
        value = this.value
    )
}

private fun CounterLog.toEntity(): CounterLogEntity {
    return CounterLogEntity(
        id = this.id,
        counterId = this.counterId,
        timestamp = this.timestamp,
        value = this.value
    )
}