package com.andrew264.habits.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.domain.scheduler.SessionAlarmScheduler
import com.andrew264.habits.receiver.SessionLimitReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionAlarmSchedulerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SessionAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "SessionAlarmScheduler"
        private const val REQUEST_CODE = 3001
    }

    override fun schedule(packageName: String, limitMinutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(limitMinutes.toLong())
        val intent = Intent(context, SessionLimitReceiver::class.java).apply {
            action = SessionLimitReceiver.ACTION_SESSION_LIMIT_ALARM
            putExtra(SessionLimitReceiver.EXTRA_PACKAGE_NAME, packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            Log.d(TAG, "Scheduled session limit alarm for $packageName in $limitMinutes minutes.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Could not schedule exact alarm. Check for SCHEDULE_EXACT_ALARM permission.", e)
        }
    }

    override fun cancel() {
        val intent = Intent(context, SessionLimitReceiver::class.java).apply {
            action = SessionLimitReceiver.ACTION_SESSION_LIMIT_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled session limit alarm.")
        }
    }
}