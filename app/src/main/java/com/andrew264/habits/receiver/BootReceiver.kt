package com.andrew264.habits.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.scheduler.WaterAlarmScheduler
import com.andrew264.habits.domain.usecase.StartPresenceMonitoringUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var waterAlarmScheduler: WaterAlarmScheduler

    @Inject
    lateinit var startPresenceMonitoringUseCase: StartPresenceMonitoringUseCase

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)


    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed.")

            val pendingResult = goAsync()

            scope.launch {
                try {
                    val settings = settingsRepository.settingsFlow.first()

                    // End any dangling session from before the reboot
                    appUsageRepository.endCurrentUsageSession(System.currentTimeMillis())

                    // Restart presence service if it was active
                    if (settings.isBedtimeTrackingEnabled) {
                        Log.d(TAG, "Service was persisted as active. Attempting to start UserPresenceService.")
                        startPresenceMonitoringUseCase.execute()
                    } else {
                        Log.d(TAG, "Service was persisted as inactive. Not starting on boot.")
                    }

                    // Reschedule water reminders if they were active
                    if (settings.isWaterTrackingEnabled && settings.isWaterReminderEnabled) {
                        Log.d(TAG, "Water reminders were active. Rescheduling first reminder.")
                        waterAlarmScheduler.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing boot tasks in BootReceiver: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}