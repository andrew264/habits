package com.andrew264.habits.ui.bedtime

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
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
import java.util.Locale
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            BedtimeScreenContent(
                modifier = modifier,
                uiState = uiState,
                onSetTimelineRange = viewModel::setTimelineRange,
                onSelectSchedule = { viewModel.selectSchedule(it.id) }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BedtimeScreenContent(
    modifier: Modifier = Modifier,
    uiState: BedtimeUiState,
    onSetTimelineRange: (BedtimeChartRange) -> Unit,
    onSelectSchedule: (Schedule) -> Unit,
) {
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        // Presence History Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sleep History",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

                // Timeline Range Buttons
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

                // Timeline Visualization
                if (uiState.timelineSegments.isEmpty()) {
                    Text(
                        text = "No presence data available for this time range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(
                        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                    ) {
                        if (uiState.selectedTimelineRange.isLinear) {
                            // Linear Timeline for short ranges
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
                            // Sleep Chart for long ranges
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

        HorizontalDivider()

        // Sleep Schedule Configuration Section
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
                    text = "${String.format(Locale.getDefault(), "%.1f", scheduleInfo.coverage.totalHours)} hours/week (${String.format(Locale.getDefault(), "%.1f", scheduleInfo.coverage.coveragePercentage)}%)",
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

@Preview(name = "Bedtime Screen - Day View", showBackground = true)
@Composable
private fun BedtimeScreenContentDayPreview() {
    val now = System.currentTimeMillis()
    val range = BedtimeChartRange.DAY
    val startTime = now - range.durationMillis

    val segments = remember {
        listOf(
            com.andrew264.habits.domain.model.TimelineSegment(startTime, startTime + TimeUnit.HOURS.toMillis(2), UserPresenceState.AWAKE, TimeUnit.HOURS.toMillis(2)),
            com.andrew264.habits.domain.model.TimelineSegment(startTime + TimeUnit.HOURS.toMillis(2), startTime + TimeUnit.HOURS.toMillis(10), UserPresenceState.SLEEPING, TimeUnit.HOURS.toMillis(8)),
            com.andrew264.habits.domain.model.TimelineSegment(startTime + TimeUnit.HOURS.toMillis(10), startTime + TimeUnit.HOURS.toMillis(12), UserPresenceState.AWAKE, TimeUnit.HOURS.toMillis(2)),
            com.andrew264.habits.domain.model.TimelineSegment(startTime + TimeUnit.HOURS.toMillis(12), now, UserPresenceState.UNKNOWN, now - (startTime + TimeUnit.HOURS.toMillis(12)))
        )
    }

    HabitsTheme {
        BedtimeScreenContent(
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
                    summary = "Every day: 10:00 PM - 6:00 AM (+1d)",
                    coverage = ScheduleCoverage(totalHours = 56.0, coveragePercentage = 33.3)
                )
            ),
            onSetTimelineRange = {},
            onSelectSchedule = {}
        )
    }
}


@Preview(name = "Bedtime Screen - Week View", showBackground = true)
@Composable
private fun BedtimeScreenContentWeekPreview() {
    val now = System.currentTimeMillis()
    val range = BedtimeChartRange.WEEK
    val startTime = now - range.durationMillis

    val segments = remember {
        (0..6).flatMap { day ->
            val dayStart = startTime + TimeUnit.DAYS.toMillis(day.toLong())
            listOf(
                com.andrew264.habits.domain.model.TimelineSegment(dayStart + TimeUnit.HOURS.toMillis(22), dayStart + TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(6), UserPresenceState.SLEEPING, TimeUnit.HOURS.toMillis(8)),
                com.andrew264.habits.domain.model.TimelineSegment(dayStart + TimeUnit.HOURS.toMillis(6), dayStart + TimeUnit.HOURS.toMillis(22), UserPresenceState.AWAKE, TimeUnit.HOURS.toMillis(16))
            )
        }
    }

    HabitsTheme {
        BedtimeScreenContent(
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