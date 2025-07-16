package com.andrew264.habits.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import androidx.window.core.layout.WindowSizeClass
import com.andrew264.habits.ui.navigation.AppNavDisplay
import com.andrew264.habits.ui.navigation.Home
import com.andrew264.habits.ui.navigation.TopLevelBackStack
import com.andrew264.habits.ui.navigation.railItems
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.water.home.WaterHomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestInitialPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val topLevelBackStack = rememberSaveable(saver = TopLevelBackStack.Saver) {
        TopLevelBackStack(Home)
    }
    val wideNavRailState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    val waterHomeViewModel: WaterHomeViewModel = hiltViewModel()

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompact = windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

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

    Row(Modifier.fillMaxSize()) {
        AppNavigationRail(
            topLevelBackStack = topLevelBackStack,
            state = wideNavRailState,
            isCompact = isCompact,
            scope = scope
        )

        Scaffold(
            topBar = {
                MainTopAppBar(
                    topLevelBackStack = topLevelBackStack,
                    railState = wideNavRailState,
                    isCompact = isCompact,
                    scope = scope,
                    onWaterReminderClick = waterHomeViewModel::onShowReminderDialog
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
                onRequestPermissions = onRequestInitialPermissions,
                onOpenAppSettings = onOpenAppSettings,
                waterHomeViewModel = waterHomeViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun AppNavigationRailPreview() {
    val topLevelBackStack = remember { TopLevelBackStack(Home) }
    val state = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    AppNavigationRail(
        topLevelBackStack = topLevelBackStack,
        state = state,
        isCompact = false,
        scope = scope
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun AppNavigationRailContentPreview() {
    val topLevelBackStack = remember { TopLevelBackStack(Home) }
    val railState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    MaterialTheme {
        Surface {
            AppNavigationRailContent(
                topLevelBackStack = topLevelBackStack,
                railState = railState,
                isRailExpanded = true,
                isCompact = false,
                scope = scope
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppNavigationRail(
    topLevelBackStack: TopLevelBackStack,
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
                topLevelBackStack = topLevelBackStack,
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
                    modifier = Modifier.padding(start = Dimens.PaddingExtraLarge),
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
                topLevelBackStack = topLevelBackStack,
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
    topLevelBackStack: TopLevelBackStack,
    railState: WideNavigationRailState,
    isRailExpanded: Boolean,
    isCompact: Boolean,
    scope: CoroutineScope
) {
    val currentTopLevelRoute = topLevelBackStack.currentTopLevelRoute
    val view = LocalView.current

    Column(Modifier.verticalScroll(rememberScrollState())) {
        railItems.forEach { screen ->
            val selected = currentTopLevelRoute == screen
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
                    topLevelBackStack.switchTopLevel(screen)
                    if (isCompact) {
                        scope.launch { railState.collapse() }
                    }
                }
            )
        }
    }
}