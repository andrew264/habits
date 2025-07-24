package com.andrew264.habits.ui.schedule.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.common.dialogs.HabitsTimePickerDialog
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeRangeRow(
    timeRange: TimeRange,
    onDelete: () -> Unit,
    onUpdate: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFromPicker by rememberSaveable { mutableStateOf(false) }
    var showToPicker by rememberSaveable { mutableStateOf(false) }
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
                    val fromInteractionSource = remember { MutableInteractionSource() }
                    HapticInteractionEffect(fromInteractionSource)
                    FilledTonalButton(
                        onClick = { showFromPicker = true },
                        interactionSource = fromInteractionSource,
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            text = FormatUtils.formatTimeFromMinute(timeRange.fromMinuteOfDay),
                        )
                    }

                    Text(
                        text = "→",
                    )

                    val toInteractionSource = remember { MutableInteractionSource() }
                    HapticInteractionEffect(toInteractionSource)
                    FilledTonalButton(
                        onClick = { showToPicker = true },
                        interactionSource = toInteractionSource,
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            text = FormatUtils.formatTimeFromMinute(minuteOfDay = timeRange.toMinuteOfDay),
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
            val deleteInteractionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(deleteInteractionSource)
            IconButton(
                onClick = {
                    onDelete()
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                },
                interactionSource = deleteInteractionSource,
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