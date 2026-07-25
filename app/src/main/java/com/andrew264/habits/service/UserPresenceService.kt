package com.andrew264.habits.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.andrew264.habits.MainActivity
import com.andrew264.habits.R
import com.andrew264.habits.domain.model.ActiveSessionState
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.repository.ScreenHistoryRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.receiver.SessionLimitReceiver
import com.andrew264.habits.ui.navigation.Usage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class UserPresenceService : Service() {

    private var deviceStateReceiver: BroadcastReceiver? = null
    private var foregroundAppReceiver: BroadcastReceiver? = null
    private var tickerJob: Job? = null

    companion object {
        private const val TAG = "UserPresenceService"
        private const val NOTIFICATION_CHANNEL_ID = "UserPresenceServiceChannel"
        private const val NOTIFICATION_ID = 1

        private const val SESSION_PROGRESS_CHANNEL_ID = "SessionProgressChannel"
        private const val SESSION_PROGRESS_NOTIFICATION_ID = 3

        const val ACTION_START_SERVICE = "com.andrew264.habits.action.START_PRESENCE_SERVICE"
        const val ACTION_STOP_SERVICE = "com.andrew264.habits.action.STOP_PRESENCE_SERVICE"
        const val ACTION_FOREGROUND_APP_CHANGED = "com.andrew264.habits.action.FOREGROUND_APP_CHANGED"
        const val ACTION_ACCESSIBILITY_INTERRUPTED = "com.andrew264.habits.action.ACCESSIBILITY_INTERRUPTED"

        const val EXTRA_PACKAGE_NAME = "com.andrew264.habits.extra.PACKAGE_NAME"
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    @Inject
    lateinit var screenHistoryRepository: ScreenHistoryRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var isScreenOn: Boolean = true
    private lateinit var ignoredPackages: Set<String>
    private var lastStartedPackageName: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        createNotificationChannel()

        ignoredPackages = setOf(
            "com.android.systemui",
            this.packageName
        )

        serviceScope.launch {
            combine(
                settingsRepository.settingsFlow,
                appUsageRepository.activeSessionFlow
            ) { settings, activeSession ->
                Pair(settings, activeSession)
            }.collect { (settings, activeSession) ->
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                if (settings.isAppUsageTrackingEnabled) {
                    notificationManager.notify(NOTIFICATION_ID, createDefaultNotification())
                }
                tickerJob?.cancel()

                if (activeSession != null && settings.isAppUsageTrackingEnabled) {
                    val limitMillis = TimeUnit.MINUTES.toMillis(activeSession.sessionLimitMinutes.toLong())
                    val updateIntervalMillis = (limitMillis * 0.05).toLong().coerceAtLeast(1000L)

                    tickerJob = launch {
                        while (isActive) {
                            updateProgressNotification(notificationManager, activeSession, settings)
                            delay(updateIntervalMillis.milliseconds)
                        }
                    }
                } else {
                    notificationManager.cancel(SESSION_PROGRESS_NOTIFICATION_ID)
                }
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d(TAG, "onStartCommand with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                serviceScope.launch {
                    configureAndStartMonitoring()
                }
            }

            ACTION_STOP_SERVICE -> {
                serviceScope.launch {
                    stopMonitoringAndSelf()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun configureAndStartMonitoring() {
        unregisterDeviceStateReceiver()
        unregisterForegroundAppReceiver()

        val settings = settingsRepository.settingsFlow.first()
        if (!settings.isAppUsageTrackingEnabled) {
            Log.i(TAG, "App usage tracking is disabled. Stopping service.")
            stopMonitoringAndSelf()
            return
        }

        Log.i(TAG, "Configuring monitoring for App Usage.")

        registerDeviceStateReceiver()
        registerForegroundAppReceiver()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createDefaultNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun stopMonitoringAndSelf() {
        Log.i(TAG, "Stopping all monitoring.")
        tickerJob?.cancel()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(SESSION_PROGRESS_NOTIFICATION_ID)

        unregisterDeviceStateReceiver()
        unregisterForegroundAppReceiver()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createDefaultNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("destination_route", Usage::class.java.simpleName)
        }
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(this, Usage::class.java.simpleName.hashCode(), notificationIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Habit Tracker")
            .setContentText("Monitoring app usage.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateProgressNotification(
        notificationManager: NotificationManager,
        session: ActiveSessionState,
        settings: PersistentSettings
    ) {
        if (session.sessionLimitMinutes <= 0) return

        val originalLimitMillis = TimeUnit.MINUTES.toMillis(session.sessionLimitMinutes.toLong())
        val snoozeEndTime = settings.sessionSnoozeTimestamps[session.packageName] ?: 0L

        val effectiveEndTime = maxOf(session.startTimestamp + originalLimitMillis, snoozeEndTime)
        val effectiveLimitMillis = effectiveEndTime - session.startTimestamp

        val elapsedMillis = System.currentTimeMillis() - session.startTimestamp
        val progressPercent = ((elapsedMillis.toFloat() / effectiveLimitMillis.toFloat()) * 100).toInt().coerceIn(0, 100)

        val safeSegment = Notification.ProgressStyle.Segment(75)
        val cautionSegment = Notification.ProgressStyle.Segment(15)
        val dangerSegment = Notification.ProgressStyle.Segment(10)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            safeSegment.semanticStyle = Notification.SEMANTIC_STYLE_SAFE
            cautionSegment.semanticStyle = Notification.SEMANTIC_STYLE_CAUTION
            dangerSegment.semanticStyle = Notification.SEMANTIC_STYLE_DANGER
        }

        var appIcon: android.graphics.drawable.Icon? = null
        var appName = session.packageName
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(session.packageName, 0)
            appName = pm.getApplicationLabel(appInfo).toString()
            if (appInfo.icon != 0) {
                appIcon = android.graphics.drawable.Icon.createWithResource(session.packageName, appInfo.icon)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load icon/label for progress notification", e)
        }

        val progressStyle = Notification.ProgressStyle()
            .setProgress(progressPercent)
            .setProgressSegments(listOf(safeSegment, cautionSegment, dangerSegment))
            .setStyledByProgress(true)

        if (appIcon != null) {
            progressStyle.progressTrackerIcon = appIcon
        }

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("destination_route", "Usage/${session.packageName}")
        }
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, session.packageName.hashCode(), notificationIntent, pendingIntentFlags)

        val snoozeIntent = Intent(this, SessionLimitReceiver::class.java).apply {
            action = SessionLimitReceiver.ACTION_SNOOZE_SESSION
            putExtra(SessionLimitReceiver.EXTRA_PACKAGE_NAME, session.packageName)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            session.packageName.hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeAction = Notification.Action.Builder(
            null,
            "Snooze 5 Min",
            snoozePendingIntent
        ).build()

        val builder = Notification.Builder(this, SESSION_PROGRESS_CHANNEL_ID)
            .setContentTitle("$appName Session")
            .setContentText("Time remaining")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setWhen(effectiveEndTime)
            .setStyle(progressStyle)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            .addAction(snoozeAction)

        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            builder.setRequestPromotedOngoing(true)
        }
        notificationManager.notify(SESSION_PROGRESS_NOTIFICATION_ID, builder.build())
    }

    private fun registerDeviceStateReceiver() {
        if (deviceStateReceiver != null) return

        deviceStateReceiver = object : BroadcastReceiver() {
            private val job = SupervisorJob()
            private val scope = CoroutineScope(Dispatchers.IO + job)

            override fun onReceive(context: Context, intent: Intent) {
                val pendingResult = goAsync()
                val timestamp = System.currentTimeMillis()

                scope.launch {
                    try {
                        val settings = settingsRepository.settingsFlow.first()

                        when (intent.action) {
                            Intent.ACTION_SCREEN_ON -> {
                                isScreenOn = true
                                if (settings.isAppUsageTrackingEnabled) {
                                    screenHistoryRepository.addScreenEvent("SCREEN_ON", timestamp)
                                }
                            }

                            Intent.ACTION_SCREEN_OFF -> {
                                isScreenOn = false
                                lastStartedPackageName = null
                                if (settings.isAppUsageTrackingEnabled) {
                                    appUsageRepository.endCurrentUsageSession(timestamp)
                                    screenHistoryRepository.addScreenEvent("SCREEN_OFF", timestamp)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing device state change: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(deviceStateReceiver, filter)
        Log.d(TAG, "Device state receiver registered.")
    }

    private fun unregisterDeviceStateReceiver() {
        deviceStateReceiver?.let {
            try {
                unregisterReceiver(it)
                deviceStateReceiver = null
                Log.d(TAG, "Device state receiver unregistered.")
            } catch (_: IllegalArgumentException) {
                Log.w(TAG, "Device state receiver already unregistered.")
            }
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Monitoring Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for habit tracker monitoring service"
        }
        manager?.createNotificationChannel(serviceChannel)

        val progressChannel = NotificationChannel(
            SESSION_PROGRESS_CHANNEL_ID,
            "App Usage Progress",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Live progress for active app sessions"
            setSound(null, null)
            enableVibration(true)
        }
        manager?.createNotificationChannel(progressChannel)
    }

    private fun registerForegroundAppReceiver() {
        if (foregroundAppReceiver != null) return

        foregroundAppReceiver = object : BroadcastReceiver() {
            private val job = SupervisorJob()
            private val scope = CoroutineScope(Dispatchers.IO + job)

            override fun onReceive(context: Context, intent: Intent) {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        when (intent.action) {
                            ACTION_FOREGROUND_APP_CHANGED -> {
                                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return@launch
                                handleForegroundAppChange(packageName)
                            }

                            ACTION_ACCESSIBILITY_INTERRUPTED -> {
                                appUsageRepository.endCurrentUsageSession(System.currentTimeMillis())
                                lastStartedPackageName = null
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_FOREGROUND_APP_CHANGED)
            addAction(ACTION_ACCESSIBILITY_INTERRUPTED)
        }
        ContextCompat.registerReceiver(this, foregroundAppReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "Foreground app receiver registered.")
    }

    private fun unregisterForegroundAppReceiver() {
        foregroundAppReceiver?.let {
            try {
                unregisterReceiver(it)
                foregroundAppReceiver = null
                Log.d(TAG, "Foreground app receiver unregistered.")
            } catch (_: IllegalArgumentException) {
                Log.w(TAG, "Foreground app receiver already unregistered.")
            }
        }
    }

    private suspend fun handleForegroundAppChange(packageName: String) {
        if (!isScreenOn) {
            Log.d(TAG, "Screen is off, ignoring foreground app change to $packageName")
            return
        }

        if (packageName in ignoredPackages) {
            Log.d(TAG, "Ignoring foreground app change to ignored package: $packageName. Session continues.")
            return
        }

        if (packageName == lastStartedPackageName) {
            Log.d(TAG, "Foreground app is the same as the last started one ($packageName). No action needed.")
            return
        }

        Log.d(TAG, "Starting new session for valid app: $packageName")
        lastStartedPackageName = packageName
        val settings = settingsRepository.settingsFlow.first()
        if (settings.isAppUsageTrackingEnabled) {
            appUsageRepository.startUsageSession(packageName, System.currentTimeMillis())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service Destroying. Cleaning up all resources.")
        serviceScope.launch {
            stopMonitoringAndSelf()
        }
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
