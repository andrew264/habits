package com.andrew264.habits.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.andrew264.habits.ui.navigation.Screen
import com.andrew264.habits.ui.navigation.railItems
import com.andrew264.habits.ui.schedule.create.ScheduleViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainTopAppBar(
    navController: NavHostController,
    railState: WideNavigationRailState,
    isCompact: Boolean,
    scope: CoroutineScope
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val view = LocalView.current

    // Hide TopAppBar only if route is invalid
    if (currentRoute == null) {
        return
    }

    val topLevelScreen = railItems.find { it.route == currentRoute }
    val isTopLevelScreen = topLevelScreen != null

    val title: String = topLevelScreen?.title ?: when (currentRoute) {
        "water_settings" -> "Water Tracking Settings"
        "water_stats" -> "Hydration Statistics"
        else -> "" // Schedule editor title is handled dynamically below
    }

    val navigationIcon: @Composable () -> Unit = if (!isTopLevelScreen) {
        {
            IconButton(onClick = {
                navController.navigateUp()
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    } else if (isCompact) {
        {
            IconButton(
                onClick = {
                    scope.launch { railState.expand() }
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Open Navigation")
            }
        }
    } else {
        {}
    }

    if (currentRoute.startsWith("schedule_editor")) {
        // Special handling for ScheduleEditorScreen as it has dynamic title and actions
        val viewModel: ScheduleViewModel = hiltViewModel(navBackStackEntry!!)
        val uiState by viewModel.uiState.collectAsState()

        TopAppBar(
            title = {
                Text(
                    text = if (uiState.isNewSchedule) "Create New Schedule" else "Update: ${uiState.schedule?.name.orEmpty()}",
                    fontWeight = FontWeight.Medium
                )
            },
            navigationIcon = navigationIcon,
            actions = {
                FilledTonalButton(
                    onClick = {
                        viewModel.saveSchedule()
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Save Schedule",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Save", fontWeight = FontWeight.Medium)
                }
            }
        )
    } else {
        // Standard TopAppBar for other screens
        TopAppBar(
            title = { Text(title) },
            navigationIcon = navigationIcon,
            actions = {
                if (currentRoute == Screen.Water.route) {
                    IconButton(onClick = {
                        navController.navigate("water_stats")
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Hydration Statistics")
                    }
                    IconButton(onClick = {
                        navController.navigate("water_settings")
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Water Settings")
                    }
                }
            }
        )
    }
}