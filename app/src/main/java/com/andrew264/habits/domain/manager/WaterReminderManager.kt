package com.andrew264.habits.domain.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.receiver.WaterReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "WaterReminderManager"
        private const val REMINDER_REQUEST_CODE = 2001
    }

    fun scheduleNextReminder(intervalMinutes: Long) {
        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(intervalMinutes)
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_WATER_REMINDER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled next water reminder for $intervalMinutes minutes from now.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Could not schedule exact alarm. Check for SCHEDULE_EXACT_ALARM permission if required on API 31+.", e)
            // Fallback or notify user if this permission becomes necessary
        }
    }

    fun handleSnooze(snoozeMinutes: Long) {
        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(snoozeMinutes)
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_WATER_REMINDER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        Log.d(TAG, "Snoozed water reminder for $snoozeMinutes minutes.")
    }

    fun cancelReminders() {
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_WATER_REMINDER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled water reminders.")
        }
    }
}