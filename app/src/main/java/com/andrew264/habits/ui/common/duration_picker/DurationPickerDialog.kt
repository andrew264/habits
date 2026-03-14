package com.andrew264.habits.ui.common.duration_picker

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.R
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DurationPickerDialog(
    title: String,
    description: String = "",
    initialHours: Int = 0,
    initialMinutes: Int = 0,
    initialSeconds: Int = 0,
    showSeconds: Boolean = false,
    minuteInterval: Int = 5,
    onDismissRequest: () -> Unit,
    onConfirm: (hours: Int, minutes: Int, seconds: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf("%02d".format(initialHours)) }
    val initialMinuteCleaned = initialMinutes - (initialMinutes % minuteInterval)
    var selectedMinute by remember { mutableStateOf("%02d".format(initialMinuteCleaned)) }
    var selectedSecond by remember { mutableStateOf("%02d".format(initialSeconds)) }

    val hourItems = remember { (0..23).map { "%02d".format(it) } }
    val minuteItems = remember(minuteInterval) { (0..59 step minuteInterval).map { "%02d".format(it) } }
    val secondItems = remember { (0..59).map { "%02d".format(it) } }

    val view = LocalView.current

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLargeIncreased,
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(min = 300.dp, max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)

                if (description.isNotBlank()) {
                    Spacer(Modifier.height(Dimens.PaddingSmall))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(Dimens.PaddingLarge))

                DurationPicker(
                    hours = hourItems,
                    minutes = minuteItems,
                    seconds = if (showSeconds) secondItems else null,
                    selectedHour = selectedHour,
                    selectedMinute = selectedMinute,
                    selectedSecond = selectedSecond,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it },
                    onSecondChange = { selectedSecond = it }
                )

                Spacer(Modifier.height(Dimens.PaddingExtraLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(id = R.string.duration_picker_dialog_cancel))
                    }
                    Spacer(Modifier.width(Dimens.PaddingSmall))

                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            val h = selectedHour.toInt()
                            val m = selectedMinute.toInt()
                            val s = selectedSecond.toInt()
                            onConfirm(h, m, s)
                        }
                    ) {
                        Text(stringResource(id = R.string.duration_picker_dialog_ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationPicker(
    hours: List<String>,
    minutes: List<String>,
    seconds: List<String>?,
    selectedHour: String,
    selectedMinute: String,
    selectedSecond: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
    onSecondChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        NumberPicker(
            items = hours,
            selectedItem = selectedHour,
            onValueChange = onHourChange,
            modifier = Modifier.width(64.dp)
        )
        Text("hr", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 4.dp))
        NumberPicker(
            items = minutes,
            selectedItem = selectedMinute,
            onValueChange = onMinuteChange,
            loop = true,
            modifier = Modifier.width(64.dp)
        )
        Text("min", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 4.dp))

        if (seconds != null) {
            NumberPicker(
                items = seconds,
                selectedItem = selectedSecond,
                onValueChange = onSecondChange,
                loop = true,
                modifier = Modifier.width(64.dp)
            )
            Text("sec", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Preview
@Composable
private fun DurationPickerDialogPreview() {
    HabitsTheme {
        DurationPickerDialog(
            title = "Set duration",
            description = "",
            initialHours = 1,
            initialMinutes = 30,
            initialSeconds = 15,
            showSeconds = true,
            minuteInterval = 1,
            onDismissRequest = {},
            onConfirm = { _, _, _ -> }
        )
    }
}