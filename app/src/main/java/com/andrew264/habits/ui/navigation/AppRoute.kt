package com.andrew264.habits.ui.navigation

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey, Parcelable

sealed interface TopLevelRoute : AppRoute {
    val title: String
    val selectedIcon: ImageVector
    val unselectedIcon: ImageVector
}

@Parcelize
@Serializable
data object Home : TopLevelRoute {
    @IgnoredOnParcel
    override val title = "Home"
    @IgnoredOnParcel
    override val selectedIcon = Icons.Filled.Home
    @IgnoredOnParcel
    override val unselectedIcon = Icons.Outlined.Home
}

@Parcelize
@Serializable
data object Water : TopLevelRoute {
    @IgnoredOnParcel
    override val title = "Water"
    @IgnoredOnParcel
    override val selectedIcon = Icons.Filled.WaterDrop
    @IgnoredOnParcel
    override val unselectedIcon = Icons.Outlined.WaterDrop
}

@Parcelize
@Serializable
data object Schedules : TopLevelRoute {
    @IgnoredOnParcel
    override val title = "Schedules"
    @IgnoredOnParcel
    override val selectedIcon = Icons.Filled.Schedule
    @IgnoredOnParcel
    override val unselectedIcon = Icons.Outlined.Schedule
}

@Parcelize
@Serializable
data object Usage : TopLevelRoute {
    @IgnoredOnParcel
    override val title = "Usage"
    @IgnoredOnParcel
    override val selectedIcon = Icons.Filled.Timeline
    @IgnoredOnParcel
    override val unselectedIcon = Icons.Outlined.Timeline
}

@Parcelize
@Serializable
data object MonitoringSettings : TopLevelRoute {
    @IgnoredOnParcel
    override val title = "Monitoring"
    @IgnoredOnParcel
    override val selectedIcon = Icons.Filled.Settings
    @IgnoredOnParcel
    override val unselectedIcon = Icons.Outlined.Settings
}

@Parcelize
@Serializable
data object Bedtime : TopLevelRoute {
    @IgnoredOnParcel
    override val title = "Bedtime"
    @IgnoredOnParcel
    override val selectedIcon = Icons.Filled.Alarm
    @IgnoredOnParcel
    override val unselectedIcon = Icons.Outlined.Alarm
}

@Parcelize
@Serializable
data object WaterStats : AppRoute

@Parcelize
@Serializable
data class ScheduleEditor(val scheduleId: String?) : AppRoute

@Parcelize
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