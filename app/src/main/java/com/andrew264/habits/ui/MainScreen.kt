package com.andrew264.habits.ui

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.andrew264.habits.ui.navigation.ContainerGraph
import com.andrew264.habits.ui.navigation.railItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun ContainerScreen(
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val navController = rememberNavController()
    val wideNavRailState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isCompact =
        calculateWindowSizeClass(activity = LocalActivity.current as Activity).widthSizeClass == WindowWidthSizeClass.Compact

    Row(Modifier.fillMaxSize()) {
        AppNavigationRail(
            navController = navController,
            state = wideNavRailState,
            isCompact = isCompact,
            scope = scope
        )

        Scaffold(
            topBar = {
                MainTopAppBar(
                    navController = navController,
                    railState = wideNavRailState,
                    isCompact = isCompact,
                    scope = scope
                )
            },
            floatingActionButton = {
                MainFab(navController = navController)
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            ContainerGraph(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                snackbarHostState = snackbarHostState,
                onRequestPermissions = onRequestPermissions,
                onOpenAppSettings = onOpenAppSettings
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppNavigationRail(
    navController: NavHostController,
    state: WideNavigationRailState,
    isCompact: Boolean,
    scope: CoroutineScope
) {
    val view = LocalView.current
    if (isCompact) {
        ModalWideNavigationRail(
            state = state,
            hideOnCollapse = true
        ) {
            AppNavigationRailContent(
                navController = navController,
                railState = state,
                isRailExpanded = true,
                isCompact = true,
                scope = scope
            )
        }
    } else {
        WideNavigationRail(
            state = state,
            header = {
                val expanded = state.targetValue == WideNavigationRailValue.Expanded
                IconButton(
                    onClick = {
                        scope.launch { if (expanded) state.collapse() else state.expand() }
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    modifier = Modifier.padding(start = 24.dp),
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Crossfade(targetState = expanded, label = "MenuIconCrossfade") { isExpanded ->
                        if (isExpanded) {
                            Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = "Collapse rail")
                        } else {
                            Icon(Icons.Filled.Menu, contentDescription = "Expand rail")
                        }
                    }
                }
            }
        ) {
            val expanded = state.targetValue == WideNavigationRailValue.Expanded
            AppNavigationRailContent(
                navController = navController,
                railState = state,
                isRailExpanded = expanded,
                isCompact = false,
                scope = scope
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppNavigationRailContent(
    navController: NavHostController,
    railState: WideNavigationRailState,
    isRailExpanded: Boolean,
    isCompact: Boolean,
    scope: CoroutineScope
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val view = LocalView.current

    Column(Modifier.verticalScroll(rememberScrollState())) {
        railItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            WideNavigationRailItem(
                railExpanded = isRailExpanded,
                icon = {
                    Icon(
                        if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = selected,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    if (isCompact) {
                        scope.launch { railState.collapse() }
                    }
                }
            )
        }
    }
}