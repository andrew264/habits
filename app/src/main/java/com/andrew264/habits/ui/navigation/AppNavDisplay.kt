package com.andrew264.habits.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.andrew264.habits.ui.bedtime.BedtimeScreen
import com.andrew264.habits.ui.bedtime.BedtimeSettingsScreen
import com.andrew264.habits.ui.privacy.DataManagementScreen
import com.andrew264.habits.ui.schedule.SchedulesListDetailScreen
import com.andrew264.habits.ui.settings.SettingsScreen
import com.andrew264.habits.ui.usage.UsageSettingsScreen
import com.andrew264.habits.ui.usage.UsageStatsScreen
import com.andrew264.habits.ui.usage.whitelist.WhitelistScreen
import com.andrew264.habits.ui.water.WaterScreen
import com.andrew264.habits.ui.water.WaterSettingsScreen
import com.andrew264.habits.ui.water.WaterStatsScreen
import com.andrew264.habits.ui.water.WaterViewModel

@Composable
fun AppNavDisplay(
    modifier: Modifier = Modifier,
    backStack: List<AppRoute>,
    onBack: (Int) -> Unit,
    entryDecorators: List<NavEntryDecorator<*>>,
    snackbarHostState: SnackbarHostState,
    onNavigate: (AppRoute) -> Unit,
    waterViewModel: WaterViewModel,
    onRequestActivityPermission: () -> Unit
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = onBack,
        entryDecorators = entryDecorators,
        entryProvider = entryProvider {
            entry<Home> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Hello World from Home Screen!", textAlign = TextAlign.Center, modifier = Modifier.clickable {})
                }
            }
            entry<Water> {
                WaterScreen(
                    viewModel = waterViewModel,  // TODO: ugh, viewModel here, its disgusting; it is need so the MainScreen can take us here when we interact with water remainder notification
                    onNavigateToStats = { onNavigate(WaterStats) },
                    onNavigateToSettings = { onNavigate(WaterSettings) }
                )
            }
            entry<WaterStats> {
                WaterStatsScreen(onNavigateUp = { onBack(1) })
            }
            entry<WaterSettings> {
                WaterSettingsScreen(onNavigateUp = { onBack(1) })
            }
            entry<Schedules> {
                SchedulesListDetailScreen(snackbarHostState = snackbarHostState, onNavigateUp = { onBack(1) })
            }
            entry<Settings> {
                SettingsScreen(onRequestActivityPermission = onRequestActivityPermission, onNavigate = onNavigate)
            }
            entry<Usage> {
                UsageStatsScreen(onNavigate = onNavigate)
            }
            entry<UsageSettings> {
                UsageSettingsScreen(onNavigateUp = { onBack(1) }, onNavigate = onNavigate)
            }
            entry<Bedtime> {
                BedtimeScreen(onNavigate = onNavigate)
            }
            entry<BedtimeSettings> {
                BedtimeSettingsScreen(
                    onNavigateUp = { onBack(1) },
                    onRequestActivityPermission = onRequestActivityPermission
                )
            }
            entry<Whitelist> {
                WhitelistScreen(onNavigateUp = { onBack(1) })
            }
            entry<Privacy> {
                DataManagementScreen(
                    snackbarHostState = snackbarHostState,
                    onNavigateUp = { onBack(1) }
                )
            }
        },
        transitionSpec = { sharedAxisX(forward = true) },
        popTransitionSpec = { sharedAxisX(forward = false) },
        predictivePopTransitionSpec = { _ -> sharedAxisX(forward = false) }
    )
}