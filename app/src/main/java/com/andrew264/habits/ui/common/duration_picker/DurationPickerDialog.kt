package com.andrew264.habits.ui.common.duration_picker

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DurationPickerDialog(
    title: String,
    description: String,
    initialTotalMinutes: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (totalMinutes: Int) -> Unit
) {
    val initialHours = initialTotalMinutes / 60
    val initialMinutes = initialTotalMinutes % 60
    var selectedHour by remember { mutableStateOf("%02d".format(initialHours)) }
    // Ensure initial minute is a multiple of 5
    val initialMinuteCleaned = initialMinutes - initialMinutes % 5
    var selectedMinute by remember { mutableStateOf("%02d".format(initialMinuteCleaned)) }

    val hourItems = remember { (0..23).map { "%02d".format(it) } }
    val minuteItems = remember { (0..55 step 5).map { "%02d".format(it) } }

    val view = LocalView.current

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLargeIncreased,
            tonalElevation = 6.dp,
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(Dimens.PaddingSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Dimens.PaddingLarge))

                // the meat
                DurationPicker(
                    hours = hourItems,
                    minutes = minuteItems,
                    selectedHour = selectedHour,
                    selectedMinute = selectedMinute,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it }
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
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(Dimens.PaddingSmall))

                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            val totalMinutes = selectedHour.toInt() * 60 + selectedMinute.toInt()
                            onConfirm(totalMinutes)
                        }
                    ) {
                        Text("OK")
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
    selectedHour: String,
    selectedMinute: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit
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
            modifier = Modifier.width(80.dp)
        )
        Text("hr", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
        NumberPicker(
            items = minutes,
            selectedItem = selectedMinute,
            onValueChange = onMinuteChange,
            loop = true,
            modifier = Modifier.width(80.dp)
        )
        Text("min", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

@Preview
@Composable
private fun DurationPickerDialogPreview() {
    HabitsTheme {
        DurationPickerDialog(
            title = "Set daily limit",
            description = "This app limit for Chrome will reset at midnight",
            initialTotalMinutes = 90,
            onDismissRequest = {},
            onConfirm = { totalMinutes ->
                println("Confirmed total minutes: $totalMinutes")
            }
        )
    }
}