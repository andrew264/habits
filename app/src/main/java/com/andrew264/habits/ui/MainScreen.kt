package com.andrew264.habits.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.navigation.*
import com.andrew264.habits.ui.water.WaterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenLayout(
    topLevelBackStack: TopLevelBackStack,
    onWaterReminderClick: () -> Unit,
    waterViewModel: WaterViewModel,
    onRequestActivityPermission: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    NavigationSuiteScaffold(
        navigationItems = {
            railItems.forEach { screen ->
                val selected = topLevelBackStack.currentTopLevelRoute == screen
                val interactionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(interactionSource)
                NavigationSuiteItem(
                    selected = selected,
                    onClick = {
                        if (topLevelBackStack.currentTopLevelRoute != screen) {
                            topLevelBackStack.switchTopLevel(screen)
                        }
                    },
                    icon = {
                        Icon(
                            if (selected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = screen.title
                        )
                    },
                    label = { Text(screen.title) },
                    interactionSource = interactionSource
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    topLevelBackStack = topLevelBackStack,
                    onWaterReminderClick = onWaterReminderClick
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            AppNavDisplay(
                modifier = Modifier.padding(innerPadding),
                backStack = topLevelBackStack.backStack,
                onBack = { topLevelBackStack.removeLast() },
                entryDecorators = listOf(
                    rememberSceneSetupNavEntryDecorator(),
                    rememberSavedStateNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                snackbarHostState = snackbarHostState,
                onNavigate = { topLevelBackStack.add(it) },
                waterViewModel = waterViewModel,
                onRequestActivityPermission = onRequestActivityPermission
            )
        }
    }
}


@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestInitialPermissions: () -> Unit,
    onRequestActivityPermission: () -> Unit,
) {
    val topLevelBackStack = rememberSaveable(saver = TopLevelBackStack.Saver) {
        TopLevelBackStack(Home)
    }
    val uiState by viewModel.uiState.collectAsState()
    val waterViewModel: WaterViewModel = hiltViewModel()

    // Handle one-time navigation events from the ViewModel
    LaunchedEffect(uiState.destinationRoute) {
        val routeStr = uiState.destinationRoute
        if (routeStr != null) {
            val topLevelRoute = railItems.find { it.javaClass.simpleName == routeStr.split("/").first() }
            if (topLevelRoute != null) {
                topLevelBackStack.switchTopLevel(topLevelRoute)
            }
            viewModel.onRouteConsumed()
        }
    }

    // Handle initial permission check
    LaunchedEffect(Unit) {
        if (viewModel.needsInitialPermissionCheck()) {
            onRequestInitialPermissions()
        }
    }

    MainScreenLayout(
        topLevelBackStack = topLevelBackStack,
        onWaterReminderClick = waterViewModel::onShowReminderDialog,
        waterViewModel = waterViewModel,
        onRequestActivityPermission = onRequestActivityPermission
    )
}