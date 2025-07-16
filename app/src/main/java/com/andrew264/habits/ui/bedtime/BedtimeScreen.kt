package com.andrew264.habits.ui.bedtime

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.andrew264.habits.ui.common.charts.SleepChart
import com.andrew264.habits.ui.common.charts.TimelineChart
import com.andrew264.habits.ui.common.charts.TimelineLabelStrategy
import com.andrew264.habits.ui.common.components.FeatureDisabledContent
import com.andrew264.habits.ui.common.components.ScheduleSelector
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.MonitoringSettings
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
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
                            onSetTimelineRange = viewModel::setTimelineRange,
                            onConfigureScheduleClicked = {
                                scope.launch {
                                    scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
                                }
                            },
                            isSupportingPaneHidden = { scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Hidden }
                        )
                    }
                },
                supportingPane = {
                    AnimatedPane {
                        BedtimeSupportingPane(
                            uiState = uiState,
                            onSelectSchedule = { viewModel.selectSchedule(it.id) }
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BedtimeMainPane(
    uiState: BedtimeUiState,
    onSetTimelineRange: (BedtimeChartRange) -> Unit,
    onConfigureScheduleClicked: () -> Unit,
    isSupportingPaneHidden: () -> Boolean
) {
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = Dimens.PaddingLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.PaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    val ranges = BedtimeChartRange.entries
                    ButtonGroup(
                        overflowIndicator = { menuState ->
                            IconButton(onClick = { menuState.show() }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                        },
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        ranges.forEachIndexed { index, range ->
                            customItem(
                                buttonGroupContent = {
                                    ElevatedToggleButton(
                                        checked = uiState.selectedTimelineRange == range,
                                        onCheckedChange = {
                                            onSetTimelineRange(range)
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            ranges.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                    ) {
                                        Text(range.label)
                                    }
                                },
                                menuContent = { menuState ->
                                    DropdownMenuItem(
                                        text = { Text(range.label) },
                                        onClick = {
                                            onSetTimelineRange(range)
                                            menuState.dismiss()
                                        }
                                    )
                                }
                            )
                        }
                    }
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
                            .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
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
                                    .height(60.dp)
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
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }

        if (isSupportingPaneHidden()) {
            FilledTonalButton(
                onClick = onConfigureScheduleClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configure Sleep Schedule")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
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


@Composable
fun ScheduleInfoCard(
    scheduleInfo: ScheduleInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text(
                text = scheduleInfo.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Dimens.PaddingMedium))
            HorizontalDivider()
            Spacer(Modifier.height(Dimens.PaddingMedium))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = "Total hours",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", scheduleInfo.coverage.totalHours)} hours/week (${
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            scheduleInfo.coverage.coveragePercentage
                        )
                    }%)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun PresenceLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(
            color = UserPresenceState.AWAKE.toColor(),
            label = "Awake"
        )
        LegendItem(
            color = UserPresenceState.SLEEPING.toColor(),
            label = "Sleeping"
        )
        LegendItem(
            color = UserPresenceState.UNKNOWN.toColor(),
            label = "Unknown"
        )
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.PaddingMedium)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun UserPresenceState.toColor(): Color {
    return when (this) {
        UserPresenceState.AWAKE -> Color(0xFF4CAF50) // Green
        UserPresenceState.SLEEPING -> Color(0xFF3F51B5) // Indigo
        UserPresenceState.UNKNOWN -> Color(0xFF9E9E9E) // Grey
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
            isSupportingPaneHidden = { true }
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
        BedtimeScreen(onNavigate = {})
    }
}