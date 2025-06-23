package com.andrew264.habits.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.andrew264.habits.service.UserPresenceService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed. Attempting to start UserPresenceService.")

            val preferredStartAction = if (hasActivityRecognitionPermission(context)) {
                UserPresenceService.ACTION_START_SERVICE_SLEEP_API
            } else {
                Log.w(TAG, "Activity Recognition permission not found. Suggesting Heuristics mode.")
                UserPresenceService.ACTION_START_SERVICE_HEURISTICS
            }


            val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
                action = preferredStartAction
            }

            try {
                context.startForegroundService(serviceIntent)
                Log.i(TAG, "UserPresenceService start command sent with action: $preferredStartAction")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting UserPresenceService on boot: ${e.message}", e)
            }
        }
    }

    private fun hasActivityRecognitionPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }
}