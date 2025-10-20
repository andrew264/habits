package com.andrew264.habits

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.andrew264.habits.ui.MainScreen
import com.andrew264.habits.ui.MainViewModel
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            lifecycleScope.launch {
                permissionManager.onResult(permissions)
            }
        }

        viewModel.handleIntent(intent)

        setContent {
            HabitsTheme {
                LaunchedEffect(permissionManager) {
                    permissionManager.requests.collect { permissions ->
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                }
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }
}