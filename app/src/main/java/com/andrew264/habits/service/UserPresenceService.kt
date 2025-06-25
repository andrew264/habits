package com.andrew264.habits.service

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.andrew264.habits.MainActivity
import com.andrew264.habits.R
import com.andrew264.habits.data.repository.SettingsRepository
import com.andrew264.habits.data.repository.UserPresenceHistoryRepository
import com.andrew264.habits.model.ManualSleepSchedule
import com.andrew264.habits.receiver.SleepReceiver
import com.andrew264.habits.state.UserPresenceState
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import com.google.android.gms.location.SleepSegmentRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class UserPresenceService : Service() {

    private enum class EvaluationSource {
        SCREEN_ON,
        SCREEN_OFF,
        USER_PRESENT,
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
        const val ACTION_PROCESS_SLEEP_SEGMENT_EVENTS = "com.andrew264.habits.action.PROCESS_SLEEP_SEGMENT_EVENTS"
        const val ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS = "com.andrew264.habits.action.PROCESS_SLEEP_CLASSIFY_EVENTS"

        const val ACTION_SET_MANUAL_BEDTIME = "com.andrew264.habits.action.SET_MANUAL_BEDTIME"
        const val ACTION_CLEAR_MANUAL_BEDTIME = "com.andrew264.habits.action.CLEAR_MANUAL_BEDTIME"
        const val ACTION_SET_MANUAL_WAKE_UP_TIME = "com.andrew264.habits.action.SET_MANUAL_WAKE_UP_TIME"
        const val ACTION_CLEAR_MANUAL_WAKE_UP_TIME = "com.andrew264.habits.action.CLEAR_MANUAL_WAKE_UP_TIME"

        const val EXTRA_SLEEP_SEGMENTS = "com.andrew264.habits.extra.SLEEP_SEGMENTS"
        const val EXTRA_SLEEP_CLASSIFY_EVENTS = "com.andrew264.habits.extra.SLEEP_CLASSIFY_EVENTS"
        const val EXTRA_MANUAL_BEDTIME_HOUR = "com.andrew264.habits.extra.MANUAL_BEDTIME_HOUR"
        const val EXTRA_MANUAL_BEDTIME_MINUTE = "com.andrew264.habits.extra.MANUAL_BEDTIME_MINUTE"
        const val EXTRA_MANUAL_WAKE_UP_HOUR = "com.andrew264.habits.extra.MANUAL_WAKE_UP_HOUR"
        const val EXTRA_MANUAL_WAKE_UP_MINUTE = "com.andrew264.habits.extra.MANUAL_WAKE_UP_MINUTE"

        private const val DEFAULT_HEURISTIC_SLEEP_DURATION_HOURS = 8

        private val _userPresenceStateFlow = MutableStateFlow(UserPresenceState.UNKNOWN)
        val userPresenceState: StateFlow<UserPresenceState> get() = _userPresenceStateFlow.asStateFlow()

        private val _isServiceActiveFlow = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> get() = _isServiceActiveFlow.asStateFlow()

        private val _manualSleepScheduleFlow = MutableStateFlow(ManualSleepSchedule())

        private fun updateUserPresenceStateInternal(
            newState: UserPresenceState,
            reason: String,
            repository: UserPresenceHistoryRepository?,
            scope: CoroutineScope
        ) {
            val oldState = _userPresenceStateFlow.value
            if (oldState != newState) {
                _userPresenceStateFlow.value = newState
                Log.i(TAG, "STATE CHANGE: User presence -> $newState (Reason: $reason)")
                repository?.let { repo ->
                    scope.launch {
                        try {
                            val timestamp = System.currentTimeMillis()
                            repo.addPresenceEvent(newState, timestamp)
                            Log.d(TAG, "Logged presence event: $newState at $timestamp")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to log presence event to DB", e)
                        }
                    }
                } ?: Log.w(TAG, "UserPresenceHistoryRepository not available when trying to log state change.")
            }
        }
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userPresenceHistoryRepository: UserPresenceHistoryRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val activityRecognitionClient by lazy { ActivityRecognition.getClient(this) }
    private var sleepApiPendingIntent: PendingIntent? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> evaluateState(EvaluationSource.SCREEN_ON)
                Intent.ACTION_SCREEN_OFF -> evaluateState(EvaluationSource.SCREEN_OFF)
                Intent.ACTION_USER_PRESENT -> evaluateState(EvaluationSource.USER_PRESENT)
            }
        }
    }

    private var isReceiverRegistered = false
    private var _isServiceActuallyRunning = false
    private var currentManualSleepSchedule = ManualSleepSchedule()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        createNotificationChannel()

        serviceScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                Log.d(
                    TAG,
                    "Settings loaded/changed: Active=${settings.isServiceActive}, Schedule=${settings.manualSleepSchedule}"
                )
                _isServiceActiveFlow.value = settings.isServiceActive
                _manualSleepScheduleFlow.value = settings.manualSleepSchedule
                currentManualSleepSchedule = settings.manualSleepSchedule

                if (settings.isServiceActive && !_isServiceActuallyRunning) {
                    Log.d(TAG, "Service persisted as active, and not running. Starting monitoring logic.")
                    startAllMonitoringLogic()
                } else if (!settings.isServiceActive && _isServiceActuallyRunning) {
                    Log.d(TAG, "Service persisted as inactive, but monitoring logic is running. Stopping.")
                    stopAllMonitoringLogic()
                }
                if (_isServiceActuallyRunning) {
                    evaluateState(EvaluationSource.SCREEN_OFF)
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
                    if (!_isServiceActiveFlow.value) {
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
                    events?.forEach { event -> evaluateState(EvaluationSource.SLEEP_API_SEGMENT, event) }
                }

                ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS -> {
                    val events: ArrayList<SleepClassifyEvent>? = intent.getParcelableArrayListExtra(EXTRA_SLEEP_CLASSIFY_EVENTS, SleepClassifyEvent::class.java)
                    events?.forEach { event -> evaluateState(EvaluationSource.SLEEP_API_CLASSIFY, event) }
                }


                ACTION_SET_MANUAL_BEDTIME -> {
                    val hour = intent.getIntExtra(EXTRA_MANUAL_BEDTIME_HOUR, -1)
                    val minute = intent.getIntExtra(EXTRA_MANUAL_BEDTIME_MINUTE, -1)
                    if (hour != -1 && minute != -1) {
                        settingsRepository.updateManualBedtime(hour, minute)
                        Log.i(TAG, "Manual bedtime set to: $hour:$minute and persisted.")
                    }
                }

                ACTION_CLEAR_MANUAL_BEDTIME -> {
                    settingsRepository.updateManualBedtime(null, null)
                    Log.i(TAG, "Manual bedtime cleared and persisted.")
                }

                ACTION_SET_MANUAL_WAKE_UP_TIME -> {
                    val hour = intent.getIntExtra(EXTRA_MANUAL_WAKE_UP_HOUR, -1)
                    val minute = intent.getIntExtra(EXTRA_MANUAL_WAKE_UP_MINUTE, -1)
                    if (hour != -1 && minute != -1) {
                        settingsRepository.updateManualWakeUpTime(hour, minute)
                        Log.i(TAG, "Manual wake-up time set to: $hour:$minute and persisted.")
                    }
                }

                ACTION_CLEAR_MANUAL_WAKE_UP_TIME -> {
                    settingsRepository.updateManualWakeUpTime(null, null)
                    Log.i(TAG, "Manual wake-up time cleared and persisted.")
                }

                ACTION_STOP_SERVICE -> {
                    Log.d(TAG, "ACTION_STOP_SERVICE received. Persisting inactive state.")
                    settingsRepository.updateServiceActiveState(false)
                }
            }
        }
        return if (intent?.action == ACTION_STOP_SERVICE && !_isServiceActiveFlow.value) START_NOT_STICKY else START_STICKY
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

        startForeground(NOTIFICATION_ID, createNotification())
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            evaluateState(EvaluationSource.SCREEN_ON)
        } else {
            evaluateState(EvaluationSource.SCREEN_OFF)
        }
    }

    private fun stopAllMonitoringLogic() {
        if (!_isServiceActuallyRunning) {
            Log.d(TAG, "Stop logic called, but service monitoring is not active.")
            if (_userPresenceStateFlow.value != UserPresenceState.UNKNOWN) {
                updateUserPresenceStateInternal(UserPresenceState.UNKNOWN, "Service Monitoring Stopped", userPresenceHistoryRepository, serviceScope)
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
        updateUserPresenceStateInternal(UserPresenceState.UNKNOWN, "Service Monitoring Stopped", userPresenceHistoryRepository, serviceScope)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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

        val currentPresenceText = _userPresenceStateFlow.value.name.replace('_', ' ')
        val baseText = if (isSleepApiAvailable) "Sleep API & Heuristics" else "Heuristics Only"
        val contentText = "$baseText Active. State: $currentPresenceText"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Habit Tracker Presence")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    @Synchronized
    private fun evaluateState(
        source: EvaluationSource,
        data: Any? = null
    ) {
        val oldState = _userPresenceStateFlow.value
        var newStateCandidate = oldState
        var reasonForChange = "No Change (Source: $source, Old State: $oldState)"

        Log.d(TAG, "Evaluating state. Source: $source, Old State: $oldState, Is Night Time: ${isNightTime()}, Manual Schedule: $currentManualSleepSchedule")

        // Priority 1: AWAKE conditions
        if (source == EvaluationSource.SCREEN_ON || source == EvaluationSource.USER_PRESENT) {
            newStateCandidate = UserPresenceState.AWAKE
            reasonForChange = "Device Interaction (Source: $source)"
        } else if (source == EvaluationSource.SLEEP_API_SEGMENT && data is SleepSegmentEvent) {
            val event = data
            if (event.status == SleepSegmentEvent.STATUS_SUCCESSFUL) {
                val now = System.currentTimeMillis()
                // Check if sleep segment just ended (within last 15 mins)
                if (now > event.startTimeMillis && now > event.endTimeMillis && now < event.endTimeMillis + TimeUnit.MINUTES.toMillis(15)) {
                    newStateCandidate = UserPresenceState.AWAKE
                    reasonForChange = "Sleep API Segment Just Ended"
                }
            }
        }

        // If newState is AWAKE from above, this is the highest priority.
        if (newStateCandidate == UserPresenceState.AWAKE) {
            if (oldState != UserPresenceState.AWAKE) {
                updateUserPresenceStateInternal(newStateCandidate, reasonForChange, userPresenceHistoryRepository, serviceScope)
                updateNotificationContentOnly()
            } else {
                Log.d(TAG, "State confirmed AWAKE by $source. No change from $oldState. Notification content may update if text changed.")
                updateNotificationContentOnly()
            }
            return
        }

        // Priority 2: SLEEPING conditions from Sleep API (if not determined to be AWAKE)
        if (source == EvaluationSource.SLEEP_API_SEGMENT && data is SleepSegmentEvent) {
            val event = data
            if (event.status == SleepSegmentEvent.STATUS_SUCCESSFUL) {
                val now = System.currentTimeMillis()
                // Check if currently in a sleep segment
                if (now >= event.startTimeMillis && now <= event.endTimeMillis) {
                    newStateCandidate = UserPresenceState.SLEEPING
                    reasonForChange = "Sleep API In Segment"
                }
            }
        }
        // Consider SleepClassifyEvent for high-confidence sleep if needed, though Segment is usually primary
        else if (source == EvaluationSource.SLEEP_API_CLASSIFY && data is SleepClassifyEvent) {
            val event = data
            if (event.confidence >= 75 && (event.light <= 1 || event.motion <= 1)) {
                newStateCandidate = UserPresenceState.SLEEPING
                reasonForChange = "Sleep API Classify High Confidence Sleep (Confidence: ${event.confidence})"
            }
        }

        // If newState is SLEEPING from API, this is a strong signal.
        if (newStateCandidate == UserPresenceState.SLEEPING && (reasonForChange.startsWith("Sleep API In Segment") || reasonForChange.startsWith("Sleep API Classify High Confidence Sleep"))) {
            if (oldState != UserPresenceState.SLEEPING) {
                updateUserPresenceStateInternal(newStateCandidate, reasonForChange, userPresenceHistoryRepository, serviceScope)
                updateNotificationContentOnly()
            } else {
                Log.d(TAG, "State confirmed SLEEPING by Sleep API. No change from $oldState.")
                updateNotificationContentOnly()
            }
            return
        }

        // Priority 3: Heuristic SLEEPING by bedtime schedule
        if (!isNightTime()) { // It's daytime
            if (oldState == UserPresenceState.SLEEPING && source == EvaluationSource.SCREEN_OFF) {
                newStateCandidate = UserPresenceState.AWAKE
                reasonForChange = "Heuristic: Daytime and screen off, was previously sleeping"
            }
        }

        // Final update based on determined newStateCandidate
        if (newStateCandidate != oldState) {
            updateUserPresenceStateInternal(newStateCandidate, reasonForChange, userPresenceHistoryRepository, serviceScope)
            updateNotificationContentOnly()
        } else {
            updateNotificationContentOnly()
            if (source != EvaluationSource.SCREEN_OFF) {
                Log.d(TAG, "Evaluation ended. No change from $oldState. Source: $source, Final determined reason for no change: $reasonForChange")
            }
        }
    }

    private fun isNightTime(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val schedule = currentManualSleepSchedule
        val userBedtimeInMinutes = schedule.bedtimeInMinutesTotal
        val userWakeUpTimeInMinutes = schedule.wakeUpInMinutesTotal

        // Log current time and schedule for debugging
        // Log.d(TAG, "isNightTime check: Current Time=${currentHour}:${currentMinute} ($currentTimeInMinutes mins). Schedule BedtimeMins=${userBedtimeInMinutes}, WakeupMins=${userWakeUpTimeInMinutes}")

        if (userBedtimeInMinutes != null) {
            val effectiveWakeUpTimeInMinutes = userWakeUpTimeInMinutes
                ?: ((userBedtimeInMinutes + DEFAULT_HEURISTIC_SLEEP_DURATION_HOURS * 60) % (24 * 60))

            return if (userBedtimeInMinutes <= effectiveWakeUpTimeInMinutes) {
                currentTimeInMinutes >= userBedtimeInMinutes && currentTimeInMinutes < effectiveWakeUpTimeInMinutes
            } else {
                currentTimeInMinutes >= userBedtimeInMinutes || currentTimeInMinutes < effectiveWakeUpTimeInMinutes
            }
        }
        // Default: 10 PM (22:00) to 6 AM (06:00) if no schedule set
        return currentHour >= 22 || currentHour < 6
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
                evaluateState(EvaluationSource.SCREEN_OFF)
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to Sleep API.", e)
                isSleepApiAvailable = false
                evaluateState(EvaluationSource.SCREEN_OFF)
            }
        } else {
            Log.w(TAG, "Attempted to subscribe to Sleep API, but permission is missing.")
            isSleepApiAvailable = false
            evaluateState(EvaluationSource.SCREEN_OFF)
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
                    evaluateState(EvaluationSource.SCREEN_OFF)
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unsubscribe from Sleep API updates.", e)
                    isSleepApiAvailable = false
                    evaluateState(EvaluationSource.SCREEN_OFF)
                }
        } ?: Log.d(TAG, "No active Sleep API subscription (PendingIntent was null).")
        if (sleepApiPendingIntent == null) {
            isSleepApiAvailable = false
            evaluateState(EvaluationSource.SCREEN_OFF)
        }
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
        if (_isServiceActuallyRunning && _userPresenceStateFlow.value != UserPresenceState.UNKNOWN) {
            updateUserPresenceStateInternal(UserPresenceState.UNKNOWN, "Service Destroyed", userPresenceHistoryRepository, serviceScope)
        }
        if (_isServiceActuallyRunning) {
            stopAllMonitoringLogic()
        } else {
            unregisterScreenStateReceiver()
            unsubscribeFromSleepUpdates()
        }
        serviceJob.cancel()
        Log.d(TAG, "Service Destroyed. Final state: ${_userPresenceStateFlow.value}")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}