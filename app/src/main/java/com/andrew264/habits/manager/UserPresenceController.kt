package com.andrew264.habits.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.andrew264.habits.service.UserPresenceService

class UserPresenceController(private val context: Context) {

    companion object {
        private const val TAG = "UserPresenceController"
    }

    fun handleInitialServiceStart(activityRecognitionGranted: Boolean) {
        if (activityRecognitionGranted) {
            startServiceWithMode(
                UserPresenceService.ACTION_START_SERVICE_SLEEP_API,
                "Initial (AR granted)"
            )
        } else {
            Toast.makeText(
                context,
                "Activity Recognition permission denied for initial start. Falling back to heuristics.",
                Toast.LENGTH_LONG
            ).show()
            startServiceWithMode(
                UserPresenceService.ACTION_START_SERVICE_HEURISTICS,
                "Initial (AR denied, fallback)"
            )
        }
    }

    fun startServiceWithSleepApi(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startServiceWithMode(
                UserPresenceService.ACTION_START_SERVICE_SLEEP_API,
                "User requested Sleep API"
            )
            return true
        } else {
            Log.w(
                TAG,
                "Attempted to start with Sleep API, but Activity Recognition permission not granted."
            )
            return false
        }
    }

    fun startServiceWithHeuristics() {
        startServiceWithMode(
            UserPresenceService.ACTION_START_SERVICE_HEURISTICS,
            "User requested Heuristics"
        )
    }

    private fun startServiceWithMode(action: String, reason: String) {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            this.action = action
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            val modeName =
                if (action == UserPresenceService.ACTION_START_SERVICE_SLEEP_API) "Sleep API" else "Heuristics"
            Toast.makeText(
                context,
                "Service command sent: $modeName mode. ($reason)",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "Service start command sent. Action: $action, Reason: $reason")
        } catch (e: Exception) {
            Toast.makeText(context, "Error starting service: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error starting service with action $action", e)
        }
    }

    fun stopService() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_STOP_SERVICE
        }
        try {
            // Use startService for stop, as the service will handle stopping foreground if needed.
            context.startService(serviceIntent)
            Toast.makeText(context, "User Presence Service Stop command sent.", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "Service stop command sent.")
        } catch (e: Exception) {
            Toast.makeText(context, "Error stopping service: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error stopping service", e)
        }
    }

    fun setManualBedtime(hour: Int, minute: Int) {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_SET_MANUAL_BEDTIME
            putExtra(UserPresenceService.EXTRA_MANUAL_BEDTIME_HOUR, hour)
            putExtra(UserPresenceService.EXTRA_MANUAL_BEDTIME_MINUTE, minute)
        }
        try {
            context.startService(serviceIntent)
            Toast.makeText(
                context,
                "Manual bedtime ($hour:$minute) command sent.",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "Manual bedtime set to $hour:$minute")
        } catch (e: Exception) {
            Toast.makeText(context, "Error setting manual bedtime: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error setting manual bedtime", e)
        }
    }

    fun clearManualBedtime() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_CLEAR_MANUAL_BEDTIME
        }
        try {
            context.startService(serviceIntent)
            Toast.makeText(context, "Clear manual bedtime command sent.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Manual bedtime cleared")
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error clearing manual bedtime: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Error clearing manual bedtime", e)
        }
    }

    fun setManualWakeUpTime(hour: Int, minute: Int) {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_SET_MANUAL_WAKE_UP_TIME
            putExtra(UserPresenceService.EXTRA_MANUAL_WAKE_UP_HOUR, hour)
            putExtra(UserPresenceService.EXTRA_MANUAL_WAKE_UP_MINUTE, minute)
        }
        try {
            context.startService(serviceIntent)
            Toast.makeText(
                context,
                "Manual wake-up time ($hour:$minute) command sent.",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "Manual wake-up time set to $hour:$minute")
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error setting manual wake-up time: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Error setting manual wake-up time", e)
        }
    }

    fun clearManualWakeUpTime() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_CLEAR_MANUAL_WAKE_UP_TIME
        }
        try {
            context.startService(serviceIntent)
            Toast.makeText(context, "Clear manual wake-up time command sent.", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "Manual wake-up time cleared")
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error clearing manual wake-up time: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Error clearing manual wake-up time", e)
        }
    }
}