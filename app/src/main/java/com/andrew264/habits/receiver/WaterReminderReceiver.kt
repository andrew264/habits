package com.andrew264.habits.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.andrew264.habits.R
import com.andrew264.habits.domain.analyzer.ScheduleAnalyzer
import com.andrew264.habits.domain.manager.WaterReminderManager
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.repository.ScheduleRepository
import com.andrew264.habits.repository.SettingsRepository
import com.andrew264.habits.repository.WaterRepository
import com.andrew264.habits.service.UserPresenceService
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
    lateinit var waterReminderManager: WaterReminderManager

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
            waterReminderManager.cancelReminders() // Clean up alarms if feature is off
            return
        }

        if (UserPresenceService.userPresenceState.value != UserPresenceState.AWAKE) {
            Log.d(TAG, "Aborting reminder: User is not AWAKE. Current state: ${UserPresenceService.userPresenceState.value}")
            // Reschedule for later, so we don't miss reminders completely
            waterReminderManager.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
            return
        }

        val schedule = settings.waterReminderScheduleId?.let { scheduleRepository.getSchedule(it).first() }
        val analyzer = schedule?.let { ScheduleAnalyzer(it.groups) }
        if (analyzer?.isCurrentTimeInSchedule() == false) {
            Log.d(TAG, "Aborting reminder: Current time is outside the selected schedule.")
            waterReminderManager.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
            return
        }

        // --- Show Notification ---
        Log.d(TAG, "All conditions met. Showing reminder notification.")
        showReminderNotification(context, settings.waterReminderSnoozeMinutes)

        // --- Reschedule ---
        waterReminderManager.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
    }

    private suspend fun handleLogWaterAction(context: Context) {
        waterRepository.logWater(QUICK_ADD_AMOUNT_ML)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Logged $QUICK_ADD_AMOUNT_ML ml from notification action.")
        // Re-schedule next reminder based on settings
        val settings = settingsRepository.settingsFlow.first()
        if (settings.isWaterReminderEnabled) {
            waterReminderManager.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
        }
    }

    private suspend fun handleSnoozeAction(context: Context) {
        val settings = settingsRepository.settingsFlow.first()
        waterReminderManager.handleSnooze(settings.waterReminderSnoozeMinutes.toLong())
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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper water drop icon
            .setContentTitle("Time to Hydrate!")
            .setContentText("Don't forget to drink some water to stay on track with your goal.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Log ${QUICK_ADD_AMOUNT_ML}ml", logPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Snooze ($snoozeMinutes min)", snoozePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Water Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to drink water"
        }
        notificationManager.createNotificationChannel(channel)
    }
}