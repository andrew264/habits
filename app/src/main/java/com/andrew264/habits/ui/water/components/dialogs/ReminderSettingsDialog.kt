package com.andrew264.habits.ui.water.components.dialogs

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.components.ScheduleSelector
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings

@Composable
fun ReminderSettingsDialog(
    settings: PersistentSettings,
    allSchedules: List<Schedule>,
    onDismiss: () -> Unit,
    onSave: (isEnabled: Boolean, interval: String, snooze: String, schedule: Schedule?) -> Unit
) {
    var isEnabled by rememberSaveable { mutableStateOf(settings.isWaterReminderEnabled) }
    var interval by rememberSaveable { mutableStateOf(settings.waterReminderIntervalMinutes.toString()) }
    var snooze by rememberSaveable { mutableStateOf(settings.waterReminderSnoozeMinutes.toString()) }
    var selectedSchedule by rememberSaveable(settings.waterReminderScheduleId, allSchedules) {
        mutableStateOf(allSchedules.find { it.id == settings.waterReminderScheduleId })
    }
    val view = LocalView.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .padding(Dimens.PaddingExtraLarge)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                Text("Reminder Settings", style = MaterialTheme.typography.headlineSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Reminders", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            val feedback = if (it) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                            view.performHapticFeedback(feedback)
                        },

                        enabled = settings.isWaterTrackingEnabled
                    )
                }
                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it },
                    label = { Text("Interval (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled && settings.isWaterTrackingEnabled
                )
                OutlinedTextField(
                    value = snooze,
                    onValueChange = { snooze = it },
                    label = { Text("Snooze (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled && settings.isWaterTrackingEnabled
                )
                ScheduleSelector(
                    schedules = allSchedules,
                    selectedSchedule = selectedSchedule,
                    onScheduleSelected = { selectedSchedule = it },
                    label = "Reminder Schedule",
                    enabled = isEnabled && settings.isWaterTrackingEnabled
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onDismiss()
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    }) { Text("Cancel") }
                    Spacer(Modifier.width(Dimens.PaddingSmall))
                    TextButton(onClick = {
                        onSave(isEnabled, interval, snooze, selectedSchedule)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Preview
@Composable
internal fun ReminderSettingsDialogPreview() {
    val settings = createPreviewPersistentSettings()
    val allSchedules = listOf(
        Schedule(id = "1", name = "Schedule 1", groups = emptyList()),
        Schedule(id = "2", name = "Schedule 2", groups = emptyList())
    )
    ReminderSettingsDialog(
        settings = settings,
        allSchedules = allSchedules,
        onDismiss = {},
        onSave = { _, _, _, _ -> }
    )
}