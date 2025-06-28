package com.andrew264.habits.manager

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.andrew264.habits.service.UserPresenceService

class UserPresenceController(private val context: Context) {

    companion object {
        private const val TAG = "UserPresenceController"
    }

    fun startService() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_START_SERVICE
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Toast.makeText(context, "Presence monitoring service start requested.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Service start command sent. Action: ${UserPresenceService.ACTION_START_SERVICE}")
        } catch (e: Exception) {
            Toast.makeText(context, "Error requesting service start: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error requesting service start", e)
        }
    }

    fun stopService() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_STOP_SERVICE
        }
        try {
            context.startService(serviceIntent)
            Toast.makeText(context, "Presence monitoring service stop requested.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Service stop command sent.")
        } catch (e: Exception) {
            Toast.makeText(context, "Error requesting service stop: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error requesting service stop", e)
        }
    }
}