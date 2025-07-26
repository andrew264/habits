package com.andrew264.habits.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.domain.usecase.CheckUsageLimitsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SessionLimitReceiver : BroadcastReceiver() {

    @Inject
    lateinit var checkUsageLimitsUseCase: CheckUsageLimitsUseCase

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "SessionLimitReceiver"
        const val ACTION_SESSION_LIMIT_ALARM = "com.andrew264.habits.action.SESSION_LIMIT_ALARM"
        const val EXTRA_PACKAGE_NAME = "com.andrew264.habits.extra.PACKAGE_NAME"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SESSION_LIMIT_ALARM) return

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (packageName == null) {
            Log.w(TAG, "Received session limit alarm with no package name.")
            return
        }

        Log.d(TAG, "Session limit alarm received for package: $packageName")
        val pendingResult = goAsync()

        scope.launch {
            try {
                checkUsageLimitsUseCase.checkSessionLimitFromAlarm(packageName)
            } finally {
                pendingResult.finish()
            }
        }
    }
}