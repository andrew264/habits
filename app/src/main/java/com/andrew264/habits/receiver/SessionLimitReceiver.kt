package com.andrew264.habits.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.domain.manager.SnoozeManager
import com.andrew264.habits.domain.usecase.CheckUsageLimitsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SessionLimitReceiver : BroadcastReceiver() {

    @Inject
    lateinit var checkUsageLimitsUseCase: CheckUsageLimitsUseCase

    @Inject
    lateinit var snoozeManager: SnoozeManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "SessionLimitReceiver"
        const val ACTION_SESSION_LIMIT_ALARM = "com.andrew264.habits.action.SESSION_LIMIT_ALARM"
        const val ACTION_SNOOZE_SESSION = "com.andrew264.habits.action.SNOOZE_SESSION"
        const val EXTRA_PACKAGE_NAME = "com.andrew264.habits.extra.PACKAGE_NAME"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (packageName == null) {
            Log.w(TAG, "Received session limit action with no package name.")
            return
        }

        val pendingResult = goAsync()

        scope.launch {
            try {
                when (intent.action) {
                    ACTION_SESSION_LIMIT_ALARM -> {
                        Log.d(TAG, "Session limit alarm received for package: $packageName")
                        checkUsageLimitsUseCase.checkSessionLimitFromAlarm(packageName)
                    }

                    ACTION_SNOOZE_SESSION -> {
                        Log.d(TAG, "Snoozing session for package: $packageName")
                        snoozeManager.snoozeApp(packageName, TimeUnit.MINUTES.toMillis(5))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
