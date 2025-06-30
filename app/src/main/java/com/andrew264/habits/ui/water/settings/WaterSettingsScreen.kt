package com.andrew264.habits.ui.water.settings

import android.os.Build
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.model.schedule.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterSettingsScreen(
    viewModel: WaterSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val allSchedules by viewModel.allSchedules.collectAsState()
    val selectedSchedule = allSchedules.find { it.id == settings.waterReminderScheduleId }
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- General Section ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Water Tracking", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = settings.isWaterTrackingEnabled,
                        onCheckedChange = { isEnabled ->
                            viewModel.onWaterTrackingEnabledChanged(isEnabled)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                val feedback = if (isEnabled) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                                view.performHapticFeedback(feedback)
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = settings.waterDailyTargetMl.toString(),
                    onValueChange = viewModel::onDailyTargetChanged,
                    label = { Text("Daily Target (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.isWaterTrackingEnabled
                )
            }
        }

        // --- Reminders Section ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Reminders", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = settings.isWaterReminderEnabled,
                        onCheckedChange = { isEnabled ->
                            viewModel.onReminderEnabledChanged(isEnabled)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                val feedback = if (isEnabled) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                                view.performHapticFeedback(feedback)
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        },
                        enabled = settings.isWaterTrackingEnabled
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = settings.waterReminderIntervalMinutes.toString(),
                    onValueChange = viewModel::onReminderIntervalChanged,
                    label = { Text("Reminder Interval (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.isWaterTrackingEnabled && settings.isWaterReminderEnabled
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = settings.waterReminderSnoozeMinutes.toString(),
                    onValueChange = viewModel::onSnoozeTimeChanged,
                    label = { Text("Snooze Time (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.isWaterTrackingEnabled && settings.isWaterReminderEnabled
                )
                Spacer(Modifier.height(16.dp))
                if (selectedSchedule != null) {
                    ScheduleSelector(
                        schedules = allSchedules,
                        selectedSchedule = selectedSchedule,
                        onScheduleSelected = viewModel::onReminderScheduleChanged,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = settings.isWaterTrackingEnabled && settings.isWaterReminderEnabled,
                        label = "Reminder Schedule"
                    )
                }
            }
        }
    }
}

// A more generic version of the ScheduleSelector from BedtimeScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSelector(
    schedules: List<Schedule>,
    selectedSchedule: Schedule,
    onScheduleSelected: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Active Schedule"
) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (enabled) {
                expanded = !expanded
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSchedule.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            enabled = enabled
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
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                )
            }
        }
    }
}