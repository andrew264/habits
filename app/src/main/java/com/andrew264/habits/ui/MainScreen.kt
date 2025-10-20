package com.andrew264.habits.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.SaveableStateHolderNavEntryDecorator
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.navigation.AppNavDisplay
import com.andrew264.habits.ui.navigation.Home
import com.andrew264.habits.ui.navigation.TopLevelBackStack
import com.andrew264.habits.ui.navigation.railItems
import com.andrew264.habits.ui.water.WaterViewModel

@Composable
private fun MainScreenLayout(
    topLevelBackStack: TopLevelBackStack,
    waterViewModel: WaterViewModel,
    onRequestActivityPermission: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveableStateHolder = rememberSaveableStateHolder()

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
                            contentDescription = stringResource(id = screen.title)
                        )
                    },
                    label = { Text(stringResource(id = screen.title)) },
                    interactionSource = interactionSource
                )
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) { innerPadding ->
            AppNavDisplay(
                modifier = Modifier.padding(innerPadding),
                backStack = topLevelBackStack.backStack,
                onBack = { topLevelBackStack.removeLast() },
                entryDecorators = listOf(
                    SaveableStateHolderNavEntryDecorator(saveableStateHolder),
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
        waterViewModel = waterViewModel,
        onRequestActivityPermission = onRequestActivityPermission
    )
}