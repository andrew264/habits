package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.WaterRepository
import javax.inject.Inject

/**
 * A simple use case to log a water intake entry.
 */
class LogWaterUseCase @Inject constructor(
    private val waterRepository: WaterRepository
) {
    suspend fun execute(amountMl: Int) {
        waterRepository.logWater(amountMl)
    }
}