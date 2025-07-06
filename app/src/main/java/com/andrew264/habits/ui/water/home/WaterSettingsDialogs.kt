package com.andrew264.habits.ui.water.home

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.components.ScheduleSelector
import com.andrew264.habits.ui.theme.Dimens

@Composable
internal fun TargetSettingsDialog(
    settings: PersistentSettings,
    onDismiss: () -> Unit,
    onSave: (isEnabled: Boolean, targetMl: String) -> Unit
) {
    var isEnabled by remember { mutableStateOf(settings.isWaterTrackingEnabled) }
    var targetMl by remember { mutableStateOf(settings.waterDailyTargetMl.toString()) }
    val view = LocalView.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingExtraLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                Text("Tracking Settings", style = MaterialTheme.typography.headlineSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Tracking", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            val feedback = if (it) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                            view.performHapticFeedback(feedback)
                        }
                    )
                }
                OutlinedTextField(
                    value = targetMl,
                    onValueChange = { targetMl = it },
                    label = { Text("Daily Target (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled
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
                        onSave(isEnabled, targetMl)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
internal fun ReminderSettingsDialog(
    settings: PersistentSettings,
    allSchedules: List<Schedule>,
    onDismiss: () -> Unit,
    onSave: (isEnabled: Boolean, interval: String, snooze: String, schedule: Schedule?) -> Unit
) {
    var isEnabled by remember { mutableStateOf(settings.isWaterReminderEnabled) }
    var interval by remember { mutableStateOf(settings.waterReminderIntervalMinutes.toString()) }
    var snooze by remember { mutableStateOf(settings.waterReminderSnoozeMinutes.toString()) }
    var selectedSchedule by remember(settings.waterReminderScheduleId, allSchedules) {
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
                        }
                    )
                }
                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it },
                    label = { Text("Interval (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled
                )
                OutlinedTextField(
                    value = snooze,
                    onValueChange = { snooze = it },
                    label = { Text("Snooze (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled
                )
                ScheduleSelector(
                    schedules = allSchedules,
                    selectedSchedule = selectedSchedule,
                    onScheduleSelected = { selectedSchedule = it },
                    label = "Reminder Schedule",
                    enabled = isEnabled
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