package com.andrew264.habits.ui.water.settings

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.ui.common.components.ScheduleSelector
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterSettingsScreen(
    viewModel: WaterSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val allSchedules = uiState.allSchedules
    val selectedSchedule = allSchedules.find { it.id == settings.waterReminderScheduleId }
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        // --- General Section ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Dimens.PaddingLarge)) {
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
                            val feedback = if (isEnabled) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                            view.performHapticFeedback(feedback)
                        }
                    )
                }
                Spacer(Modifier.height(Dimens.PaddingLarge))
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
            Column(Modifier.padding(Dimens.PaddingLarge)) {
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
                            val feedback = if (isEnabled) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                            view.performHapticFeedback(feedback)
                        },
                        enabled = settings.isWaterTrackingEnabled
                    )
                }
                Spacer(Modifier.height(Dimens.PaddingLarge))
                OutlinedTextField(
                    value = settings.waterReminderIntervalMinutes.toString(),
                    onValueChange = viewModel::onReminderIntervalChanged,
                    label = { Text("Reminder Interval (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.isWaterTrackingEnabled && settings.isWaterReminderEnabled
                )
                Spacer(Modifier.height(Dimens.PaddingLarge))
                OutlinedTextField(
                    value = settings.waterReminderSnoozeMinutes.toString(),
                    onValueChange = viewModel::onSnoozeTimeChanged,
                    label = { Text("Snooze Time (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.isWaterTrackingEnabled && settings.isWaterReminderEnabled
                )
                Spacer(Modifier.height(Dimens.PaddingLarge))
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