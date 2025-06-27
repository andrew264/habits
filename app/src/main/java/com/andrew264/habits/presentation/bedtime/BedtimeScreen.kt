package com.andrew264.habits.presentation.bedtime

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.state.UserPresenceState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BedtimeScreen(
    modifier: Modifier = Modifier,
    viewModel: BedtimeViewModel = hiltViewModel()
) {
    val currentBedtimeHour by viewModel.currentBedtimeHour.collectAsState()
    val currentBedtimeMinute by viewModel.currentBedtimeMinute.collectAsState()
    val currentWakeUpHour by viewModel.currentWakeUpHour.collectAsState()
    val currentWakeUpMinute by viewModel.currentWakeUpMinute.collectAsState()

    var showBedtimePicker by remember { mutableStateOf(false) }
    var showWakeUpTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isSystem24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    val bedtimePickerState = rememberTimePickerState(
        initialHour = currentBedtimeHour ?: 22,
        initialMinute = currentBedtimeMinute ?: 0,
        is24Hour = isSystem24Hour
    )
    val wakeUpTimePickerState = rememberTimePickerState(
        initialHour = currentWakeUpHour ?: 6,
        initialMinute = currentWakeUpMinute ?: 0,
        is24Hour = isSystem24Hour
    )

    LaunchedEffect(currentBedtimeHour, currentBedtimeMinute) {
        currentBedtimeHour?.let { bedtimePickerState.hour = it }
        currentBedtimeMinute?.let { bedtimePickerState.minute = it }
    }
    LaunchedEffect(currentWakeUpHour, currentWakeUpMinute) {
        currentWakeUpHour?.let { wakeUpTimePickerState.hour = it }
        currentWakeUpMinute?.let { wakeUpTimePickerState.minute = it }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val formatter = remember(isSystem24Hour) {
        if (isSystem24Hour) SimpleDateFormat("HH:mm", Locale.getDefault())
        else SimpleDateFormat("hh:mm a", Locale.getDefault())
    }

    val timelineSegments by viewModel.timelineSegments.collectAsState()
    val selectedTimelineRange by viewModel.selectedTimelineRange.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                            totalDurationMillis = selectedTimelineRange.durationMillis,
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
                text = "Sleep Schedule Configuration",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Set your sleep schedule to help track your sleep patterns. This is also used by Sleep API integration.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TimeSettingCard(
                title = "Bedtime",
                icon = Icons.Outlined.Bedtime,
                currentTimeHour = currentBedtimeHour,
                currentTimeMinute = currentBedtimeMinute,
                defaultTimeInfo = "Defaults to ~10 PM",
                formatter = formatter,
                onSetTimeClick = { showBedtimePicker = true },
                onClearTimeClick = {
                    viewModel.clearBedtime()
                    scope.launch {
                        snackbarHostState.showSnackbar("Custom bedtime cleared.")
                    }
                }
            )

            TimeSettingCard(
                title = "Wake-up Time",
                icon = Icons.Outlined.WbSunny,
                currentTimeHour = currentWakeUpHour,
                currentTimeMinute = currentWakeUpMinute,
                defaultTimeInfo = "Defaults to ~6 AM or 8hrs after bedtime",
                formatter = formatter,
                onSetTimeClick = { showWakeUpTimePicker = true },
                onClearTimeClick = {
                    viewModel.clearWakeUpTime()
                    scope.launch {
                        snackbarHostState.showSnackbar("Custom wake-up time cleared.")
                    }
                }
            )

            SleepDurationInfo(
                bedtimeHour = currentBedtimeHour,
                bedtimeMinute = currentBedtimeMinute,
                wakeUpHour = currentWakeUpHour,
                wakeUpMinute = currentWakeUpMinute
            )

            // Time Picker Dialogs
            if (showBedtimePicker) {
                TimePickerDialog(
                    title = {
                        Text(
                            "Set Bedtime",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    },
                    onDismissRequest = { showBedtimePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.setBedtime(
                                    bedtimePickerState.hour,
                                    bedtimePickerState.minute
                                )
                                showBedtimePicker = false
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, bedtimePickerState.hour)
                                    set(Calendar.MINUTE, bedtimePickerState.minute)
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Bedtime set to: ${formatter.format(cal.time)}"
                                    )
                                }
                            },
                            shapes = ButtonDefaults.shapes()
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showBedtimePicker = false },
                            shapes = ButtonDefaults.shapes()
                        ) { Text("Cancel") }
                    }
                ) {
                    TimePicker(state = bedtimePickerState)
                }
            }

            if (showWakeUpTimePicker) {
                TimePickerDialog(
                    title = {
                        Text(
                            "Set Wake-up Time",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    },
                    onDismissRequest = { showWakeUpTimePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.setWakeUpTime(
                                    wakeUpTimePickerState.hour,
                                    wakeUpTimePickerState.minute
                                )
                                showWakeUpTimePicker = false
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, wakeUpTimePickerState.hour)
                                    set(Calendar.MINUTE, wakeUpTimePickerState.minute)
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Wake-up time set to: ${formatter.format(cal.time)}"
                                    )
                                }
                            },
                            shapes = ButtonDefaults.shapes()
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showWakeUpTimePicker = false },
                            shapes = ButtonDefaults.shapes()
                        ) { Text("Cancel") }
                    }
                ) {
                    TimePicker(state = wakeUpTimePickerState)
                }
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
    totalDurationMillis: Long,
    modifier: Modifier = Modifier,
    barHeight: Dp = 24.dp,
    labelSpacing: Dp = 6.dp
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val labelTextStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    val hourLabelFormatter = remember { SimpleDateFormat("ha", Locale.getDefault()) }
    val dayLabelFormatter = remember { SimpleDateFormat("E", Locale.getDefault()) }

    val barOutlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val barBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val mainBarHeightPx = barHeight.toPx()
        val barTopY = (canvasHeight - mainBarHeightPx - labelSpacing.toPx() * 2) / 2f

        if (totalDurationMillis <= 0) return@Canvas

        // Draw background
        drawRect(
            color = barBackgroundColor,
            topLeft = Offset(0f, barTopY),
            size = Size(canvasWidth, mainBarHeightPx)
        )

        // Draw segments
        var currentX = 0f
        segments.forEach { segment ->
            val segmentWidth = (segment.durationMillis.toFloat() / totalDurationMillis.toFloat()) * canvasWidth
            if (segmentWidth > 0f) {
                drawRect(
                    color = segment.state.toColor(),
                    topLeft = Offset(currentX, barTopY),
                    size = Size(segmentWidth, mainBarHeightPx)
                )
                currentX += segmentWidth
            }
        }

        // Draw outline
        drawRect(
            color = barOutlineColor,
            topLeft = Offset(0f, barTopY),
            size = Size(canvasWidth, mainBarHeightPx),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )

        // Draw Time Labels
        val timelineStartTime = segments.firstOrNull()?.startTimeMillis ?: (System.currentTimeMillis() - totalDurationMillis)
        val numLabels = if (totalDurationMillis <= TimeUnit.DAYS.toMillis(1)) 6 else 8
        val timeIncrement = totalDurationMillis / (numLabels - 1)

        for (i in 0 until numLabels) {
            val labelTimeMillis = timelineStartTime + (i * timeIncrement)
            val labelText = if (totalDurationMillis <= TimeUnit.DAYS.toMillis(1)) {
                val cal = Calendar.getInstance().apply { timeInMillis = labelTimeMillis }
                if (i == numLabels - 1 && totalDurationMillis == TimeUnit.DAYS.toMillis(1)) "Now"
                else hourLabelFormatter.format(cal.time).lowercase()
            } else {
                val cal = Calendar.getInstance().apply { timeInMillis = labelTimeMillis }
                dayLabelFormatter.format(cal.time)
            }

            val textLayoutResult = textMeasurer.measure(text = labelText, style = labelTextStyle)
            val labelX = (labelTimeMillis - timelineStartTime).toFloat() / totalDurationMillis.toFloat() * canvasWidth
            val textX = (labelX - textLayoutResult.size.width / 2f).coerceIn(0f, canvasWidth - textLayoutResult.size.width)

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, barTopY + mainBarHeightPx + labelSpacing.toPx())
            )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeSettingCard(
    title: String,
    icon: ImageVector,
    currentTimeHour: Int?,
    currentTimeMinute: Int?,
    defaultTimeInfo: String,
    formatter: SimpleDateFormat,
    onSetTimeClick: () -> Unit,
    onClearTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            val timeIsSet = currentTimeHour != null && currentTimeMinute != null
            val displayTime: String = if (timeIsSet) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, currentTimeHour)
                    set(Calendar.MINUTE, currentTimeMinute)
                }
                formatter.format(cal.time)
            } else {
                "Not Set"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = displayTime,
                        style = if (timeIsSet)
                            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    )
                    if (!timeIsSet) {
                        Text(
                            text = defaultTimeInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (timeIsSet) {
                        OutlinedButton(
                            onClick = onClearTimeClick,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                    Button(
                        onClick = onSetTimeClick,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(if (timeIsSet) "Change" else "Set")
                    }
                }
            }
        }
    }
}

@Composable
fun SleepDurationInfo(
    bedtimeHour: Int?,
    bedtimeMinute: Int?,
    wakeUpHour: Int?,
    wakeUpMinute: Int?
) {
    if (bedtimeHour != null && bedtimeMinute != null && wakeUpHour != null && wakeUpMinute != null) {
        val bedtimeCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, bedtimeHour)
            set(Calendar.MINUTE, bedtimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val wakeUpCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, wakeUpHour)
            set(Calendar.MINUTE, wakeUpMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (wakeUpCalendar.before(bedtimeCalendar)) {
            wakeUpCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val durationMillis = wakeUpCalendar.timeInMillis - bedtimeCalendar.timeInMillis
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "💤 Estimated Sleep Window",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${hours}h ${minutes}m",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (wakeUpHour * 60 + wakeUpMinute < bedtimeHour * 60 + bedtimeMinute) {
                    Text(
                        "(Sleep window crosses midnight)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    } else if (bedtimeHour != null || bedtimeMinute != null || wakeUpHour != null || wakeUpMinute != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Text(
                "Set both bedtime and wake-up time to see your estimated sleep window.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}