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

    private enum class EvaluationSource {
        SCREEN_ON,
        SCREEN_OFF,
        USER_PRESENT,
        HEURISTIC_TIMER,
        SLEEP_API_SEGMENT,
        SLEEP_API_CLASSIFY
    }

    private var isSleepApiAvailable = false

    companion object {
        private const val TAG = "UserPresenceService"
        private const val NOTIFICATION_CHANNEL_ID = "UserPresenceServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SLEEP_API_PENDING_INTENT_REQUEST_CODE = 1001

        const val ACTION_START_SERVICE = "com.andrew264.habits.action.START_PRESENCE_SERVICE"
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

        private const val WINDING_DOWN_DELAY_MS = 30 * 60 * 1000L // 30 minutes
        private const val DEFAULT_HEURISTIC_SLEEP_DURATION_HOURS = 8

        private val _userPresenceState = MutableStateFlow(UserPresenceState.UNKNOWN)
        val userPresenceState: StateFlow<UserPresenceState> = _userPresenceState.asStateFlow()

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        internal val _manualSleepSchedule = MutableStateFlow(ManualSleepSchedule())
        val manualSleepSchedule: StateFlow<ManualSleepSchedule> = _manualSleepSchedule.asStateFlow()

        private fun updateState(newState: UserPresenceState, reason: String) {
            if (_userPresenceState.value != newState) {
                _userPresenceState.value = newState
                Log.i(TAG, "STATE CHANGE: User presence -> $newState (Reason: $reason)")
            }
        }
    }

    private val activityRecognitionClient by lazy { ActivityRecognition.getClient(this) }
    private var sleepApiPendingIntent: PendingIntent? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> evaluateState(EvaluationSource.SCREEN_ON)
                Intent.ACTION_SCREEN_OFF -> evaluateState(EvaluationSource.SCREEN_OFF)
                Intent.ACTION_USER_PRESENT -> evaluateState(EvaluationSource.USER_PRESENT)
            }
        }
    }
    private val windingDownHandler = Handler(Looper.getMainLooper())
    private var windingDownRunnable: Runnable? = null
    private var isReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        _userPresenceState.value = UserPresenceState.UNKNOWN
        _isServiceActive.value = false
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startAllMonitoring()
            }

            ACTION_PROCESS_SLEEP_SEGMENT_EVENTS -> {
                val events: ArrayList<SleepSegmentEvent>? =
                    intent.getParcelableArrayListExtra(
                        EXTRA_SLEEP_SEGMENTS,
                        SleepSegmentEvent::class.java
                    )
                events?.let { processSleepSegmentEvents(it) }
            }
            // ... other actions for manual schedule remain the same ...
            ACTION_SET_MANUAL_BEDTIME -> {
                val hour = intent.getIntExtra(EXTRA_MANUAL_BEDTIME_HOUR, -1)
                val minute = intent.getIntExtra(EXTRA_MANUAL_BEDTIME_MINUTE, -1)
                if (hour != -1 && minute != -1) {
                    _manualSleepSchedule.value = _manualSleepSchedule.value.copy(
                        bedtimeHour = hour,
                        bedtimeMinute = minute
                    )
                    Log.i(TAG, "Manual bedtime set to: $hour:$minute")
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
                stopAllMonitoring()
                _isServiceActive.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun startAllMonitoring() {
        if (_isServiceActive.value) {
            Log.d(TAG, "Start command received, but service is already active.")
            return
        }
        Log.i(TAG, "Starting all monitoring services.")

        isSleepApiAvailable = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Sleep API available: $isSleepApiAvailable")

        if (isSleepApiAvailable) {
            subscribeToSleepUpdates()
        }
        registerScreenStateReceiver()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            updateState(UserPresenceState.AWAKE, "Initial Start - Screen On")
        } else if (isNightTime()) {
            evaluateState(EvaluationSource.SCREEN_OFF)
        } else {
            updateState(UserPresenceState.UNKNOWN, "Initial Start - Screen Off")
        }

        val notificationText =
            if (isSleepApiAvailable) "Sleep API & Heuristics Active" else "Heuristics Only Active"
        startForeground(NOTIFICATION_ID, createNotification(notificationText))
        _isServiceActive.value = true
    }

    private fun stopAllMonitoring() {
        Log.i(TAG, "Stopping all monitoring services.")
        if (isSleepApiAvailable) {
            unsubscribeFromSleepUpdates()
        }
        unregisterScreenStateReceiver()
        cancelWindingDownTimer()
        updateState(UserPresenceState.UNKNOWN, "Service Stopped")
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Habit Tracker Presence")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    // --- Central State Evaluation Logic ---

    @Synchronized
    private fun evaluateState(source: EvaluationSource, data: Any? = null) {
        Log.d(TAG, "Evaluating state. Source: $source, Current State: ${_userPresenceState.value}")

        // Rule #1: Absolute Awake (Highest Priority)
        if (source == EvaluationSource.SCREEN_ON || source == EvaluationSource.USER_PRESENT) {
            cancelWindingDownTimer()
            updateState(UserPresenceState.AWAKE, "Device Interaction")
            return
        }

        // Rule #2: Entering Potential Sleep
        if (source == EvaluationSource.SCREEN_OFF && isNightTime()) {
            if (_userPresenceState.value == UserPresenceState.AWAKE || _userPresenceState.value == UserPresenceState.UNKNOWN) {
                updateState(UserPresenceState.WINDING_DOWN, "Screen off at night")
                startWindingDownTimer()
            }
            return
        }

        // Rule #3: Confirming Sleep
        if (source == EvaluationSource.HEURISTIC_TIMER) {
            if (_userPresenceState.value == UserPresenceState.WINDING_DOWN) {
                updateState(UserPresenceState.SLEEPING, "Heuristic Timer Confirmation")
            }
            return
        }

        if (source == EvaluationSource.SLEEP_API_SEGMENT && data is SleepSegmentEvent) {
            val event = data
            Log.d(
                TAG,
                "Processing SleepSegmentEvent: Status=${event.status}, Start=${event.startTimeMillis}, End=${event.endTimeMillis}"
            )

            if (event.status == SleepSegmentEvent.STATUS_SUCCESSFUL) {
                val now = System.currentTimeMillis()
                // If we are currently inside the detected sleep segment, confirm SLEEPING.
                if (now >= event.startTimeMillis && now <= event.endTimeMillis) {
                    cancelWindingDownTimer()
                    updateState(UserPresenceState.SLEEPING, "Sleep API In Segment")
                    return
                }
                // If a sleep segment has recently ended, we are likely AWAKE.
                if (now > event.endTimeMillis && now < event.endTimeMillis + TimeUnit.MINUTES.toMillis(
                        15
                    )
                ) {
                    cancelWindingDownTimer()
                    updateState(UserPresenceState.AWAKE, "Sleep API Segment Just Ended")
                    return
                }
            }
        }
    }

    // --- Heuristic Helpers ---

    private fun startWindingDownTimer() {
        cancelWindingDownTimer()
        Log.d(TAG, "Starting winding down timer for ${WINDING_DOWN_DELAY_MS / 1000}s")
        windingDownRunnable = Runnable {
            evaluateState(EvaluationSource.HEURISTIC_TIMER)
        }
        windingDownHandler.postDelayed(windingDownRunnable!!, WINDING_DOWN_DELAY_MS)
    }

    private fun cancelWindingDownTimer() {
        windingDownRunnable?.let {
            windingDownHandler.removeCallbacks(it)
            Log.d(TAG, "Winding down timer cancelled.")
        }
        windingDownRunnable = null
    }

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
                } else { // Overnight case
                    currentTimeInMinutes >= userBedtimeInMinutes || currentTimeInMinutes < userWakeUpTimeInMinutes
                }
            } else {
                val heuristicWindowEndMinutes =
                    (userBedtimeInMinutes + DEFAULT_HEURISTIC_SLEEP_DURATION_HOURS * 60) % (24 * 60)
                return if (userBedtimeInMinutes <= heuristicWindowEndMinutes) {
                    currentTimeInMinutes >= userBedtimeInMinutes && currentTimeInMinutes < heuristicWindowEndMinutes
                } else { // Overnight case
                    currentTimeInMinutes >= userBedtimeInMinutes || currentTimeInMinutes < heuristicWindowEndMinutes
                }
            }
        } else {
            // Default: 10 PM to 6 AM
            return currentHour >= 22 || currentHour < 6
        }
    }


    private fun registerScreenStateReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(
                this,
                screenStateReceiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
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

    // --- Sleep API ---

    private fun processSleepSegmentEvents(events: List<SleepSegmentEvent>) {
        Log.d(TAG, "Received ${events.size} sleep segment events from receiver.")
        events.forEach { event ->
            evaluateState(EvaluationSource.SLEEP_API_SEGMENT, event)
        }
    }

    private fun processSleepClassifyEvents(events: List<SleepClassifyEvent>) {
        // Can be implemented later if needed
        Log.d(TAG, "Processing ${events.size} sleep classify events.")
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
            Log.w(TAG, "Attempted to subscribe to Sleep API, but permission is missing.")
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
            Log.d(TAG, "No active Sleep API subscription (PendingIntent is null).")
        }
    }

    private fun getSleepPendingIntent(): PendingIntent {
        if (sleepApiPendingIntent == null) {
            val intent = Intent(this, SleepReceiver::class.java)
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

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "User Presence Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service Destroying. Cleaning up all resources.")
        stopAllMonitoring()
        _isServiceActive.value = false
        _manualSleepSchedule.value = ManualSleepSchedule() // Reset on destroy
        Log.d(TAG, "Service Destroyed. Final state: ${_userPresenceState.value}")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}