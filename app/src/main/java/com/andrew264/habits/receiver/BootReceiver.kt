package com.andrew264.habits.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.service.UserPresenceService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed. Attempting to start UserPresenceService.")

            val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
                action = UserPresenceService.ACTION_START_SERVICE
            }

            try {
                context.startForegroundService(serviceIntent)
                Log.i(TAG, "UserPresenceService start command sent on boot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting UserPresenceService on boot: ${e.message}", e)
            }
        }
    }
}