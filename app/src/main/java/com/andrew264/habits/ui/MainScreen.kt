package com.andrew264.habits.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.SaveableStateHolderNavEntryDecorator
import com.andrew264.habits.ui.common.SnackbarHandler
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.navigation.AppNavDisplay
import com.andrew264.habits.ui.navigation.Home
import com.andrew264.habits.ui.navigation.TopLevelBackStack
import com.andrew264.habits.ui.navigation.railItems

@Composable
private fun MainScreenLayout(
    topLevelBackStack: TopLevelBackStack,
    snackbarHostState: SnackbarHostState
) {
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
                onNavigate = { topLevelBackStack.add(it) }
            )
        }
    }
}


@Composable
fun MainScreen(
    viewModel: MainViewModel,
) {
    val topLevelBackStack = rememberSaveable(saver = TopLevelBackStack.Saver) {
        TopLevelBackStack(Home)
    }
    val uiState by viewModel.uiState.collectAsState()

    val snackbarViewModel: SnackbarViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarHandler(snackbarManager = snackbarViewModel.snackbarManager, snackbarHostState = snackbarHostState)

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
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (viewModel.needsInitialPermissionCheck() && ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            viewModel.requestInitialPermissions()
        }
    }

    MainScreenLayout(
        topLevelBackStack = topLevelBackStack,
        snackbarHostState = snackbarHostState
    )
}