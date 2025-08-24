package com.andrew264.habits.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppUsageAccessibilityService : AccessibilityService() {

    private var lastRecordedPackageName: CharSequence? = null

    companion object {
        private const val TAG = "AppUsageAccessService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName
        if (packageName == null || packageName == lastRecordedPackageName) {
            return
        }
        lastRecordedPackageName = packageName

        Log.d(TAG, "Foreground app changed to: $packageName. Broadcasting change.")
        val intent = Intent(UserPresenceService.ACTION_FOREGROUND_APP_CHANGED).apply {
            putExtra(UserPresenceService.EXTRA_PACKAGE_NAME, packageName.toString())
            `package` = this@AppUsageAccessibilityService.packageName
        }
        sendBroadcast(intent)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted.")
        val intent = Intent(UserPresenceService.ACTION_ACCESSIBILITY_INTERRUPTED).apply {
            `package` = this@AppUsageAccessibilityService.packageName
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed.")
    }
}