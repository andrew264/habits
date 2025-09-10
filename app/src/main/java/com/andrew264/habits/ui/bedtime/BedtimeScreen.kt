package com.andrew264.habits.ui.bedtime

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.domain.analyzer.ScheduleCoverage
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.ui.bedtime.components.BedtimeContent
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.FeatureDisabledContent
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.BedtimeSettings
import com.andrew264.habits.ui.theme.HabitsTheme
import java.util.concurrent.TimeUnit


@Composable
fun BedtimeScreen(
    modifier: Modifier = Modifier,
    viewModel: BedtimeViewModel = hiltViewModel(),
    onNavigate: (AppRoute) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInitialComposition = remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isInitialComposition.value) {
            isInitialComposition.value = false
        } else {
            viewModel.refresh()
        }
    }

    BedtimeScreen(
        modifier = modifier,
        uiState = uiState,
        onSetTimelineRange = viewModel::setTimelineRange,
        onRefresh = viewModel::refresh,
        onNavigate = onNavigate,
        onShowSettings = { onNavigate(BedtimeSettings) }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BedtimeScreen(
    modifier: Modifier = Modifier,
    uiState: BedtimeUiState,
    onSetTimelineRange: (BedtimeChartRange) -> Unit,
    onRefresh: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onShowSettings: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    when {
        uiState.isLoading && uiState.timelineSegments.isEmpty() -> {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Bedtime") }) },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) { paddingValues ->
                ContainedLoadingIndicator(Modifier.padding(paddingValues))
            }
        }

        !uiState.isBedtimeTrackingEnabled -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Bedtime") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) { paddingValues ->
                FeatureDisabledContent(
                    modifier = Modifier.padding(paddingValues),
                    title = "Bedtime Tracking Disabled",
                    description = "This feature uses sleep schedules and the Sleep API to track your sleep patterns. You can enable it in settings.",
                    buttonText = "Go to Settings",
                    onEnableClicked = { onNavigate(BedtimeSettings) }
                )
            }
        }

        else -> {
            Scaffold(
                modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeFlexibleTopAppBar(
                        title = { Text("Bedtime") },
                        actions = {
                            val interactionSource = remember { MutableInteractionSource() }
                            HapticInteractionEffect(interactionSource)
                            IconButton(
                                onClick = onShowSettings,
                                interactionSource = interactionSource
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Configure Sleep Schedule"
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) { paddingValues ->
                BedtimeContent(
                    uiState = uiState,
                    onSetTimelineRange = onSetTimelineRange,
                    onRefresh = onRefresh,
                    paddingValues = paddingValues
                )
            }
        }
    }
}

// Previews
@Preview(name = "Bedtime Screen", showBackground = true)
@Composable
private fun BedtimeScreenPreview() {
    val now = System.currentTimeMillis()
    val range = BedtimeChartRange.DAY
    val startTime = now - range.durationMillis
    val segments = remember {
        listOf(
            com.andrew264.habits.domain.model.TimelineSegment(
                startTime,
                startTime + TimeUnit.HOURS.toMillis(8),
                com.andrew264.habits.model.UserPresenceState.SLEEPING,
                TimeUnit.HOURS.toMillis(8)
            ),
            com.andrew264.habits.domain.model.TimelineSegment(
                startTime + TimeUnit.HOURS.toMillis(8),
                now,
                com.andrew264.habits.model.UserPresenceState.AWAKE,
                now - (startTime + TimeUnit.HOURS.toMillis(8))
            )
        )
    }
    HabitsTheme {
        BedtimeScreen(
            uiState = BedtimeUiState(
                isLoading = false,
                isBedtimeTrackingEnabled = true,
                selectedTimelineRange = range,
                timelineSegments = segments,
                viewStartTimeMillis = startTime,
                viewEndTimeMillis = now,
                allSchedules = listOf(DefaultSchedules.defaultSleepSchedule),
                selectedSchedule = DefaultSchedules.defaultSleepSchedule,
                scheduleInfo = ScheduleInfo(
                    summary = "Mon-Fri: 11:00 PM - 7:00 AM (+1d)\nSat, Sun: 12:00 AM - 9:00 AM (+1d)",
                    coverage = ScheduleCoverage(totalHours = 58.0, coveragePercentage = 34.5)
                )
            ),
            onSetTimelineRange = {},
            onRefresh = {},
            onNavigate = {},
            onShowSettings = {}
        )
    }
}

@Preview(name = "Bedtime Screen - Feature Disabled", showBackground = true)
@Composable
private fun BedtimeScreenFeatureDisabledPreview() {
    HabitsTheme {
        BedtimeScreen(
            uiState = BedtimeUiState(isBedtimeTrackingEnabled = false, isLoading = false),
            onSetTimelineRange = {},
            onRefresh = {},
            onNavigate = {},
            onShowSettings = {}
        )
    }
}