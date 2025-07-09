package com.andrew264.habits.ui.schedule.create

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.common.dialogs.HabitsTimePickerDialog
import com.andrew264.habits.ui.theme.Dimens
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onDayClick: (DayOfWeek) -> Unit,
) {
    val view = LocalView.current

    Column(
        modifier = Modifier.padding(Dimens.PaddingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
    ) {
        Text(
            text = "Days",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DayOfWeek.entries.forEach { day ->
                val selected = day in selectedDays
                val dayName = day.name.take(1).replaceFirstChar { it.titlecase(Locale.getDefault()) }

                val backgroundColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else Color.Transparent,
                    label = "DaySelectorBackgroundColor"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    label = "DaySelectorContentColor"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                    label = "DaySelectorBorderColor"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color = backgroundColor)
                        .border(width = 1.dp, color = borderColor, shape = CircleShape)
                        .clickable {
                            onDayClick(day)
                            val feedback = if (!selected) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                            view.performHapticFeedback(feedback)
                        }
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun DaySelectorPreview() {
    val selectedDays = remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)) }
    DaySelector(
        selectedDays = selectedDays.value,
        onDayClick = { day ->
            selectedDays.value = if (day in selectedDays.value) selectedDays.value - day else selectedDays.value + day
        })
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
    val view = LocalView.current

    val isOvernight = timeRange.toMinuteOfDay < timeRange.fromMinuteOfDay

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Dimens.PaddingMedium,
                    end = Dimens.PaddingExtraSmall,
                    top = Dimens.PaddingExtraSmall,
                    bottom = Dimens.PaddingExtraSmall
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    FilledTonalButton(
                        onClick = {
                            showFromPicker = true
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            text = formatTime(minuteOfDay = timeRange.fromMinuteOfDay),
                        )
                    }

                    Text(
                        text = "→",
                    )

                    FilledTonalButton(
                        onClick = {
                            showToPicker = true
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            text = formatTime(minuteOfDay = timeRange.toMinuteOfDay),
                        )
                    }

                    if (isOvernight) {
                        Text(
                            text = "+1d",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    onDelete()
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                },
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
        HabitsTimePickerDialog(
            onDismissRequest = { showFromPicker = false },
            onConfirm = { hour, minute ->
                val newMinuteOfDay = hour * 60 + minute
                onUpdate(timeRange.copy(fromMinuteOfDay = newMinuteOfDay))
                showFromPicker = false
            },
            title = "From Time",
            initialHour = timeRange.fromMinuteOfDay / 60,
            initialMinute = timeRange.fromMinuteOfDay % 60
        )
    }

    if (showToPicker) {
        HabitsTimePickerDialog(
            onDismissRequest = { showToPicker = false },
            onConfirm = { hour, minute ->
                val newMinuteOfDay = hour * 60 + minute
                onUpdate(timeRange.copy(toMinuteOfDay = newMinuteOfDay))
                showToPicker = false
            },
            title = "To Time",
            initialHour = timeRange.toMinuteOfDay / 60,
            initialMinute = timeRange.toMinuteOfDay % 60
        )
    }
}

@Composable
private fun formatTime(minuteOfDay: Int): String {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return time.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun TimeRangeRowPreview() {
    TimeRangeRow(
        timeRange = TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60),
        onDelete = {},
        onUpdate = {}
    )
}