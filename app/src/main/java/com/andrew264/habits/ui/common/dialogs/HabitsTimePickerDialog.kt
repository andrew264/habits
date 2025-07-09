package com.andrew264.habits.ui.common.dialogs

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    title: String,
    initialHour: Int,
    initialMinute: Int,
) {
    val view = LocalView.current
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )

    LaunchedEffect(timePickerState, view) {
        snapshotFlow { Pair(timePickerState.hour, timePickerState.minute) }
            .drop(1)
            .distinctUntilChanged()
            .collect {
                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
            }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Dimens.PaddingExtraLarge, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.PaddingMedium),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onDismissRequest()
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(Dimens.PaddingSmall))
                    TextButton(
                        onClick = {
                            onConfirm(timePickerState.hour, timePickerState.minute)
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun HabitsTimePickerDialogPreview() {
    HabitsTimePickerDialog(
        onDismissRequest = {},
        onConfirm = { _, _ -> },
        title = "Select time",
        initialHour = 10,
        initialMinute = 30
    )
}