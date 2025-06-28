package com.andrew264.habits.presentation.bedtime

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.state.UserPresenceState
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BedtimeScreen(
    modifier: Modifier = Modifier,
    viewModel: BedtimeViewModel = hiltViewModel()
) {
    val timelineSegments by viewModel.timelineSegments.collectAsState()
    val selectedTimelineRange by viewModel.selectedTimelineRange.collectAsState()
    val allSchedules by viewModel.allSchedules.collectAsState()
    val selectedSchedule by viewModel.selectedSchedule.collectAsState()
    val scheduleInfo by viewModel.scheduleInfo.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
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
                                    checked = selectedTimelineRange == range,
                                    onCheckedChange = { viewModel.setTimelineRange(range) },
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
                    if (timelineSegments.isEmpty()) {
                        Text(
                            text = if (selectedTimelineRange == TimelineRange.DAY)
                                "Loading today's data or no data available yet..."
                            else "Loading weekly data or no data available yet...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        UserPresenceTimeline(
                            segments = timelineSegments,
                            selectedRange = selectedTimelineRange,
                            viewStartTimeMillis = viewModel.viewStartTimeMillis.collectAsState().value,
                            viewEndTimeMillis = viewModel.viewEndTimeMillis.collectAsState().value,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Legend
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
                schedules = allSchedules,
                selectedSchedule = selectedSchedule,
                onScheduleSelected = { viewModel.selectSchedule(it.id) },
                modifier = Modifier.fillMaxWidth()
            )

            scheduleInfo?.let { info ->
                ScheduleInfoCard(
                    scheduleInfo = info,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
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

@Composable
fun UserPresenceTimeline(
    segments: List<TimelineSegment>,
    selectedRange: TimelineRange,
    viewStartTimeMillis: Long,
    viewEndTimeMillis: Long,
    modifier: Modifier = Modifier,
    barHeight: Dp = 24.dp,
    labelSpacing: Dp = 6.dp,
    tickHeight: Dp = 4.dp
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val labelTextStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    val hourLabelFormatter = remember { SimpleDateFormat("ha", Locale.getDefault()) }
    val dayLabelFormatter = remember { SimpleDateFormat("E", Locale.getDefault()) }

    val barOutlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val barBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cornerRadius = CornerRadius(barHeight.value / 3)

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val mainBarHeightPx = barHeight.toPx()
        val barTopY = (canvasHeight - mainBarHeightPx - labelSpacing.toPx() * 2 - tickHeight.toPx()) / 2f
        val totalDurationMillis = viewEndTimeMillis - viewStartTimeMillis

        if (totalDurationMillis <= 0) return@Canvas

        // Draw background
        drawRoundRect(
            color = barBackgroundColor,
            topLeft = Offset(0f, barTopY),
            size = Size(canvasWidth, mainBarHeightPx),
            cornerRadius = cornerRadius
        )

        // Draw segments
        segments.forEach { segment ->
            val startOffset = max(0L, segment.startTimeMillis - viewStartTimeMillis)
            val endOffset = segment.endTimeMillis - viewStartTimeMillis
            val segmentWidth = ((endOffset - startOffset).toFloat() / totalDurationMillis.toFloat()) * canvasWidth
            val currentX = (startOffset.toFloat() / totalDurationMillis.toFloat()) * canvasWidth

            if (segmentWidth > 0f) {
                drawRect(
                    color = segment.state.toColor(),
                    topLeft = Offset(currentX, barTopY),
                    size = Size(segmentWidth, mainBarHeightPx)
                )
            }
        }

        // Clip drawing to the rounded rectangle shape for the outline
        drawRoundRect(
            color = barOutlineColor,
            topLeft = Offset(0f, barTopY),
            size = Size(canvasWidth, mainBarHeightPx),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx())
        )

        val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(viewStartTimeMillis), ZoneId.systemDefault())

        val timeFormatter: SimpleDateFormat
        val timeIncrement: Long
        val roundedStartDateTime: LocalDateTime
        when (selectedRange) {
            TimelineRange.TWELVE_HOURS -> {
                timeFormatter = hourLabelFormatter
                timeIncrement = TimeUnit.HOURS.toMillis(2)
                roundedStartDateTime = startDateTime.truncatedTo(ChronoUnit.HOURS)
            }

            TimelineRange.DAY -> {
                timeFormatter = hourLabelFormatter
                timeIncrement = TimeUnit.HOURS.toMillis(4)
                roundedStartDateTime = startDateTime.truncatedTo(ChronoUnit.HOURS)
            }

            TimelineRange.WEEK -> {
                timeFormatter = dayLabelFormatter
                timeIncrement = TimeUnit.DAYS.toMillis(1)
                roundedStartDateTime = startDateTime.truncatedTo(ChronoUnit.DAYS)
            }
        }

        val roundedStartTimeMillis = roundedStartDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        var labelTimeMillis = roundedStartTimeMillis
        val drawnLabels = mutableListOf<Pair<TextLayoutResult, Float>>()

        while (labelTimeMillis <= viewEndTimeMillis) {
            if (labelTimeMillis >= viewStartTimeMillis) {
                val cal = Calendar.getInstance().apply { timeInMillis = labelTimeMillis }
                val labelText = timeFormatter.format(cal.time).lowercase()
                val textLayoutResult = textMeasurer.measure(text = labelText, style = labelTextStyle)

                val labelX = (labelTimeMillis - viewStartTimeMillis).toFloat() / totalDurationMillis * canvasWidth
                val textX = (labelX - textLayoutResult.size.width / 2f).coerceIn(0f, canvasWidth - textLayoutResult.size.width)

                val canDraw = drawnLabels.none { (prevLayout, prevX) ->
                    textX < prevX + prevLayout.size.width && textX + textLayoutResult.size.width > prevX
                }

                if (canDraw) {
                    drawnLabels.add(textLayoutResult to textX)

                    // Draw tick mark
                    drawLine(
                        color = tickColor,
                        start = Offset(labelX, barTopY + mainBarHeightPx),
                        end = Offset(labelX, barTopY + mainBarHeightPx + tickHeight.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw text
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(textX, barTopY + mainBarHeightPx + labelSpacing.toPx() + tickHeight.toPx())
                    )
                }
            }
            labelTimeMillis += timeIncrement
        }
    }
}


fun UserPresenceState.toColor(): Color {
    return when (this) {
        UserPresenceState.AWAKE -> Color(0xFF4CAF50) // Green
        UserPresenceState.SLEEPING -> Color(0xFF3F51B5) // Indigo
        UserPresenceState.UNKNOWN -> Color(0xFF9E9E9E) // Grey
    }
}