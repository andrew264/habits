package com.andrew264.habits.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

sealed interface TopLevelRoute : AppRoute {
    val title: String
    val selectedIcon: ImageVector
    val unselectedIcon: ImageVector
}

@Serializable
data object Home : TopLevelRoute {
    override val title = "Home"
    override val selectedIcon = Icons.Filled.Home
    override val unselectedIcon = Icons.Outlined.Home
}

@Serializable
data object Water : TopLevelRoute {
    override val title = "Water"
    override val selectedIcon = Icons.Filled.WaterDrop
    override val unselectedIcon = Icons.Outlined.WaterDrop
}

@Serializable
data object Schedules : TopLevelRoute {
    override val title = "Schedules"
    override val selectedIcon = Icons.Filled.Schedule
    override val unselectedIcon = Icons.Outlined.Schedule
}

@Serializable
data object Usage : TopLevelRoute {
    override val title = "Usage"
    override val selectedIcon = Icons.Filled.Timeline
    override val unselectedIcon = Icons.Outlined.Timeline
}

@Serializable
data object MonitoringSettings : TopLevelRoute {
    override val title = "Monitoring"
    override val selectedIcon = Icons.Filled.Settings
    override val unselectedIcon = Icons.Outlined.Settings
}

@Serializable
data object Bedtime : TopLevelRoute {
    override val title = "Bedtime"
    override val selectedIcon = Icons.Filled.Alarm
    override val unselectedIcon = Icons.Outlined.Alarm
}

@Serializable
data object WaterStats : AppRoute

@Serializable
data class ScheduleEditor(val scheduleId: String?) : AppRoute

@Serializable
data object Whitelist : AppRoute

val railItems: List<TopLevelRoute> = listOf(
    Home,
    Water,
    Usage,
    Bedtime,
    Schedules,
    MonitoringSettings
)