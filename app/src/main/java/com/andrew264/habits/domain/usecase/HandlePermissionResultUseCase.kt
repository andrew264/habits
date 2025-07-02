package com.andrew264.habits.domain.usecase

import android.util.Log
import com.andrew264.habits.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Encapsulates the business logic for what to do after the user responds to permission requests.
 */
class HandlePermissionResultUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val startPresenceMonitoringUseCase: StartPresenceMonitoringUseCase
) {
    suspend fun execute(
        activityRecognitionGranted: Boolean,
        notificationsGranted: Boolean
    ) {
        if (activityRecognitionGranted) {
            // Check settings and restart service if it was supposed to be active
            if (settingsRepository.settingsFlow.first().isServiceActive) {
                Log.d(
                    "HandlePermissionResultUseCase",
                    "Activity permission granted and service should be active. Ensuring service (re)starts."
                )
                startPresenceMonitoringUseCase.execute()
            }
        }

        if (!notificationsGranted) {
            Log.d(
                "HandlePermissionResultUseCase",
                "Notification permission denied. Service notifications might not show or service may not run reliably."
            )
        }
    }
}