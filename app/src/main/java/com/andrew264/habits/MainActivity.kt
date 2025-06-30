package com.andrew264.habits

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.andrew264.habits.data.repository.SettingsRepository
import com.andrew264.habits.manager.UserPresenceController
import com.andrew264.habits.presentation.ContainerScreen
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.util.PermissionHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var permissionHandler: PermissionHandler

    @Inject
    lateinit var userPresenceController: UserPresenceController

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var initialPermissionCheckDone = false

    companion object {
        private const val KEY_INITIAL_PERMISSION_CHECK_DONE = "initialPermissionCheckDone"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionHandler =
            PermissionHandler(this) { activityRecognitionGranted, notificationsGranted ->
                handlePermissionResults(activityRecognitionGranted, notificationsGranted)
            }

        savedInstanceState?.let {
            initialPermissionCheckDone = it.getBoolean(KEY_INITIAL_PERMISSION_CHECK_DONE, false)
        }

        if (!initialPermissionCheckDone) {
            permissionHandler.requestRelevantPermissions()
        }

        setContent {
            HabitsTheme {
                ContainerScreen(
                    onRequestPermissions = { permissionHandler.requestRelevantPermissions() },
                    onOpenAppSettings = { openAppSettings() }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_INITIAL_PERMISSION_CHECK_DONE, initialPermissionCheckDone)
    }

    private fun handlePermissionResults(
        activityRecognitionGranted: Boolean,
        notificationsGranted: Boolean
    ) {
        if (activityRecognitionGranted) {
            lifecycleScope.launch {
                if (settingsRepository.settingsFlow.first().isServiceActive) {
                    Log.d(
                        "MainActivity",
                        "Activity permission granted and service should be active. Ensuring service (re)starts."
                    )
                    userPresenceController.startService()
                }
            }
        }

        if (!notificationsGranted) {
            Log.d(
                "MainActivity",
                "Notification permission denied. Service notifications might not show or service may not run reliably."
            )
        }
        initialPermissionCheckDone = true
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}