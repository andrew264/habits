package com.andrew264.habits.data.repository

import com.andrew264.habits.data.dao.WaterIntakeDao
import com.andrew264.habits.data.entity.WaterIntakeEntry
import com.andrew264.habits.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepositoryImpl @Inject constructor(
    private val waterIntakeDao: WaterIntakeDao
) : WaterRepository {
    override suspend fun logWater(amountMl: Int) {
        val entry = WaterIntakeEntry(
            timestamp = System.currentTimeMillis(),
            amountMl = amountMl
        )
        waterIntakeDao.insert(entry)
    }

    override fun getTodaysIntakeFlow(): Flow<List<WaterIntakeEntry>> {
        val today = LocalDate.now()
        val startOfTodayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val startOfTomorrowMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return waterIntakeDao.getEntriesInRangeFlow(startOfTodayMillis, startOfTomorrowMillis)
    }

    override fun getIntakeForDateRangeFlow(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<WaterIntakeEntry>> {
        val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // end.plusDays(1) to make the range inclusive of the end date
        val endMillis = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return waterIntakeDao.getEntriesInRangeFlow(startMillis, endMillis)
    }
}