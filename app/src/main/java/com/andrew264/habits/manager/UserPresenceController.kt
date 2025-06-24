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
        Log.d(TAG, "handleInitialServiceStart called. ActivityRecognitionGranted: $activityRecognitionGranted. Service will start based on persisted state.")
    }

    fun startService() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_START_SERVICE
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Toast.makeText(context, "Presence monitoring service start requested.", Toast.LENGTH_SHORT)
                .show()
            Log.d(
                TAG,
                "Service start command sent. Action: ${UserPresenceService.ACTION_START_SERVICE}"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Error requesting service start: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error requesting service start", e)
        }
    }

    fun stopService() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_STOP_SERVICE
        }
        try {
            context.startService(serviceIntent)
            Toast.makeText(context, "Presence monitoring service stop requested.", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "Service stop command sent.")
        } catch (e: Exception) {
            Toast.makeText(context, "Error requesting service stop: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error requesting service stop", e)
        }
    }

    // These methods now just send intents. The service will handle persisting the change.
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
            Log.d(TAG, "Manual bedtime set command sent to $hour:$minute")
        } catch (e: Exception) {
            Toast.makeText(context, "Error sending set manual bedtime: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Error sending set manual bedtime", e)
        }
    }

    fun clearManualBedtime() {
        val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
            action = UserPresenceService.ACTION_CLEAR_MANUAL_BEDTIME
        }
        try {
            context.startService(serviceIntent)
            Toast.makeText(context, "Clear manual bedtime command sent.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Clear manual bedtime command sent")
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error sending clear manual bedtime: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Error sending clear manual bedtime", e)
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
            Log.d(TAG, "Manual wake-up time set command sent to $hour:$minute")
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error sending set manual wake-up time: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Error sending set manual wake-up time", e)
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
            Log.d(TAG, "Clear manual wake-up time command sent")
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error sending clear manual wake-up time: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Error sending clear manual wake-up time", e)
        }
    }
}