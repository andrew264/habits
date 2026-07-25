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
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WaterRepository
import com.andrew264.habits.domain.scheduler.WaterAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class WaterReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var waterRepository: WaterRepository

    @Inject
    lateinit var waterAlarmScheduler: WaterAlarmScheduler

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

        if (!settings.isWaterTrackingEnabled || !settings.isWaterReminderEnabled) {
            Log.d(TAG, "Aborting reminder: Feature disabled in settings.")
            waterAlarmScheduler.cancelReminders()
            return
        }

        val now = LocalTime.now()
        val currentMinuteOfDay = now.hour * 60 + now.minute
        val start = settings.waterReminderStartMinuteOfDay
        val end = settings.waterReminderEndMinuteOfDay

        val isInTimeWindow = if (start <= end) {
            currentMinuteOfDay in start..end
        } else {
            // Overnight window (e.g., 10 PM to 6 AM)
            currentMinuteOfDay >= start || currentMinuteOfDay <= end
        }

        if (!isInTimeWindow) {
            Log.d(TAG, "Aborting reminder: Current time ($currentMinuteOfDay min) is outside window [$start, $end].")
            waterAlarmScheduler.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
            return
        }

        Log.d(TAG, "All conditions met. Showing reminder notification.")
        showReminderNotification(context, settings.waterReminderSnoozeMinutes)

        waterAlarmScheduler.scheduleNextReminder(settings.waterReminderIntervalMinutes.toLong())
    }

    private suspend fun handleLogWaterAction(context: Context) {
        waterRepository.logWater(QUICK_ADD_AMOUNT_ML)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Logged $QUICK_ADD_AMOUNT_ML ml from notification action.")

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

        val logIntent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_LOG_WATER_QUICK
        }
        val logPendingIntent = PendingIntent.getBroadcast(
            context,
            ACTION_LOG_WATER_QUICK.hashCode(),
            logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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