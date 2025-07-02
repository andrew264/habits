package com.andrew264.habits.domain.repository

import com.andrew264.habits.data.entity.WaterIntakeEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface WaterRepository {
    suspend fun logWater(amountMl: Int)
    fun getTodaysIntakeFlow(): Flow<List<WaterIntakeEntry>>
    fun getIntakeForDateRangeFlow(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<WaterIntakeEntry>>
}