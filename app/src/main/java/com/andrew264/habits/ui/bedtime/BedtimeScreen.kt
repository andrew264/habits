package com.andrew264.habits.ui.bedtime

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.charts.TimelineChart
import com.andrew264.habits.ui.common.charts.TimelineLabelStrategy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BedtimeScreen(
    modifier: Modifier = Modifier,
    viewModel: BedtimeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Presence History Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Presence History",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Timeline Range Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        val ranges = TimelineRange.entries
                        ranges.forEachIndexed { index, range ->
                            ElevatedToggleButton(
                                checked = uiState.selectedTimelineRange == range,
                                onCheckedChange = {
                                    viewModel.setTimelineRange(range)
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline Visualization
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.timelineSegments.isEmpty()) {
                    Text(
                        text = "No presence data available for this time range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    val timelineLabelStrategy = remember(uiState.selectedTimelineRange) {
                        when (uiState.selectedTimelineRange) {
                            TimelineRange.TWELVE_HOURS -> TimelineLabelStrategy.TWELVE_HOURS
                            TimelineRange.DAY -> TimelineLabelStrategy.DAY
                            TimelineRange.WEEK -> TimelineLabelStrategy.WEEK
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

                    Spacer(modifier = Modifier.height(12.dp))

                    PresenceLegend()
                }
            }
        }

        HorizontalDivider()

        // Sleep Schedule Configuration Section
        Text(
            text = "Sleep Schedule",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select a schedule to define your typical sleep period. This is used by the Sleep API and other heuristics to determine your presence state.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ScheduleSelector(
            schedules = uiState.allSchedules,
            selectedSchedule = uiState.selectedSchedule,
            onScheduleSelected = { viewModel.selectSchedule(it.id) },
            modifier = Modifier.fillMaxWidth()
        )

        uiState.scheduleInfo?.let { info ->
            ScheduleInfoCard(
                scheduleInfo = info,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSelector(
    schedules: List<Schedule>,
    selectedSchedule: Schedule,
    onScheduleSelected: (Schedule) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSchedule.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Sleep Schedule") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            schedules.forEach { schedule ->
                DropdownMenuItem(
                    text = { Text(schedule.name) },
                    onClick = {
                        onScheduleSelected(schedule)
                        expanded = false
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                )
            }
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = scheduleInfo.summary,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
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
                .size(12.dp)
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