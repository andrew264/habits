package com.andrew264.habits

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.andrew264.habits.ui.MainScreen
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
            PermissionHandler(this) { permissions ->
                viewModel.handlePermissionResults(permissions)
            }

        setContent {
            HabitsTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestInitialPermissions = {
                        val pnPermission = Manifest.permission.POST_NOTIFICATIONS
                        if (ContextCompat.checkSelfPermission(this, pnPermission) != PackageManager.PERMISSION_GRANTED) {
                            permissionHandler.requestPermissions(listOf(pnPermission))
                        }
                    },
                    onRequestActivityPermission = {
                        permissionHandler.requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION))
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }
}