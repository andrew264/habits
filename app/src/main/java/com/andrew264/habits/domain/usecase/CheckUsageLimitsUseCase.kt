package com.andrew264.habits.domain.usecase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.andrew264.habits.R
import com.andrew264.habits.data.dao.AppUsageEventDao
import com.andrew264.habits.domain.manager.SnoozeManager
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WhitelistRepository
import com.andrew264.habits.ui.blocker.BlockerActivity
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
    private val appUsageEventDao: AppUsageEventDao,
    private val snoozeManager: SnoozeManager
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val packageManager = context.packageManager

    companion object {
        private const val TAG = "CheckUsageLimitsUseCase"
        private const val NOTIFICATION_CHANNEL_ID = "UsageLimitChannel"
        const val EXTRA_PACKAGE_NAME = "com.andrew264.habits.extra.PACKAGE_NAME"
        const val EXTRA_LIMIT_TYPE = "com.andrew264.habits.extra.LIMIT_TYPE"
        const val EXTRA_TIME_USED_MS = "com.andrew264.habits.extra.TIME_USED_MS"
        const val EXTRA_LIMIT_MINUTES = "com.andrew264.habits.extra.LIMIT_MINUTES"
    }

    suspend fun checkSessionLimitFromAlarm(packageName: String) {
        if (snoozeManager.isAppSnoozed(packageName)) {
            Log.d(TAG, "Session limit check for $packageName skipped, app is snoozed.")
            return
        }

        val settings = settingsRepository.settingsFlow.first()
        if (!settings.isAppUsageTrackingEnabled) return

        val ongoingEvent = appUsageEventDao.getOngoingEvent()
        if (ongoingEvent?.packageName != packageName) {
            Log.w(TAG, "Session limit alarm fired for $packageName, but it's no longer the foreground app. Ignoring.")
            return
        }

        val app = whitelistRepository.getWhitelistedApps().first().find { it.packageName == packageName }
        val limitMinutes = app?.sessionLimitMinutes ?: return
        val sessionDurationMs = System.currentTimeMillis() - ongoingEvent.startTimestamp

        Log.d(TAG, "Session limit alarm check for $packageName. Duration: ${sessionDurationMs}ms, Limit: ${limitMinutes}min")

        if (settings.isAppBlockingEnabled) {
            launchBlocker(
                packageName = packageName,
                limitType = "session",
                timeUsedMs = sessionDurationMs,
                limitMinutes = limitMinutes
            )
        } else if (settings.usageLimitNotificationsEnabled) {
            sendNotification(
                packageName,
                "Session Limit Reached",
                "You've used ${getAppName(packageName)} for ${TimeUnit.MILLISECONDS.toMinutes(sessionDurationMs)} minutes."
            )
        }
    }

    suspend fun checkDailyLimit(packageName: String) {
        if (snoozeManager.isAppSnoozed(packageName)) {
            Log.d(TAG, "Daily limit check for $packageName skipped, app is snoozed.")
            return
        }

        val settings = settingsRepository.settingsFlow.first()
        if (!settings.isAppUsageTrackingEnabled) return

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
            settingsRepository.addNotifiedDailyPackage(packageName)

            if (settings.isAppBlockingEnabled) {
                launchBlocker(
                    packageName = packageName,
                    limitType = "daily",
                    timeUsedMs = usageTodayMs,
                    limitMinutes = limitMinutes
                )
            } else if (settings.usageLimitNotificationsEnabled) {
                sendNotification(
                    packageName,
                    "Daily Limit Reached",
                    "You've used ${getAppName(packageName)} for over $limitMinutes minutes today."
                )
            }
        }
    }

    private fun launchBlocker(packageName: String, limitType: String, timeUsedMs: Long, limitMinutes: Int) {
        val intent = Intent(context, BlockerActivity::class.java).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_LIMIT_TYPE, limitType)
            putExtra(EXTRA_TIME_USED_MS, timeUsedMs)
            putExtra(EXTRA_LIMIT_MINUTES, limitMinutes)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
        Log.d(TAG, "Launched BlockerActivity for $packageName")
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