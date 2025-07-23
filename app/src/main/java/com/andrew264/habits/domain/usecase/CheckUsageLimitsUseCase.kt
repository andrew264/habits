package com.andrew264.habits.domain.usecase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.andrew264.habits.R
import com.andrew264.habits.data.dao.AppUsageEventDao
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WhitelistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUsageLimitsUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val whitelistRepository: WhitelistRepository,
    private val appUsageEventDao: AppUsageEventDao
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val packageManager = context.packageManager

    private val notifiedSessionPackages = mutableSetOf<String>()

    companion object {
        private const val TAG = "CheckUsageLimitsUseCase"
        private const val NOTIFICATION_CHANNEL_ID = "UsageLimitChannel"
    }

    suspend fun checkSessionLimit(packageName: String, sessionDurationMs: Long) {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.usageLimitNotificationsEnabled) return

        if (packageName in notifiedSessionPackages) return

        val app = whitelistRepository.getWhitelistedApps().first().find { it.packageName == packageName }
        val limitMinutes = app?.sessionLimitMinutes ?: return
        val limitMs = TimeUnit.MINUTES.toMillis(limitMinutes.toLong())

        if (sessionDurationMs >= limitMs) {
            Log.d(TAG, "Session limit exceeded for $packageName")
            sendNotification(
                packageName,
                "Session Limit Reached",
                "You've used ${getAppName(packageName)} for ${TimeUnit.MILLISECONDS.toMinutes(sessionDurationMs)} minutes this session."
            )
            notifiedSessionPackages.add(packageName)
        }
    }

    suspend fun checkDailyLimit(packageName: String) {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.usageLimitNotificationsEnabled) return

        val notifiedPackagesToday = settingsRepository.getNotifiedDailyPackages().first()
        if (packageName in notifiedPackagesToday) return

        val app = whitelistRepository.getWhitelistedApps().first().find { it.packageName == packageName }
        val limitMinutes = app?.dailyLimitMinutes ?: return
        val limitMs = TimeUnit.MINUTES.toMillis(limitMinutes.toLong())

        val todayStartMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = appUsageEventDao.getEventsInRange(todayStartMs, System.currentTimeMillis()).first()
        val usageTodayMs = events
            .filter { it.packageName == packageName }
            .sumOf { event ->
                val end = event.endTimestamp ?: System.currentTimeMillis()
                val start = event.startTimestamp
                if (end > start) end - start else 0
            }

        if (usageTodayMs >= limitMs) {
            Log.d(TAG, "Daily limit exceeded for $packageName")
            sendNotification(
                packageName,
                "Daily Limit Reached",
                "You've used ${getAppName(packageName)} for over $limitMinutes minutes today."
            )
            settingsRepository.addNotifiedDailyPackage(packageName)
        }
    }

    fun clearSessionNotified(packageName: String) {
        notifiedSessionPackages.remove(packageName)
        Log.d(TAG, "Cleared session notification state for $packageName")
    }

    private fun sendNotification(packageName: String, title: String, content: String) {
        createNotificationChannel()

        val notificationId = packageName.hashCode()

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hourglass_24)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Usage Limit Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for when app usage limits are reached"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun getAppName(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}