package com.andrew264.habits.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.andrew264.habits.presentation.UserPresenceControlScreen
import com.andrew264.habits.presentation.SetBedtimeScreen


@Composable
fun ContainerGraph(
    navController: NavHostController,
    onRequestPermissions: () -> Unit,
    onStartWithSleepApi: () -> Unit,
    onStartWithHeuristics: () -> Unit,
    onStopService: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSetBedtime: (hour: Int, minute: Int) -> Unit,
    onClearBedtime: () -> Unit,
    onSetWakeUpTime: (hour: Int, minute: Int) -> Unit,
    onClearWakeUpTime: () -> Unit
){
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxHeight()
    ){
        composable(route = Screen.Home.route) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Hello World from Home Screen!",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        // navController.navigate(Screen.PermissionSettings.route)
                    }
                )
            }
        }
        composable(route = Screen.PermissionSettings.route) {
            UserPresenceControlScreen(
                onRequestPermissions = onRequestPermissions,
                onStartWithSleepApi = onStartWithSleepApi,
                onStartWithHeuristics = onStartWithHeuristics,
                onStopService = onStopService,
                onOpenAppSettings = onOpenAppSettings
            )
        }

        composable(route = Screen.SetSleepTime.route) {
            SetBedtimeScreen(
                onSetBedtime = onSetBedtime,
                onClearBedtime = onClearBedtime,
                onSetWakeUpTime = onSetWakeUpTime,
                onClearWakeUpTime = onClearWakeUpTime
            )
        }

        composable(route = "TEST") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hello test", modifier = Modifier.clickable { navController.navigateUp() })
            }
        }
    }
}