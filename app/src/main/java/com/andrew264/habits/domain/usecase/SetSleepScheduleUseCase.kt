package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Updates the user's selected sleep schedule.
 */
class SetSleepScheduleUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun execute(scheduleId: String) {
        settingsRepository.updateSelectedScheduleId(scheduleId)
    }
}