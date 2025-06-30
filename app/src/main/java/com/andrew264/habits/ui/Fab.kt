package com.andrew264.habits.ui

import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.andrew264.habits.ui.navigation.Screen
import com.andrew264.habits.ui.schedule.ScheduleViewMode
import com.andrew264.habits.ui.schedule.ScheduleViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainFab(
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val view = LocalView.current

    when (currentRoute) {
        Screen.Schedules.route -> {
            SmallExtendedFloatingActionButton(
                onClick = {
                    navController.navigate("schedule_editor")
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "New Schedule") },
                text = { Text("New Schedule") }
            )
        }

        "schedule_editor?scheduleId={scheduleId}" -> {
            val viewModel: ScheduleViewModel = hiltViewModel(navBackStackEntry!!)
            val viewMode by viewModel.viewMode.collectAsState()

            if (viewMode == ScheduleViewMode.GROUPED) {
                SmallExtendedFloatingActionButton(
                    onClick = {
                        viewModel.addGroup()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create New Group"
                        )
                    },
                    text = { Text(text = "New Group") }
                )
            }
        }
    }
}