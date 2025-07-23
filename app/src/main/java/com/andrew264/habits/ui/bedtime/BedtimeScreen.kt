package com.andrew264.habits.ui.bedtime

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.domain.analyzer.ScheduleCoverage
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.bedtime.components.PresenceLegend
import com.andrew264.habits.ui.bedtime.components.ScheduleInfoCard
import com.andrew264.habits.ui.bedtime.components.toColor
import com.andrew264.habits.ui.common.charts.SleepChart
import com.andrew264.habits.ui.common.charts.TimelineChart
import com.andrew264.habits.ui.common.charts.TimelineLabelStrategy
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.FeatureDisabledContent
import com.andrew264.habits.ui.common.components.FilterButtonGroup
import com.andrew264.habits.ui.common.components.ScheduleSelector
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.MonitoringSettings
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.launch
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
        onSelectSchedule = { viewModel.selectSchedule(it.id) },
        onRefresh = viewModel::refresh,
        onNavigate = onNavigate
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun BedtimeScreen(
    modifier: Modifier = Modifier,
    uiState: BedtimeUiState,
    onSetTimelineRange: (BedtimeChartRange) -> Unit,
    onSelectSchedule: (Schedule) -> Unit,
    onRefresh: () -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    when {
        uiState.isLoading && uiState.timelineSegments.isEmpty() -> {
            ContainedLoadingIndicator()
        }

        !uiState.isBedtimeTrackingEnabled -> {
            FeatureDisabledContent(
                title = "Bedtime Tracking Disabled",
                description = "This feature uses sleep schedules and the Sleep API to track your sleep patterns. You can enable it in the Monitoring settings.",
                buttonText = "Go to Settings",
                onEnableClicked = { onNavigate(MonitoringSettings) }
            )
        }

        else -> {
            val scaffoldNavigator = rememberSupportingPaneScaffoldNavigator()
            val scope = rememberCoroutineScope()

            NavigableSupportingPaneScaffold(
                modifier = modifier,
                navigator = scaffoldNavigator,
                mainPane = {
                    AnimatedPane {
                        BedtimeMainPane(
                            uiState = uiState,
                            onSetTimelineRange = onSetTimelineRange,
                            onConfigureScheduleClicked = {
                                scope.launch {
                                    scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
                                }
                            },
                            isSupportingPaneHidden = { scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Hidden },
                            onRefresh = onRefresh
                        )
                    }
                },
                supportingPane = {
                    AnimatedPane {
                        BedtimeSupportingPane(
                            uiState = uiState,
                            onSelectSchedule = onSelectSchedule
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BedtimeMainPane(
    uiState: BedtimeUiState,
    onSetTimelineRange: (BedtimeChartRange) -> Unit,
    onConfigureScheduleClicked: () -> Unit,
    isSupportingPaneHidden: () -> Boolean,
    onRefresh: () -> Unit
) {
    val isRefreshing = uiState.isLoading
    val pullToRefreshState = rememberPullToRefreshState()
    val view = LocalView.current
    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.PaddingLarge),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(Dimens.PaddingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Sleep History",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterButtonGroup(
                            options = BedtimeChartRange.entries,
                            selectedOption = uiState.selectedTimelineRange,
                            onOptionSelected = onSetTimelineRange,
                            getLabel = { it.label }
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
                    if (uiState.timelineSegments.isEmpty()) {
                        Text(
                            text = "No presence data available for this time range.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                        ) {
                            if (uiState.selectedTimelineRange.isLinear) {
                                val timelineLabelStrategy = remember(uiState.selectedTimelineRange) {
                                    when (uiState.selectedTimelineRange) {
                                        BedtimeChartRange.TWELVE_HOURS -> TimelineLabelStrategy.TWELVE_HOURS
                                        else -> TimelineLabelStrategy.DAY
                                    }
                                }
                                TimelineChart(
                                    segments = uiState.timelineSegments,
                                    getStartTimeMillis = { it.startTimeMillis },
                                    getEndTimeMillis = { it.endTimeMillis },
                                    getColor = { it.state.toColor() },
                                    viewStartTimeMillis = uiState.viewStartTimeMillis,
                                    viewEndTimeMillis = uiState.viewEndTimeMillis,
                                    labelStrategy = timelineLabelStrategy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(92.dp),
                                    barHeight = 64.dp
                                )
                                PresenceLegend()
                            } else {
                                val sleepSegments = remember(uiState.timelineSegments) {
                                    uiState.timelineSegments.filter { it.state == UserPresenceState.SLEEPING }
                                }
                                val rangeInDays = if (uiState.selectedTimelineRange == BedtimeChartRange.WEEK) 7 else 30
                                SleepChart(
                                    segments = sleepSegments,
                                    getStartTimeMillis = { it.startTimeMillis },
                                    getEndTimeMillis = { it.endTimeMillis },
                                    getState = { it.state },
                                    getColorForState = { it.toColor() },
                                    rangeInDays = rangeInDays,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isSupportingPaneHidden()) {
                FilledTonalButton(
                    onClick = {
                        onConfigureScheduleClicked()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configure Sleep Schedule")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        PullToRefreshDefaults.LoadingIndicator(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                }
        )
    }
}

@Composable
private fun BedtimeSupportingPane(
    uiState: BedtimeUiState,
    onSelectSchedule: (Schedule) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        Text(
            text = "Sleep Schedule",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Select a schedule to define your typical sleep period. This is used by the Sleep API and other heuristics to determine your presence state.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimens.PaddingSmall)
        )
        ScheduleSelector(
            schedules = uiState.allSchedules,
            selectedSchedule = uiState.selectedSchedule,
            onScheduleSelected = onSelectSchedule,
            modifier = Modifier.fillMaxWidth(),
            label = "Active Sleep Schedule"
        )
        uiState.scheduleInfo?.let { info ->
            ScheduleInfoCard(
                scheduleInfo = info,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Bedtime Main Pane (Compact)", showBackground = true)
@Composable
private fun BedtimeMainPaneCompactPreview() {
    val now = System.currentTimeMillis()
    val range = BedtimeChartRange.DAY
    val startTime = now - range.durationMillis
    val segments = remember {
        listOf(
            com.andrew264.habits.domain.model.TimelineSegment(
                startTime,
                startTime + TimeUnit.HOURS.toMillis(8),
                UserPresenceState.SLEEPING,
                TimeUnit.HOURS.toMillis(8)
            ),
            com.andrew264.habits.domain.model.TimelineSegment(
                startTime + TimeUnit.HOURS.toMillis(8),
                now,
                UserPresenceState.AWAKE,
                now - (startTime + TimeUnit.HOURS.toMillis(8))
            )
        )
    }
    HabitsTheme {
        BedtimeMainPane(
            uiState = BedtimeUiState(
                isLoading = false,
                isBedtimeTrackingEnabled = true,
                selectedTimelineRange = range,
                timelineSegments = segments,
                viewStartTimeMillis = startTime,
                viewEndTimeMillis = now,
            ),
            onSetTimelineRange = {},
            onConfigureScheduleClicked = {},
            isSupportingPaneHidden = { true },
            onRefresh = {}
        )
    }
}

@Preview(name = "Bedtime Supporting Pane", showBackground = true)
@Composable
private fun BedtimeSupportingPanePreview() {
    HabitsTheme {
        BedtimeSupportingPane(
            uiState = BedtimeUiState(
                allSchedules = listOf(DefaultSchedules.defaultSleepSchedule),
                selectedSchedule = DefaultSchedules.defaultSleepSchedule,
                scheduleInfo = ScheduleInfo(
                    summary = "Mon-Fri: 11:00 PM - 7:00 AM (+1d)\nSat, Sun: 12:00 AM - 9:00 AM (+1d)",
                    coverage = ScheduleCoverage(totalHours = 58.0, coveragePercentage = 34.5)
                )
            ),
            onSelectSchedule = {}
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
            onSelectSchedule = {},
            onRefresh = {},
            onNavigate = {}
        )
    }
}