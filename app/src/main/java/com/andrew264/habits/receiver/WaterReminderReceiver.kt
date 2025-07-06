package com.andrew264.habits.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.andrew264.habits.MainActivity
import com.andrew264.habits.R
import com.andrew264.habits.domain.analyzer.ScheduleAnalyzer
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.domain.repository.WaterRepository
import com.andrew264.habits.domain.scheduler.WaterAlarmScheduler
import com.andrew264.habits.model.UserPresenceState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WaterReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var waterRepository: WaterRepository

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    @Inject
    lateinit var waterAlarmScheduler: WaterAlarmScheduler

    @Inject
    lateinit var userPresenceHistoryRepository: UserPresenceHistoryRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "WaterReminderReceiver"
        const val ACTION_WATER_REMINDER_ALARM = "com.andrew264.habits.action.WATER_REMINDER_ALARM"
        const val ACTION_LOG_WATER_QUICK = "com.andrew264.habits.action.LOG_WATER_QUICK"
        const val ACTION_SNOOZE_WATER_REMINDER = "com.andrew264.habits.action.SNOOZE_WATER_REMINDER"

        private const val NOTIFICATION_CHANNEL_ID = "WaterReminderChannel"
        private const val NOTIFICATION_ID = 2
        private const val QUICK_ADD_AMOUNT_ML = 250
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        Log.d(TAG, "Received action: ${intent.action}")
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (intent.action) {
                    ACTION_WATER_REMINDER_ALARM -> handleReminderAlarm(context)
                    ACTION_LOG_WATER_QUICK -> handleLogWaterAction(context)
                    ACTION_SNOOZE_WATER_REMINDER -> handleSnoozeAction(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminderAlarm(context: Context) {
        val settings = settingsRepository.settingsFlow.first()

        // --- Condition Checks ---
        if (!settings.isWaterTrackingEnabled || !settings.isWaterReminderEnabled) {
            Log.d(TAG, "Aborting reminder: Feature disabled in settings.")
            waterAlarmScheduler.cancelReminders() // Clean up alarms if feature is off
            return
        }

        val currentUserState = userPresenceHistoryRepository.userPresenceState.value
        if (currentUserState != UserPresenceState.AWAKE) {
            Log.d(TAG, "Aborting reminder: User is not AWAKE. Current state: $currentUserState")
            // Reschedule for later, so we don't miss reminders completely
            waterAlarmScheduler.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
            return
        }

        val schedule = settings.waterReminderScheduleId?.let { scheduleRepository.getSchedule(it).first() }
        val analyzer = schedule?.let { ScheduleAnalyzer(it.groups) }
        if (analyzer?.isCurrentTimeInSchedule() == false) {
            Log.d(TAG, "Aborting reminder: Current time is outside the selected schedule.")
            waterAlarmScheduler.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
            return
        }

        // --- Show Notification ---
        Log.d(TAG, "All conditions met. Showing reminder notification.")
        showReminderNotification(context, settings.waterReminderSnoozeMinutes)

        // --- Reschedule ---
        waterAlarmScheduler.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
    }

    private suspend fun handleLogWaterAction(context: Context) {
        waterRepository.logWater(QUICK_ADD_AMOUNT_ML)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Logged $QUICK_ADD_AMOUNT_ML ml from notification action.")
        // Re-schedule next reminder based on settings
        val settings = settingsRepository.settingsFlow.first()
        if (settings.isWaterReminderEnabled) {
            waterAlarmScheduler.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
        }
    }

    private suspend fun handleSnoozeAction(context: Context) {
        val settings = settingsRepository.settingsFlow.first()
        waterAlarmScheduler.handleSnooze(settings.waterReminderSnoozeMinutes.toLong())
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Snoozed reminder from notification action.")
    }

    private fun showReminderNotification(
        context: Context,
        snoozeMinutes: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        // Content Intent to open the app to the water screen
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("destination_route", "Water")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            "Water".hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Log Action
        val logIntent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_LOG_WATER_QUICK
        }
        val logPendingIntent = PendingIntent.getBroadcast(
            context,
            ACTION_LOG_WATER_QUICK.hashCode(),
            logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action
        val snoozeIntent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_WATER_REMINDER
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            ACTION_SNOOZE_WATER_REMINDER.hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop_24)
            .setContentTitle("Time to Hydrate!")
            .setContentText("Don't forget to drink some water.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_add_24, "Log ${QUICK_ADD_AMOUNT_ML}ml", logPendingIntent)
            .addAction(R.drawable.ic_snooze_24, "Snooze ($snoozeMinutes min)", snoozePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Water Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to drink water"
        }
        notificationManager.createNotificationChannel(channel)
    }
}