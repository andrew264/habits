package com.andrew264.habits.ui.navigation

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.andrew264.habits.R
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
import kotlin.math.roundToInt

@Composable
fun AppNavDisplay(
    modifier: Modifier = Modifier,
    backStack: List<AppRoute>,
    onBack: () -> Unit,
    entryDecorators: List<NavEntryDecorator<AppRoute>>,
    onNavigate: (AppRoute) -> Unit
) {
    val density = LocalDensity.current
    val slideDistance = remember(density) {
        with(density) { 30.dp.toPx() }.roundToInt()
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = onBack,
        entryDecorators = entryDecorators,
        entryProvider = entryProvider {
            entry<Home> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.app_nav_display_home_screen_text), textAlign = TextAlign.Center, modifier = Modifier.clickable {})
                }
            }
            entry<Water> {
                WaterScreen(
                    onNavigateToStats = { onNavigate(WaterStats) },
                    onNavigateToSettings = { onNavigate(WaterSettings) }
                )
            }
            entry<WaterStats> {
                WaterStatsScreen(onNavigateUp = { onBack() })
            }
            entry<WaterSettings> {
                WaterSettingsScreen(onNavigateUp = { onBack() })
            }
            entry<Schedules> {
                SchedulesListDetailScreen(onNavigateUp = { onBack() })
            }
            entry<Settings> {
                SettingsScreen(onNavigate = onNavigate)
            }
            entry<Usage> {
                UsageStatsScreen(onNavigate = onNavigate)
            }
            entry<UsageSettings> {
                UsageSettingsScreen(onNavigateUp = { onBack() }, onNavigate = onNavigate)
            }
            entry<Bedtime> {
                BedtimeScreen(onNavigate = onNavigate)
            }
            entry<BedtimeSettings> {
                BedtimeSettingsScreen(onNavigateUp = { onBack() })
            }
            entry<Whitelist> {
                WhitelistScreen(onNavigateUp = { onBack() })
            }
            entry<Privacy> {
                DataManagementScreen(
                    onNavigateUp = { onBack() }
                )
            }
        },
        transitionSpec = { sharedAxisXEnter(forward = true, slideDistance = slideDistance) togetherWith sharedAxisXExit(forward = true, slideDistance = slideDistance) },
        popTransitionSpec = { sharedAxisXEnter(forward = false, slideDistance = slideDistance) togetherWith sharedAxisXExit(forward = false, slideDistance = slideDistance) },
        predictivePopTransitionSpec = { _ -> sharedAxisXEnter(forward = false, slideDistance = slideDistance) togetherWith sharedAxisXExit(forward = false, slideDistance = slideDistance) }
    )
}