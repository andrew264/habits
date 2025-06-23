package com.andrew264.habits.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.andrew264.habits.navigation.ContainerGraph
import com.andrew264.habits.navigation.railItems
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContainerScreen(
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSetBedtime: (hour: Int, minute: Int) -> Unit,
    onClearBedtime: () -> Unit,
    onSetWakeUpTime: (hour: Int, minute: Int) -> Unit,
    onClearWakeUpTime: () -> Unit
){
    val navController = rememberNavController()
    val wideNavRailState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { scaffoldInternalPadding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldInternalPadding)
        ) {
            WideNavigationRail(
                state = wideNavRailState,
                header = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (wideNavRailState.targetValue == WideNavigationRailValue.Expanded) {
                                    wideNavRailState.collapse()
                                } else {
                                    wideNavRailState.expand()
                                }
                            }
                        },
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = if (wideNavRailState.targetValue == WideNavigationRailValue.Expanded) {
                                Icons.AutoMirrored.Filled.MenuOpen
                            } else {
                                Icons.Filled.Menu
                            },
                            contentDescription = if (wideNavRailState.targetValue == WideNavigationRailValue.Expanded) {
                                "Collapse rail"
                            } else {
                                "Expand rail"
                            }
                        )
                    }
                }
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                railItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    WideNavigationRailItem(
                        railExpanded = wideNavRailState.targetValue == WideNavigationRailValue.Expanded,
                        icon = {
                            Icon(
                                imageVector = if (selected) {
                                    screen.selectedIcon
                                } else {
                                    screen.unselectedIcon
                                },
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }

            ContainerGraph(
                navController = navController,
                onRequestPermissions = onRequestPermissions,
                onOpenAppSettings = onOpenAppSettings,
                onSetBedtime = onSetBedtime,
                onClearBedtime = onClearBedtime,
                onSetWakeUpTime = onSetWakeUpTime,
                onClearWakeUpTime = onClearWakeUpTime
            )
        }
    }
}