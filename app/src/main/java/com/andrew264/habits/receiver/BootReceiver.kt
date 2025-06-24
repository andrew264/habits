package com.andrew264.habits.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.data.repository.SettingsRepository
import com.andrew264.habits.service.UserPresenceService
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

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed.")

            val pendingResult = goAsync()

            scope.launch {
                try {
                    val settings = settingsRepository.settingsFlow.first()
                    if (settings.isServiceActive) {
                        Log.d(TAG, "Service was persisted as active. Attempting to start UserPresenceService.")
                        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
                            action = UserPresenceService.ACTION_START_SERVICE
                        }
                        try {
                            context.startForegroundService(serviceIntent)
                            Log.i(TAG, "UserPresenceService start command sent on boot.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error starting UserPresenceService on boot: ${e.message}", e)
                        }
                    } else {
                        Log.d(TAG, "Service was persisted as inactive. Not starting on boot.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading settings in BootReceiver: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}