package com.andrew264.habits.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.andrew264.habits.ui.bedtime.BedtimeScreen
import com.andrew264.habits.ui.schedule.ScheduleEditorScreen
import com.andrew264.habits.ui.schedule.SchedulesScreen
import com.andrew264.habits.ui.permissions.UserPresenceControlScreen


@Composable
fun ContainerGraph(
    navController: NavHostController,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxHeight()
    ) {
        composable(route = Screen.Home.route) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Hello World from Home Screen!",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
//                        navController.navigate(Screen.Schedules.route)
                    }
                )
            }
        }
        composable(route = Screen.Schedules.route) {
            SchedulesScreen(navController = navController)
        }
        composable(route = Screen.PermissionSettings.route) {
            UserPresenceControlScreen(
                onRequestPermissions = onRequestPermissions,
                onOpenAppSettings = onOpenAppSettings
            )
        }

        composable(route = Screen.Bedtime.route) {
            BedtimeScreen()
        }

        composable(
            route = "schedule_editor?scheduleId={scheduleId}",
            arguments = listOf(navArgument("scheduleId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            ScheduleEditorScreen(onNavigateUp = { navController.navigateUp() })
        }

        composable(route = "TEST") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hello test", modifier = Modifier.clickable { navController.navigateUp() })
            }
        }
    }
}