package com.andrew264.habits

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.andrew264.habits.presentation.ContainerScreen
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.util.PermissionHandler
import com.andrew264.habits.manager.UserPresenceController

class MainActivity : ComponentActivity() {

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var userPresenceController: UserPresenceController
    private var initialPermissionCheckDone = false

    companion object {
        private const val KEY_INITIAL_PERMISSION_CHECK_DONE = "initialPermissionCheckDone"
    }

    // --- Lifecycle Methods ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userPresenceController = UserPresenceController(applicationContext)

        permissionHandler = PermissionHandler(this) { activityRecognitionGranted, notificationsGranted ->
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
                    onStartWithSleepApi = {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                            userPresenceController.startServiceWithSleepApi()
                        } else {
                            Toast.makeText(this@MainActivity, "Activity Recognition permission not granted. Please grant permission first or use Heuristics.", Toast.LENGTH_LONG).show()
                            permissionHandler.requestRelevantPermissions()
                        }
                    },
                    onStartWithHeuristics = { userPresenceController.startServiceWithHeuristics() },
                    onStopService = { userPresenceController.stopService() },
                    onOpenAppSettings = { openAppSettings() },
                    onSetBedtime = { hour, minute -> userPresenceController.setManualBedtime(hour, minute) },
                    onClearBedtime = { userPresenceController.clearManualBedtime() },
                    onSetWakeUpTime = { hour, minute -> userPresenceController.setManualWakeUpTime(hour, minute) },
                    onClearWakeUpTime = { userPresenceController.clearManualWakeUpTime() }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_INITIAL_PERMISSION_CHECK_DONE, initialPermissionCheckDone)
    }

    // --- Permission Handling ---
    private fun handlePermissionResults(activityRecognitionGranted: Boolean, notificationsGranted: Boolean) {
        userPresenceController.handleInitialServiceStart(activityRecognitionGranted)

        if (activityRecognitionGranted) {
            Toast.makeText(this, "Activity Recognition permission granted.", Toast.LENGTH_SHORT).show()
        }

        if (!notificationsGranted) {
            Toast.makeText(this, "Notification permission denied. Service notifications might not show.", Toast.LENGTH_LONG).show()
        }
        initialPermissionCheckDone = true
    }

    // --- UI Helper Methods ---
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}