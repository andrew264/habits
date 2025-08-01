package com.andrew264.habits.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppUsageAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    private var lastRecordedPackageName: CharSequence? = null

    companion object {
        private const val TAG = "AppUsageAccessService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName

            // Ignore system UI and our own app
            if (packageName == "com.android.systemui" || packageName == applicationContext.packageName) {
                Log.d(TAG, "Ignoring foreground change to system UI or self: $packageName")
                return
            }

            if (packageName != null && packageName != lastRecordedPackageName) {
                Log.d(TAG, "Foreground app changed to: $packageName")
                lastRecordedPackageName = packageName
                serviceScope.launch {
                    val settings = settingsRepository.settingsFlow.first()
                    if (settings.isAppUsageTrackingEnabled) {
                        appUsageRepository.startUsageSession(
                            packageName.toString(),
                            System.currentTimeMillis()
                        )
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted.")
        serviceScope.launch {
            appUsageRepository.endCurrentUsageSession(System.currentTimeMillis())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed.")
        job.cancel()
    }
}