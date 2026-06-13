package com.andrew264.habits.service

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.domain.usecase.EvaluateUserPresenceUseCase
import com.andrew264.habits.domain.usecase.PresenceEvaluationInput
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.receiver.SessionLimitReceiver
import com.andrew264.habits.receiver.SleepReceiver
import com.andrew264.habits.ui.navigation.Bedtime
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import com.google.android.gms.location.SleepSegmentRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class UserPresenceService : Service() {

    private var deviceStateReceiver: BroadcastReceiver? = null
    private var foregroundAppReceiver: BroadcastReceiver? = null
    private var tickerJob: Job? = null

    companion object {
        private const val TAG = "UserPresenceService"
        private const val NOTIFICATION_CHANNEL_ID = "UserPresenceServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SLEEP_API_PENDING_INTENT_REQUEST_CODE = 1001

        const val ACTION_START_SERVICE = "com.andrew264.habits.action.START_PRESENCE_SERVICE"
        const val ACTION_STOP_SERVICE = "com.andrew264.habits.action.STOP_PRESENCE_SERVICE"
        const val ACTION_PROCESS_SLEEP_SEGMENT_EVENTS = "com.andrew264.habits.action.PROCESS_SLEEP_SEGMENT_EVENTS"
        const val ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS = "com.andrew264.habits.action.PROCESS_SLEEP_CLASSIFY_EVENTS"
        const val ACTION_FOREGROUND_APP_CHANGED = "com.andrew264.habits.action.FOREGROUND_APP_CHANGED"
        const val ACTION_ACCESSIBILITY_INTERRUPTED = "com.andrew264.habits.action.ACCESSIBILITY_INTERRUPTED"

        const val EXTRA_SLEEP_SEGMENTS = "com.andrew264.habits.extra.SLEEP_SEGMENTS"
        const val EXTRA_SLEEP_CLASSIFY_EVENTS = "com.andrew264.habits.extra.SLEEP_CLASSIFY_EVENTS"
        const val EXTRA_PACKAGE_NAME = "com.andrew264.habits.extra.PACKAGE_NAME"
    }

    @Inject
    lateinit var userPresenceHistoryRepository: UserPresenceHistoryRepository

    @Inject
    lateinit var evaluateUserPresenceUseCase: EvaluateUserPresenceUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    @Inject
    lateinit var screenHistoryRepository: ScreenHistoryRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private val activityRecognitionClient by lazy { ActivityRecognition.getClient(this) }
    private var sleepApiPendingIntent: PendingIntent? = null

    private var currentPresenceState = UserPresenceState.UNKNOWN
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
                userPresenceHistoryRepository.userPresenceState,
                settingsRepository.settingsFlow,
                appUsageRepository.activeSessionFlow
            ) { state, settings, activeSession ->
                Triple(state, settings, activeSession)
            }.collect { (state, settings, activeSession) ->
                currentPresenceState = state
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                tickerJob?.cancel()

                if (activeSession != null && settings.isAppUsageTrackingEnabled) {
                    val limitMillis = TimeUnit.MINUTES.toMillis(activeSession.sessionLimitMinutes.toLong())
                    val updateIntervalMillis = (limitMillis * 0.05).toLong().coerceAtLeast(1000L)

                    tickerJob = launch {
                        while (isActive) {
                            updateProgressNotification(notificationManager, activeSession, settings)
                            delay(updateIntervalMillis)
                        }
                    }
                } else if (settings.isBedtimeTrackingEnabled || settings.isAppUsageTrackingEnabled) {
                    notificationManager.notify(NOTIFICATION_ID, createDefaultNotification(settings))
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

            ACTION_PROCESS_SLEEP_SEGMENT_EVENTS -> {
                val events: ArrayList<SleepSegmentEvent>? =
                    intent.getParcelableArrayListExtra(EXTRA_SLEEP_SEGMENTS, SleepSegmentEvent::class.java)
                serviceScope.launch {
                    events?.forEach { event -> evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.SleepApiSegment(event)) }
                }
            }

            ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS -> {
                val events: ArrayList<SleepClassifyEvent>? =
                    intent.getParcelableArrayListExtra(EXTRA_SLEEP_CLASSIFY_EVENTS, SleepClassifyEvent::class.java)
                serviceScope.launch {
                    events?.forEach { event -> evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.SleepApiClassify(event)) }
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
        unsubscribeFromSleepUpdates()
        unregisterDeviceStateReceiver()
        unregisterForegroundAppReceiver()

        val settings = settingsRepository.settingsFlow.first()
        if (!settings.isBedtimeTrackingEnabled && !settings.isAppUsageTrackingEnabled) {
            Log.i(TAG, "No monitoring features enabled. Stopping service.")
            stopMonitoringAndSelf()
            return
        }

        Log.i(TAG, "Configuring monitoring. Bedtime: ${settings.isBedtimeTrackingEnabled}, Usage: ${settings.isAppUsageTrackingEnabled}")

        if (settings.isBedtimeTrackingEnabled) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                subscribeToSleepUpdates()
            }
            evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.InitialEvaluation)
        }

        registerDeviceStateReceiver()
        if (settings.isAppUsageTrackingEnabled) {
            registerForegroundAppReceiver()
        }

        val serviceType = if (settings.isBedtimeTrackingEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createDefaultNotification(settings),
            serviceType
        )
    }

    private fun stopMonitoringAndSelf() {
        Log.i(TAG, "Stopping all monitoring.")
        tickerJob?.cancel()
        unsubscribeFromSleepUpdates()
        unregisterDeviceStateReceiver()
        unregisterForegroundAppReceiver()
        userPresenceHistoryRepository.updateUserPresenceState(UserPresenceState.UNKNOWN)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createDefaultNotification(settings: PersistentSettings): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("destination_route", Bedtime::class.java.simpleName)
        }
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(this, Bedtime::class.java.simpleName.hashCode(), notificationIntent, pendingIntentFlags)

        val contentText = when {
            settings.isBedtimeTrackingEnabled && settings.isAppUsageTrackingEnabled -> "Monitoring bedtime and app usage."
            settings.isBedtimeTrackingEnabled -> "Monitoring bedtime. Status: ${currentPresenceState.name.lowercase()}."
            settings.isAppUsageTrackingEnabled -> "Monitoring app usage."
            else -> "Habits service is running."
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Habit Tracker")
            .setContentText(contentText)
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

        if (Build.VERSION.SDK_INT >= 37) {
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

        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("$appName Session")
            .setContentText("Time remaining")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(effectiveEndTime)
            .setStyle(progressStyle)
            .addAction(snoozeAction)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun subscribeToSleepUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Subscribing to Sleep API updates.")
            sleepApiPendingIntent = getSleepPendingIntent()
            activityRecognitionClient.requestSleepSegmentUpdates(
                sleepApiPendingIntent!!,
                SleepSegmentRequest.getDefaultSleepSegmentRequest()
            ).addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed to Sleep API.")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to Sleep API.", e)
            }
        } else {
            Log.w(TAG, "Attempted to subscribe to Sleep API, but permission is missing.")
        }
    }

    private fun unsubscribeFromSleepUpdates() {
        sleepApiPendingIntent?.let { pendingIntent ->
            Log.d(TAG, "Attempting to unsubscribe from Sleep API updates.")
            activityRecognitionClient.removeSleepSegmentUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully unsubscribed from Sleep API updates.")
                    pendingIntent.cancel()
                    sleepApiPendingIntent = null
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unsubscribe from Sleep API updates.", e)
                }
        }
    }

    private fun getSleepPendingIntent(): PendingIntent {
        val intent = Intent(this, SleepReceiver::class.java)
        intent.action = SleepReceiver.ACTION_PROCESS_SLEEP_EVENTS
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(this, SLEEP_API_PENDING_INTENT_REQUEST_CODE, intent, flags)
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
                        Log.d(TAG, "Received action: ${intent.action}. Bedtime enabled: ${settings.isBedtimeTrackingEnabled}, Usage enabled: ${settings.isAppUsageTrackingEnabled}")

                        when (intent.action) {
                            Intent.ACTION_SCREEN_ON -> {
                                isScreenOn = true
                                if (settings.isBedtimeTrackingEnabled) {
                                    evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.ScreenOn)
                                }
                                if (settings.isBedtimeTrackingEnabled || settings.isAppUsageTrackingEnabled) {
                                    screenHistoryRepository.addScreenEvent("SCREEN_ON", timestamp)
                                }
                            }

                            Intent.ACTION_SCREEN_OFF -> {
                                isScreenOn = false
                                lastStartedPackageName = null
                                if (settings.isAppUsageTrackingEnabled) {
                                    appUsageRepository.endCurrentUsageSession(timestamp)
                                }
                                if (settings.isBedtimeTrackingEnabled) {
                                    evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.ScreenOff)
                                }
                                if (settings.isBedtimeTrackingEnabled || settings.isAppUsageTrackingEnabled) {
                                    screenHistoryRepository.addScreenEvent("SCREEN_OFF", timestamp)
                                }
                            }

                            Intent.ACTION_USER_PRESENT -> {
                                if (settings.isBedtimeTrackingEnabled) {
                                    evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.UserPresent)
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
            addAction(Intent.ACTION_USER_PRESENT)
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
        val serviceChannel =
            NotificationChannel(NOTIFICATION_CHANNEL_ID, "Monitoring Service", NotificationManager.IMPORTANCE_LOW)
        serviceChannel.description = "Channel for habit tracker monitoring service"
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
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
                Log.d(TAG, "Foreground app receiver already unregistered.")
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
