package com.andrew264.habits.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.andrew264.habits.MainActivity
import com.andrew264.habits.R
import com.andrew264.habits.model.ManualSleepSchedule
import com.andrew264.habits.receiver.SleepReceiver
import com.andrew264.habits.state.UserPresenceState
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import com.google.android.gms.location.SleepSegmentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UserPresenceService : Service() {

    enum class OperatingMode {
        SLEEP_API_ACTIVE,
        HEURISTICS_ACTIVE,
        STOPPED
    }

    private var currentMode = OperatingMode.STOPPED

    companion object {
        private const val TAG = "UserPresenceService"
        private const val NOTIFICATION_CHANNEL_ID = "UserPresenceServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SLEEP_API_PENDING_INTENT_REQUEST_CODE = 1001

        const val ACTION_START_SERVICE_SLEEP_API = "com.andrew264.habits.action.START_SLEEP_API"
        const val ACTION_START_SERVICE_HEURISTICS = "com.andrew264.habits.action.START_HEURISTICS"
        const val ACTION_STOP_SERVICE = "com.andrew264.habits.action.STOP_PRESENCE_SERVICE"
        const val ACTION_PROCESS_SLEEP_SEGMENT_EVENTS =
            "com.andrew264.habits.action.PROCESS_SLEEP_SEGMENT_EVENTS"
        const val ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS =
            "com.andrew264.habits.action.PROCESS_SLEEP_CLASSIFY_EVENTS"

        const val ACTION_SET_MANUAL_BEDTIME = "com.andrew264.habits.action.SET_MANUAL_BEDTIME"
        const val ACTION_CLEAR_MANUAL_BEDTIME = "com.andrew264.habits.action.CLEAR_MANUAL_BEDTIME"
        const val ACTION_SET_MANUAL_WAKE_UP_TIME =
            "com.andrew264.habits.action.SET_MANUAL_WAKE_UP_TIME"
        const val ACTION_CLEAR_MANUAL_WAKE_UP_TIME =
            "com.andrew264.habits.action.CLEAR_MANUAL_WAKE_UP_TIME"


        const val EXTRA_SLEEP_SEGMENTS = "com.andrew264.habits.extra.SLEEP_SEGMENTS"
        const val EXTRA_SLEEP_CLASSIFY_EVENTS = "com.andrew264.habits.extra.SLEEP_CLASSIFY_EVENTS"
        const val EXTRA_MANUAL_BEDTIME_HOUR = "com.andrew264.habits.extra.MANUAL_BEDTIME_HOUR"
        const val EXTRA_MANUAL_BEDTIME_MINUTE = "com.andrew264.habits.extra.MANUAL_BEDTIME_MINUTE"
        const val EXTRA_MANUAL_WAKE_UP_HOUR = "com.andrew264.habits.extra.MANUAL_WAKE_UP_HOUR"
        const val EXTRA_MANUAL_WAKE_UP_MINUTE = "com.andrew264.habits.extra.MANUAL_WAKE_UP_MINUTE"


        private const val SCREEN_OFF_SLEEP_DELAY_MS = 30 * 60 * 1000L // 30 minutes
        private const val DEFAULT_HEURISTIC_SLEEP_DURATION_HOURS = 8


        private val _userPresenceState = MutableStateFlow(UserPresenceState.UNKNOWN)
        val userPresenceState: StateFlow<UserPresenceState> = _userPresenceState.asStateFlow()

        private val _currentOperatingMode = MutableStateFlow(OperatingMode.STOPPED)
        val currentOperatingMode: StateFlow<OperatingMode> = _currentOperatingMode.asStateFlow()

        internal val _manualSleepSchedule =
            MutableStateFlow(ManualSleepSchedule())
        val manualSleepSchedule: StateFlow<ManualSleepSchedule> = _manualSleepSchedule.asStateFlow()


        fun updateState(newState: UserPresenceState, source: String) {
            if (_userPresenceState.value != newState) {
                _userPresenceState.value = newState
                Log.d(TAG, "User presence state changed to: $newState (Source: $source)")
            } else {
                Log.d(TAG, "User presence state already $newState (Source: $source)")
            }
        }
    }

    private val activityRecognitionClient by lazy { ActivityRecognition.getClient(this) }
    private var sleepApiPendingIntent: PendingIntent? = null

    private val heuristicScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isRelevantIntentForAnyMode =
                intent?.action == Intent.ACTION_SCREEN_ON || intent?.action == Intent.ACTION_USER_PRESENT
            if (currentMode != OperatingMode.HEURISTICS_ACTIVE && !isRelevantIntentForAnyMode) {
                return
            }

            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "[Heuristic/Shared] Screen ON")
                    cancelHeuristicSleepTimer()
                    updateState(UserPresenceState.AWAKE, "Receiver:ScreenOn")
                }

                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "[Heuristic] Screen OFF (Processed if in Heuristics Mode)")
                    if (currentMode == OperatingMode.HEURISTICS_ACTIVE && isNightTime()) {
                        startHeuristicSleepTimer()
                    }
                }

                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "[Heuristic/Shared] User PRESENT (unlocked)")
                    cancelHeuristicSleepTimer()
                    updateState(UserPresenceState.AWAKE, "Receiver:UserPresent")
                }
            }
        }
    }
    private val heuristicHandler = Handler(Looper.getMainLooper())
    private var heuristicSleepRunnable: Runnable? = null
    private var isHeuristicReceiverRegistered = false


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            updateState(UserPresenceState.AWAKE, "InitialCheck")
        } else {
            updateState(UserPresenceState.UNKNOWN, "InitialCheck")
        }
        _currentOperatingMode.value = OperatingMode.STOPPED
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "Service onStartCommand with action: ${intent?.action}, Current Mode: $currentMode"
        )
        when (intent?.action) {
            ACTION_START_SERVICE_SLEEP_API -> {
                if (currentMode != OperatingMode.SLEEP_API_ACTIVE) {
                    stopPreviousModeResources()
                    currentMode = OperatingMode.SLEEP_API_ACTIVE
                    _currentOperatingMode.value = currentMode
                    startForegroundIfNeeded("Sleep API Monitoring Active")
                    subscribeToSleepUpdates()
                    registerHeuristicScreenReceiver() // Keep screen on/off for immediate AWAKE state
                }
            }

            ACTION_START_SERVICE_HEURISTICS -> {
                if (currentMode != OperatingMode.HEURISTICS_ACTIVE) {
                    stopPreviousModeResources()
                    currentMode = OperatingMode.HEURISTICS_ACTIVE
                    _currentOperatingMode.value = currentMode
                    startForegroundIfNeeded("Heuristic Monitoring Active")
                    registerHeuristicScreenReceiver()
                    val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                    if (!powerManager.isInteractive && isNightTime()) {
                        startHeuristicSleepTimer()
                    }
                }
            }

            ACTION_PROCESS_SLEEP_SEGMENT_EVENTS -> {
                if (currentMode == OperatingMode.SLEEP_API_ACTIVE) {
                    val events: ArrayList<SleepSegmentEvent>? =
                        intent.getParcelableArrayListExtra(
                            EXTRA_SLEEP_SEGMENTS,
                            SleepSegmentEvent::class.java
                        )
                    events?.let { processSleepSegmentEvents(it) }
                }
            }

            ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS -> {
                if (currentMode == OperatingMode.SLEEP_API_ACTIVE) {
                    val events: ArrayList<SleepClassifyEvent>? =
                        intent.getParcelableArrayListExtra(
                            EXTRA_SLEEP_CLASSIFY_EVENTS,
                            SleepClassifyEvent::class.java
                        )
                    events?.let { processSleepClassifyEvents(it) }
                }
            }

            ACTION_SET_MANUAL_BEDTIME -> {
                val hour = intent.getIntExtra(EXTRA_MANUAL_BEDTIME_HOUR, -1)
                val minute = intent.getIntExtra(EXTRA_MANUAL_BEDTIME_MINUTE, -1)
                if (hour != -1 && minute != -1) {
                    _manualSleepSchedule.value = _manualSleepSchedule.value.copy(
                        bedtimeHour = hour,
                        bedtimeMinute = minute
                    )
                    Log.i(TAG, "Manual bedtime set to: $hour:$minute")
                } else {
                    Log.w(TAG, "Invalid hour/minute for manual bedtime.")
                }
            }

            ACTION_CLEAR_MANUAL_BEDTIME -> {
                _manualSleepSchedule.value = _manualSleepSchedule.value.copy(
                    bedtimeHour = null,
                    bedtimeMinute = null
                )
                Log.i(TAG, "Manual bedtime cleared.")
            }

            ACTION_SET_MANUAL_WAKE_UP_TIME -> {
                val hour = intent.getIntExtra(EXTRA_MANUAL_WAKE_UP_HOUR, -1)
                val minute = intent.getIntExtra(EXTRA_MANUAL_WAKE_UP_MINUTE, -1)
                if (hour != -1 && minute != -1) {
                    _manualSleepSchedule.value = _manualSleepSchedule.value.copy(
                        wakeUpHour = hour,
                        wakeUpMinute = minute
                    )
                    Log.i(TAG, "Manual wake-up time set to: $hour:$minute")
                } else {
                    Log.w(TAG, "Invalid hour/minute for manual wake-up time.")
                }
            }

            ACTION_CLEAR_MANUAL_WAKE_UP_TIME -> {
                _manualSleepSchedule.value = _manualSleepSchedule.value.copy(
                    wakeUpHour = null,
                    wakeUpMinute = null
                )
                Log.i(TAG, "Manual wake-up time cleared.")
            }

            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "ACTION_STOP_SERVICE received. Stopping service.")
                stopPreviousModeResources()
                unregisterHeuristicScreenReceiver()
                currentMode = OperatingMode.STOPPED
                _currentOperatingMode.value = currentMode
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun stopPreviousModeResources() {
        Log.d(TAG, "Stopping mode-specific resources for previous mode: $currentMode")
        when (currentMode) {
            OperatingMode.SLEEP_API_ACTIVE -> {
                unsubscribeFromSleepUpdates()
                // Do not unregister heuristic receiver if shared
            }

            OperatingMode.HEURISTICS_ACTIVE -> {
                cancelHeuristicSleepTimer()
                // Do not unregister heuristic receiver if shared
            }

            OperatingMode.STOPPED -> {
                // No active mode-specific resources to stop
            }
        }
    }


    private fun startForegroundIfNeeded(contentText: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Habit Tracker Presence")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Service started in foreground.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }
    }


    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "User Presence Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    private fun getSleepPendingIntent(): PendingIntent {
        if (sleepApiPendingIntent == null) {
            val intent = Intent(this, SleepReceiver::class.java)
            // Flag MUTABLE is required for Sleep API on Android 12+
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            sleepApiPendingIntent = PendingIntent.getBroadcast(
                this,
                SLEEP_API_PENDING_INTENT_REQUEST_CODE,
                intent,
                flags
            )
        }
        return sleepApiPendingIntent!!
    }

    private fun subscribeToSleepUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Subscribing to Sleep API updates.")
            activityRecognitionClient.requestSleepSegmentUpdates(
                getSleepPendingIntent(),
                SleepSegmentRequest.getDefaultSleepSegmentRequest()
            ).addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed to Sleep API.")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to Sleep API.", e)
            }
        } else {
            Log.w(
                TAG,
                "ACTIVITY_RECOGNITION permission not granted. Cannot subscribe to Sleep API."
            )
        }
    }

    private fun unsubscribeFromSleepUpdates() {
        Log.d(TAG, "Attempting to unsubscribe from Sleep API updates.")
        if (sleepApiPendingIntent != null) {
            activityRecognitionClient.removeSleepSegmentUpdates(sleepApiPendingIntent!!)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully unsubscribed from Sleep API updates.")
                    sleepApiPendingIntent?.cancel()
                    sleepApiPendingIntent = null
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unsubscribe from Sleep API updates.", e)
                }
        } else {
            Log.d(
                TAG,
                "No active Sleep API subscription (PendingIntent is null), skipping un-subscription."
            )
        }
    }

    private fun processSleepSegmentEvents(events: List<SleepSegmentEvent>) {
        Log.d(TAG, "Processing ${events.size} sleep segment events.")
        if (events.isEmpty()) return

        val latestEvent = events.maxByOrNull { it.endTimeMillis } ?: return

        Log.d(
            TAG,
            "Latest SleepSegmentEvent: Start=${latestEvent.startTimeMillis}, End=${latestEvent.endTimeMillis}, Status=${latestEvent.status}, Duration=${latestEvent.segmentDurationMillis}ms"
        )

        val currentTime = System.currentTimeMillis()
        when (latestEvent.status) {
            SleepSegmentEvent.STATUS_SUCCESSFUL -> {
                // If current time is within 15 minutes AFTER the segment end, consider user AWAKE.
                if (currentTime > latestEvent.endTimeMillis && currentTime < latestEvent.endTimeMillis + TimeUnit.MINUTES.toMillis(
                        15
                    )
                ) {
                    updateState(UserPresenceState.AWAKE, "SleepAPI:SegmentEnd")
                }
                // If current time is INSIDE the detected sleep segment.
                else if (currentTime >= latestEvent.startTimeMillis && currentTime <= latestEvent.endTimeMillis) {
                    updateState(UserPresenceState.SLEEPING, "SleepAPI:InSegment")
                }
                // If current time is significantly after the segment (more than 15 min), assume AWAKE.
                else if (currentTime > latestEvent.endTimeMillis) {
                    updateState(UserPresenceState.AWAKE, "SleepAPI:AfterSegment")
                } else {
                    // Current time is before the segment start, or some other edge case.
                    // This might happen if events are delayed or out of order.
                    // We might be AWAKE or UNKNOWN depending on prior state.
                    // For now, don't change state aggressively if we are before the segment.
                    Log.d(
                        TAG,
                        "SleepSegment: Current time (${currentTime}) is before latest segment start (${latestEvent.startTimeMillis}). No immediate state change based on this event alone."
                    )
                }
            }

            SleepSegmentEvent.STATUS_MISSING_DATA, SleepSegmentEvent.STATUS_NOT_DETECTED -> {
                // These statuses are not definitive. If we were SLEEPING, we might still be.
                // If we were AWAKE, we are likely still AWAKE.
                // Avoid changing state to UNKNOWN too readily from these.
                Log.d(
                    TAG,
                    "Sleep API: Status ${latestEvent.status}. Not conclusive for state change."
                )
            }
        }
    }


    private fun processSleepClassifyEvents(events: List<SleepClassifyEvent>) {
        Log.d(TAG, "Processing ${events.size} sleep classify events.")
        // SleepClassifyEvents are less definitive. Consider them for UNKNOWN to AWAKE/SLEEPING transitions
        // but be cautious about overriding SleepSegmentEvent data.
        // For simplicity, we'll primarily rely on SleepSegmentEvents for SLEEPING/AWAKE states.
    }


    private fun registerHeuristicScreenReceiver() {
        if (!isHeuristicReceiverRegistered) {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT) // User unlocked the device
            }
            // Register for all users if needed, or stick to current user context.
            // Using ContextCompat.RECEIVER_NOT_EXPORTED for security if only local.
            ContextCompat.registerReceiver(
                this,
                heuristicScreenReceiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isHeuristicReceiverRegistered = true
            Log.d(TAG, "Heuristic screen state receiver registered.")
        }
    }

    private fun unregisterHeuristicScreenReceiver() {
        if (isHeuristicReceiverRegistered) {
            try {
                unregisterReceiver(heuristicScreenReceiver)
                isHeuristicReceiverRegistered = false
                Log.d(TAG, "Heuristic screen state receiver unregistered.")
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered or already unregistered.
                Log.w(
                    TAG,
                    "Heuristic receiver not registered or already unregistered: ${e.message}"
                )
            }
        }
    }

    private fun startHeuristicSleepTimer() {
        cancelHeuristicSleepTimer() // Ensure no existing timer is running
        Log.d(TAG, "[Heuristic] Starting sleep timer for ${SCREEN_OFF_SLEEP_DELAY_MS / 1000}s")
        heuristicSleepRunnable = Runnable {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            // Double check conditions: screen still off AND it's still considered night time
            if (!powerManager.isInteractive && isNightTime()) {
                Log.d(
                    TAG,
                    "[Heuristic] Sleep timer fired, screen still off. Setting state to SLEEPING."
                )
                updateState(UserPresenceState.SLEEPING, "Heuristic:TimerFired")
            } else {
                Log.d(
                    TAG,
                    "[Heuristic] Sleep timer fired, but conditions not met for SLEEPING (screen on or not night time)."
                )
            }
        }
        heuristicHandler.postDelayed(heuristicSleepRunnable!!, SCREEN_OFF_SLEEP_DELAY_MS)
    }

    private fun cancelHeuristicSleepTimer() {
        heuristicSleepRunnable?.let {
            heuristicHandler.removeCallbacks(it)
            Log.d(TAG, "[Heuristic] Sleep timer cancelled.")
        }
        heuristicSleepRunnable = null
    }

    /**
     * Determines if the current time is considered "night time" for heuristic sleep detection.
     * Uses manually set bedtime and wake-up time if available, otherwise defaults.
     */
    private fun isNightTime(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val schedule = _manualSleepSchedule.value
        val userBedtimeInMinutes = schedule.bedtimeInMinutesTotal
        val userWakeUpTimeInMinutes = schedule.wakeUpInMinutesTotal

        if (userBedtimeInMinutes != null) {
            if (userWakeUpTimeInMinutes != null) {
                return if (userBedtimeInMinutes <= userWakeUpTimeInMinutes) {
                    currentTimeInMinutes >= userBedtimeInMinutes && currentTimeInMinutes < userWakeUpTimeInMinutes
                } else {
                    currentTimeInMinutes >= userBedtimeInMinutes || currentTimeInMinutes < userWakeUpTimeInMinutes
                }
            } else {
                val heuristicWindowEndMinutes =
                    (userBedtimeInMinutes + DEFAULT_HEURISTIC_SLEEP_DURATION_HOURS * 60) % (24 * 60)

                return if (userBedtimeInMinutes <= heuristicWindowEndMinutes) {
                    currentTimeInMinutes >= userBedtimeInMinutes && currentTimeInMinutes < heuristicWindowEndMinutes
                } else {
                    currentTimeInMinutes >= userBedtimeInMinutes || currentTimeInMinutes < heuristicWindowEndMinutes
                }
            }
        } else {
            return currentHour >= 22 || currentHour < 6
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service Destroying. Cleaning up all resources.")
        unsubscribeFromSleepUpdates()
        cancelHeuristicSleepTimer()
        unregisterHeuristicScreenReceiver() // Ensure receiver is unregistered

        _userPresenceState.value = UserPresenceState.UNKNOWN
        currentMode = OperatingMode.STOPPED
        _currentOperatingMode.value = currentMode
        _manualSleepSchedule.value = ManualSleepSchedule() // Reset to default empty schedule

        Log.d(
            TAG,
            "Service Destroyed. Final state: ${_userPresenceState.value}, Mode: $currentMode"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}