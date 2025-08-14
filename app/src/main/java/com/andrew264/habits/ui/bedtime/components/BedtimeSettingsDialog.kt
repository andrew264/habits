package com.andrew264.habits.ui.bedtime.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.domain.analyzer.ScheduleCoverage
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.bedtime.BedtimeUiState
import com.andrew264.habits.ui.bedtime.ScheduleInfo
import com.andrew264.habits.ui.common.components.ScheduleSelector
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
fun BedtimeSettingsDialog(
    uiState: BedtimeUiState,
    onSelectSchedule: (Schedule) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.widthIn(min = 280.dp, max = 480.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimens.PaddingLarge)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                Text(
                    text = "Sleep Schedule",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Select a schedule to define your typical sleep period. This is used by the Sleep API and other heuristics to determine your presence state.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.PaddingSmall)
                )
                ScheduleSelector(
                    schedules = uiState.allSchedules,
                    selectedSchedule = uiState.selectedSchedule,
                    onScheduleSelected = onSelectSchedule,
                    modifier = Modifier.fillMaxWidth(),
                    label = "Active Sleep Schedule"
                )
                uiState.scheduleInfo?.let { info ->
                    ScheduleInfoCard(
                        scheduleInfo = info,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BedtimeSettingsDialogPreview() {
    HabitsTheme {
        BedtimeSettingsDialog(
            uiState = BedtimeUiState(
                allSchedules = listOf(DefaultSchedules.defaultSleepSchedule),
                selectedSchedule = DefaultSchedules.defaultSleepSchedule,
                scheduleInfo = ScheduleInfo(
                    summary = "Mon-Fri: 11:00 PM - 7:00 AM (+1d)\nSat, Sun: 12:00 AM - 9:00 AM (+1d)",
                    coverage = ScheduleCoverage(totalHours = 58.0, coveragePercentage = 34.5)
                )
            ),
            onSelectSchedule = {},
            onDismissRequest = {}
        )
    }
}