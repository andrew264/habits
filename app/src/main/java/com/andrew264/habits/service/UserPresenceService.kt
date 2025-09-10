package com.andrew264.habits.service

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.andrew264.habits.MainActivity
import com.andrew264.habits.R
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.repository.ScreenHistoryRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.domain.usecase.EvaluateUserPresenceUseCase
import com.andrew264.habits.domain.usecase.PresenceEvaluationInput
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.receiver.SleepReceiver
import com.andrew264.habits.ui.navigation.Bedtime
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import com.google.android.gms.location.SleepSegmentRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UserPresenceService : Service() {

    private var deviceStateReceiver: BroadcastReceiver? = null
    private var foregroundAppReceiver: BroadcastReceiver? = null

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
            // TODO: do we need to ignore all the launchers
        )

        serviceScope.launch {
            userPresenceHistoryRepository.userPresenceState.collect { state ->
                currentPresenceState = state
                val settings = settingsRepository.settingsFlow.first()
                if (settings.isBedtimeTrackingEnabled || settings.isAppUsageTrackingEnabled) {
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, createNotification(settings))
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
        // Always clean up first to handle re-configuration
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

        // Screen state is needed for both features
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
            createNotification(settings),
            serviceType
        )
    }

    private fun stopMonitoringAndSelf() {
        Log.i(TAG, "Stopping all monitoring.")
        unsubscribeFromSleepUpdates()
        unregisterDeviceStateReceiver()
        unregisterForegroundAppReceiver()
        userPresenceHistoryRepository.updateUserPresenceState(UserPresenceState.UNKNOWN)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(settings: PersistentSettings): Notification {
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