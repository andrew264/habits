package com.andrew264.habits.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.repository.ScreenHistoryRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.usecase.EvaluateUserPresenceUseCase
import com.andrew264.habits.domain.usecase.PresenceEvaluationInput
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeviceStateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    @Inject
    lateinit var screenHistoryRepository: ScreenHistoryRepository

    @Inject
    lateinit var evaluateUserPresenceUseCase: EvaluateUserPresenceUseCase

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "DeviceStateReceiver"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val pendingResult = goAsync()
        val timestamp = System.currentTimeMillis()

        scope.launch {
            try {
                val settings = settingsRepository.settingsFlow.first()
                Log.d(
                    TAG,
                    "Received action: ${intent.action}. Bedtime enabled: ${settings.isBedtimeTrackingEnabled}, Usage enabled: ${settings.isAppUsageTrackingEnabled}"
                )

                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        if (settings.isBedtimeTrackingEnabled) {
                            evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.ScreenOn)
                            screenHistoryRepository.addScreenEvent("SCREEN_ON", timestamp)
                        }
                    }

                    Intent.ACTION_SCREEN_OFF -> {
                        if (settings.isAppUsageTrackingEnabled) {
                            appUsageRepository.endCurrentUsageSession(timestamp)
                        }
                        if (settings.isBedtimeTrackingEnabled) {
                            evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.ScreenOff)
                            screenHistoryRepository.addScreenEvent("SCREEN_OFF", timestamp)
                        }
                    }

                    Intent.ACTION_USER_PRESENT -> {
                        if (settings.isBedtimeTrackingEnabled) {
                            evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.UserPresent)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing device state change: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}