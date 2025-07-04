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
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.domain.usecase.EvaluateUserPresenceUseCase
import com.andrew264.habits.domain.usecase.PresenceEvaluationInput
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.receiver.SleepReceiver
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

    private var isSleepApiAvailable = false

    companion object {
        private const val TAG = "UserPresenceService"
        private const val NOTIFICATION_CHANNEL_ID = "UserPresenceServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SLEEP_API_PENDING_INTENT_REQUEST_CODE = 1001

        const val ACTION_START_SERVICE = "com.andrew264.habits.action.START_PRESENCE_SERVICE"
        const val ACTION_STOP_SERVICE = "com.andrew264.habits.action.STOP_PRESENCE_SERVICE"
        const val ACTION_PROCESS_SLEEP_SEGMENT_EVENTS = "com.andrew264.habits.action.PROCESS_SLEEP_SEGMENT_EVENTS"
        const val ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS = "com.andrew264.habits.action.PROCESS_SLEEP_CLASSIFY_EVENTS"

        const val EXTRA_SLEEP_SEGMENTS = "com.andrew264.habits.extra.SLEEP_SEGMENTS"
        const val EXTRA_SLEEP_CLASSIFY_EVENTS = "com.andrew264.habits.extra.SLEEP_CLASSIFY_EVENTS"
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userPresenceHistoryRepository: UserPresenceHistoryRepository

    @Inject
    lateinit var evaluateUserPresenceUseCase: EvaluateUserPresenceUseCase

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val activityRecognitionClient by lazy { ActivityRecognition.getClient(this) }
    private var sleepApiPendingIntent: PendingIntent? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            serviceScope.launch {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.ScreenOn)
                    Intent.ACTION_SCREEN_OFF -> evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.ScreenOff)
                    Intent.ACTION_USER_PRESENT -> evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.UserPresent)
                }
            }
        }
    }

    private var isReceiverRegistered = false
    private var _isServiceActuallyRunning = false

    private var currentPresenceState = UserPresenceState.UNKNOWN

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        createNotificationChannel()

        serviceScope.launch {
            userPresenceHistoryRepository.userPresenceState.collect { state ->
                currentPresenceState = state
                updateNotificationContentOnly()
            }
        }

        serviceScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                Log.d(
                    TAG,
                    "Settings loaded/changed: Active=${settings.isServiceActive}, ScheduleId=${settings.selectedScheduleId}"
                )
                if (settings.isServiceActive && !_isServiceActuallyRunning) {
                    Log.d(TAG, "Service persisted as active, and not running. Starting monitoring logic.")
                    startAllMonitoringLogic()
                } else if (!settings.isServiceActive && _isServiceActuallyRunning) {
                    Log.d(TAG, "Service persisted as inactive, but monitoring logic is running. Stopping.")
                    stopAllMonitoringLogic()
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

        serviceScope.launch {
            when (intent?.action) {
                ACTION_START_SERVICE -> {
                    if (!settingsRepository.settingsFlow.first().isServiceActive) {
                        Log.i(TAG, "ACTION_START_SERVICE: Persisting active state.")
                        settingsRepository.updateServiceActiveState(true)
                    } else if (!_isServiceActuallyRunning) {
                        Log.i(TAG, "ACTION_START_SERVICE: Already persisted as active, but not running. Starting logic.")
                        startAllMonitoringLogic()
                    } else {
                        Log.d(TAG, "ACTION_START_SERVICE: Service already active and running.")
                    }
                }

                ACTION_PROCESS_SLEEP_SEGMENT_EVENTS -> {
                    val events: ArrayList<SleepSegmentEvent>? = intent.getParcelableArrayListExtra(EXTRA_SLEEP_SEGMENTS, SleepSegmentEvent::class.java)
                    events?.forEach { event -> evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.SleepApiSegment(event)) }
                }

                ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS -> {
                    val events: ArrayList<SleepClassifyEvent>? = intent.getParcelableArrayListExtra(EXTRA_SLEEP_CLASSIFY_EVENTS, SleepClassifyEvent::class.java)
                    events?.forEach { event -> evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.SleepApiClassify(event)) }
                }

                ACTION_STOP_SERVICE -> {
                    Log.d(TAG, "ACTION_STOP_SERVICE received. Persisting inactive state.")
                    settingsRepository.updateServiceActiveState(false)
                }
            }
        }
        return if (intent?.action == ACTION_STOP_SERVICE && !_isServiceActuallyRunning) START_NOT_STICKY else START_STICKY
    }

    private fun startAllMonitoringLogic() {
        if (_isServiceActuallyRunning) {
            Log.d(TAG, "Start logic called, but service monitoring is already active.")
            return
        }
        Log.i(TAG, "Starting all monitoring service logic.")
        _isServiceActuallyRunning = true

        isSleepApiAvailable = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Sleep API available: $isSleepApiAvailable")

        if (isSleepApiAvailable) {
            subscribeToSleepUpdates()
        }
        registerScreenStateReceiver()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        )

        // Perform initial evaluation
        serviceScope.launch {
            evaluateUserPresenceUseCase.execute(PresenceEvaluationInput.InitialEvaluation)
        }
    }

    private fun stopAllMonitoringLogic() {
        if (!_isServiceActuallyRunning) {
            Log.d(TAG, "Stop logic called, but service monitoring is not active.")
            if (userPresenceHistoryRepository.userPresenceState.value != UserPresenceState.UNKNOWN) {
                updateUserPresenceStateInternal(UserPresenceState.UNKNOWN, "Service Monitoring Stopped")
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        Log.i(TAG, "Stopping all monitoring service logic.")
        _isServiceActuallyRunning = false

        if (isSleepApiAvailable) {
            unsubscribeFromSleepUpdates()
        }
        unregisterScreenStateReceiver()
        updateUserPresenceStateInternal(UserPresenceState.UNKNOWN, "Service Monitoring Stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateUserPresenceStateInternal(
        newState: UserPresenceState,
        reason: String
    ) {
        val oldState = currentPresenceState
        if (oldState != newState) {
            userPresenceHistoryRepository.updateUserPresenceState(newState)
            Log.i(TAG, "STATE CHANGE: User presence -> $newState (Reason: $reason)")
        }
    }

    private fun updateNotificationContentOnly() {
        if (_isServiceActuallyRunning) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }


    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val currentPresenceText = currentPresenceState.name.replace('_', ' ')
        val baseText = if (isSleepApiAvailable) "Sleep API & Schedule" else "Schedule Only"
        val contentText = "$baseText Active. State: $currentPresenceText"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Habit Tracker")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun registerScreenStateReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(this, screenStateReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
            isReceiverRegistered = true
            Log.d(TAG, "Screen state receiver registered.")
        }
    }

    private fun unregisterScreenStateReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenStateReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Screen state receiver unregistered.")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver not registered or already unregistered: ${e.message}")
            }
        }
    }


    private fun subscribeToSleepUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Subscribing to Sleep API updates.")
            sleepApiPendingIntent = getSleepPendingIntent()
            activityRecognitionClient.requestSleepSegmentUpdates(
                sleepApiPendingIntent!!,
                SleepSegmentRequest.getDefaultSleepSegmentRequest()
            ).addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed to Sleep API.")
                isSleepApiAvailable = true
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to Sleep API.", e)
                isSleepApiAvailable = false
            }
        } else {
            Log.w(TAG, "Attempted to subscribe to Sleep API, but permission is missing.")
            isSleepApiAvailable = false
        }
    }

    private fun unsubscribeFromSleepUpdates() {
        Log.d(TAG, "Attempting to unsubscribe from Sleep API updates.")
        sleepApiPendingIntent?.let { pendingIntent ->
            activityRecognitionClient.removeSleepSegmentUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully unsubscribed from Sleep API updates.")
                    pendingIntent.cancel()
                    sleepApiPendingIntent = null
                    isSleepApiAvailable = false
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unsubscribe from Sleep API updates.", e)
                }
        } ?: Log.d(TAG, "No active Sleep API subscription (PendingIntent was null).")
    }

    private fun getSleepPendingIntent(): PendingIntent {
        val intent = Intent(this, SleepReceiver::class.java)
        intent.action = SleepReceiver.ACTION_PROCESS_SLEEP_EVENTS
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(this, SLEEP_API_PENDING_INTENT_REQUEST_CODE, intent, flags)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "User Presence Service Channel", NotificationManager.IMPORTANCE_LOW)
        serviceChannel.description = "Channel for habit tracker presence monitoring service"
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service Destroying. Cleaning up all resources.")
        if (_isServiceActuallyRunning) {
            updateUserPresenceStateInternal(UserPresenceState.UNKNOWN, "Service Destroyed")
            stopAllMonitoringLogic()
        }
        serviceJob.cancel()
        Log.d(TAG, "Service Destroyed. Final state: $currentPresenceState")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}