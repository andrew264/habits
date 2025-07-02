package com.andrew264.habits

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.andrew264.habits.ui.ContainerScreen
import com.andrew264.habits.ui.MainViewModel
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.util.PermissionHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var permissionHandler: PermissionHandler
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.handleIntent(intent)

        permissionHandler =
            PermissionHandler(this) { activityRecognitionGranted, notificationsGranted ->
                viewModel.handlePermissionResults(activityRecognitionGranted, notificationsGranted)
            }

        setContent {
            HabitsTheme {
                ContainerScreen(
                    viewModel = viewModel,
                    onRequestPermissions = { permissionHandler.requestRelevantPermissions() },
                    onOpenAppSettings = { openAppSettings() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}