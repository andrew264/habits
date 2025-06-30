package com.andrew264.habits.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen(
        route = "home_screen",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Water : Screen(
        route = "water_screen",
        title = "Water",
        selectedIcon = Icons.Filled.WaterDrop,
        unselectedIcon = Icons.Outlined.WaterDrop
    )

    object Schedules : Screen(
        route = "schedules_screen",
        title = "Schedules",
        selectedIcon = Icons.Filled.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    )

    object PermissionSettings : Screen(
        route = "permission_settings_screen",
        title = "Permissions",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    object Bedtime : Screen(
        route = "bedtime_screen",
        title = "Bedtime",
        selectedIcon = Icons.Filled.Alarm,
        unselectedIcon = Icons.Outlined.Alarm
    )
}

val railItems = listOf(
    Screen.Home,
    Screen.Water,
    Screen.Schedules,
    Screen.PermissionSettings,
    Screen.Bedtime
)