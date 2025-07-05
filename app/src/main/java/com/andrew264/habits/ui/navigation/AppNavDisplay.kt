package com.andrew264.habits.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import com.andrew264.habits.ui.permissions.UserPresenceControlScreen
import com.andrew264.habits.ui.schedule.create.ScheduleEditorScreen
import com.andrew264.habits.ui.schedule.create.ScheduleViewModel
import com.andrew264.habits.ui.schedule.list.SchedulesScreen
import com.andrew264.habits.ui.water.home.WaterHomeScreen
import com.andrew264.habits.ui.water.settings.WaterSettingsScreen
import com.andrew264.habits.ui.water.stats.WaterStatsScreen

// Constants for Shared Axis transition, based on Material Design guidelines.
private const val TRANSITION_DURATION = 300
private const val SLIDE_DISTANCE_PERCENT = 0.2f // A 20% slide distance.
private const val FADE_OUT_DURATION = 90
private const val FADE_IN_DELAY = FADE_OUT_DURATION
private const val FADE_IN_DURATION = TRANSITION_DURATION - FADE_IN_DELAY

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
    onOpenAppSettings: () -> Unit
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
                WaterHomeScreen(onNavigate = onNavigate)
            }
            entry<WaterSettings> {
                WaterSettingsScreen()
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
            entry<PermissionSettings> {
                UserPresenceControlScreen(
                    onRequestPermissions = onRequestPermissions,
                    onOpenAppSettings = onOpenAppSettings
                )
            }
            entry<Bedtime> {
                BedtimeScreen()
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
        },
        transitionSpec = {
            // Shared Axis X forward
            fadeIn(
                animationSpec = tween(
                    durationMillis = FADE_IN_DURATION,
                    delayMillis = FADE_IN_DELAY,
                    easing = LinearEasing
                )
            ) + slideInHorizontally(
                animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing),
                initialOffsetX = { (it * SLIDE_DISTANCE_PERCENT).toInt() }
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = FADE_OUT_DURATION,
                    easing = LinearEasing
                )
            ) + slideOutHorizontally(
                animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing),
                targetOffsetX = { -(it * SLIDE_DISTANCE_PERCENT).toInt() }
            )
        },
        popTransitionSpec = {
            // Shared Axis X backward
            fadeIn(
                animationSpec = tween(
                    durationMillis = FADE_IN_DURATION,
                    delayMillis = FADE_IN_DELAY,
                    easing = LinearEasing
                )
            ) + slideInHorizontally(
                animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing),
                initialOffsetX = { -(it * SLIDE_DISTANCE_PERCENT).toInt() }
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = FADE_OUT_DURATION,
                    easing = LinearEasing
                )
            ) + slideOutHorizontally(
                animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing),
                targetOffsetX = { (it * SLIDE_DISTANCE_PERCENT).toInt() }
            )
        },
        predictivePopTransitionSpec = {
            // Shared Axis X backward for predictive pop
            fadeIn(
                animationSpec = tween(
                    durationMillis = FADE_IN_DURATION,
                    delayMillis = FADE_IN_DELAY,
                    easing = LinearEasing
                )
            ) + slideInHorizontally(
                animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing),
                initialOffsetX = { -(it * SLIDE_DISTANCE_PERCENT).toInt() }
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = FADE_OUT_DURATION,
                    easing = LinearEasing
                )
            ) + slideOutHorizontally(
                animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing),
                targetOffsetX = { (it * SLIDE_DISTANCE_PERCENT).toInt() }
            )
        }
    )
}