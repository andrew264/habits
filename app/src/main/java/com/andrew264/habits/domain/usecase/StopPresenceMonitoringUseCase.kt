package com.andrew264.habits.domain.usecase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.service.UserPresenceService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StopPresenceMonitoringUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun execute() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_STOP_SERVICE
        }
        try {
            // Use startService for a stop command.
            context.startService(serviceIntent)
            Log.d("StopPresenceMonitoringUseCase", "Service stop command sent.")
        } catch (e: Exception) {
            Log.e("StopPresenceMonitoringUseCase", "Error requesting service stop", e)
        }
    }
}