package com.andrew264.habits.presentation.schedule

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.TimeRange
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onDayClick: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Active Days",
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DayOfWeek.entries.forEach { day ->
                    val selected = day in selectedDays
                    val dayName = day.name.take(3).lowercase()
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

                    val buttonColors = if (selected) {
                        ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.textButtonColors()
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { onDayClick(day) },
                            modifier = Modifier.fillMaxSize(),
                            shapes = ButtonDefaults.shapes(),
                            colors = buttonColors
                        ) {
                            Text(
                                text = dayName
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeRangeRow(
    timeRange: TimeRange,
    onDelete: () -> Unit,
    onUpdate: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val fromTimeState = rememberTimePickerState(
        initialHour = timeRange.fromMinuteOfDay / 60,
        initialMinute = timeRange.fromMinuteOfDay % 60,
        is24Hour = true
    )
    val toTimeState = rememberTimePickerState(
        initialHour = timeRange.toMinuteOfDay / 60,
        initialMinute = timeRange.toMinuteOfDay % 60,
        is24Hour = true
    )

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showFromPicker = true },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            text = formatTime(timeRange.fromMinuteOfDay),
                        )
                    }

                    Text(
                        text = "→",
                    )

                    FilledTonalButton(
                        onClick = { showToPicker = true },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            text = formatTime(timeRange.toMinuteOfDay),
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete time range",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showFromPicker) {
        TimePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newMinuteOfDay = fromTimeState.hour * 60 + fromTimeState.minute
                        if (newMinuteOfDay >= timeRange.toMinuteOfDay) {
                            Toast.makeText(context, "Start time must be before end time.", Toast.LENGTH_SHORT).show()
                        } else {
                            onUpdate(timeRange.copy(fromMinuteOfDay = newMinuteOfDay))
                            showFromPicker = false
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFromPicker = false },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Cancel") }
            },
            title = { Text("From Time") },
        ) {
            TimePicker(state = fromTimeState)
        }
    }
    if (showToPicker) {
        TimePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newMinuteOfDay = toTimeState.hour * 60 + toTimeState.minute
                        if (newMinuteOfDay <= timeRange.fromMinuteOfDay) {
                            Toast.makeText(context, "End time must be after start time.", Toast.LENGTH_SHORT).show()
                        } else {
                            onUpdate(timeRange.copy(toMinuteOfDay = newMinuteOfDay))
                            showToPicker = false
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showToPicker = false },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Cancel") }
            },
            title = { Text("End Time") },
        ) {
            TimePicker(state = toTimeState)
        }
    }
}

fun formatTime(minuteOfDay: Int): String {
    val hours = minuteOfDay / 60
    val minutes = minuteOfDay % 60
    return String.format(Locale.US, "%02d:%02d", hours, minutes)
}