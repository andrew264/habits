package com.andrew264.habits.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.andrew264.habits.ui.bedtime.BedtimeScreen
import com.andrew264.habits.ui.schedule.create.ScheduleEditorScreen
import com.andrew264.habits.ui.schedule.create.ScheduleViewModel
import com.andrew264.habits.ui.schedule.list.SchedulesScreen
import com.andrew264.habits.ui.settings.MonitoringSettingsScreen
import com.andrew264.habits.ui.usage.UsageTimelineScreen
import com.andrew264.habits.ui.usage.whitelist.WhitelistScreen
import com.andrew264.habits.ui.water.home.WaterHomeScreen
import com.andrew264.habits.ui.water.home.WaterHomeViewModel
import com.andrew264.habits.ui.water.stats.WaterStatsScreen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavDisplay(
    modifier: Modifier = Modifier,
    backStack: List<AppRoute>,
    onBack: () -> Unit,
    entryDecorators: List<NavEntryDecorator<*>>,
    snackbarHostState: SnackbarHostState,
    onNavigate: (AppRoute) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    waterHomeViewModel: WaterHomeViewModel
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { onBack() },
        entryDecorators = entryDecorators,
        entryProvider = entryProvider {
            entry<Home> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Hello World from Home Screen!",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable {}
                    )
                }
            }
            entry<Water> {
                WaterHomeScreen(
                    viewModel = waterHomeViewModel
                )
            }
            entry<WaterStats> {
                WaterStatsScreen()
            }
            entry<Schedules> {
                SchedulesScreen(
                    onNavigate = onNavigate,
                    snackbarHostState = snackbarHostState
                )
            }
            entry<MonitoringSettings> {
                MonitoringSettingsScreen(
                    onRequestPermissions = onRequestPermissions,
                    onOpenAppSettings = onOpenAppSettings
                )
            }
            entry<Usage> {
                UsageTimelineScreen(onNavigate = onNavigate)
            }
            entry<Bedtime> {
                BedtimeScreen(onNavigate = onNavigate)
            }
            entry<ScheduleEditor> { route ->
                val viewModel: ScheduleViewModel = hiltViewModel(
                    creationCallback = { factory: ScheduleViewModel.Factory ->
                        factory.create(route)
                    }
                )
                ScheduleEditorScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onNavigateUp = onBack
                )
            }
            entry<Whitelist> {
                WhitelistScreen()
            }
        },
        transitionSpec = { sharedAxisX(forward = true) },
        popTransitionSpec = { sharedAxisX(forward = false) },
        predictivePopTransitionSpec = { sharedAxisX(forward = false) }
    )
}