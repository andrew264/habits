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

    fun handleInitialServiceStart(activityRecognitionGranted: Boolean) {
        startService()
    }

    fun startService() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_START_SERVICE
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Toast.makeText(context, "Presence monitoring service started.", Toast.LENGTH_SHORT)
                .show()
            Log.d(
                TAG,
                "Service start command sent. Action: ${UserPresenceService.ACTION_START_SERVICE}"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Error starting service: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error starting service", e)
        }
    }

    fun stopService() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_STOP_SERVICE
        }
        try {
            context.startService(serviceIntent)
            Toast.makeText(context, "Presence monitoring service stopped.", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "Service stop command sent.")
        } catch (e: Exception) {
            Toast.makeText(context, "Error stopping service: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error stopping service", e)
        }
    }

    // --- Manual Schedule methods remain unchanged ---

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