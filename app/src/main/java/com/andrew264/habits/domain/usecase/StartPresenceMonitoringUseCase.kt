package com.andrew264.habits.domain.usecase

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.service.UserPresenceService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StartPresenceMonitoringUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend fun execute() {
        settingsRepository.updateServiceActiveState(true)
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_START_SERVICE
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.d("StartPresenceMonitoringUseCase", "Service start command sent.")
        } catch (e: Exception) {
            Log.e("StartPresenceMonitoringUseCase", "Error requesting service start", e)
        }
    }
}