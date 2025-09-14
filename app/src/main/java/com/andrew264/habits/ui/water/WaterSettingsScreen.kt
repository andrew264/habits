package com.andrew264.habits.ui.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.components.*
import com.andrew264.habits.ui.common.duration_picker.DurationPickerDialog
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import com.andrew264.habits.ui.water.components.dialogs.TargetSettingsDialog

@Composable
fun WaterSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: WaterSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showTargetDialog by viewModel.showTargetDialog.collectAsState()

    if (showTargetDialog) {
        TargetSettingsDialog(
            settings = uiState.settings,
            onDismiss = viewModel::onDismissTargetDialog,
            onSave = { _, targetMl -> viewModel.saveTargetSettings(targetMl) },
            showEnableSwitch = !uiState.settings.isWaterTrackingEnabled
        )
    }

    WaterSettingsScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onWaterTrackingToggled = viewModel::onWaterTrackingToggled,
        onEditTargetClicked = viewModel::onShowTargetDialog,
        onRemindersToggled = viewModel::onRemindersToggled,
        onIntervalChanged = viewModel::onIntervalChanged,
        onSnoozeChanged = viewModel::onSnoozeChanged,
        onScheduleSelected = { viewModel.onScheduleSelected(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaterSettingsScreen(
    uiState: WaterSettingsUiState,
    onNavigateUp: () -> Unit,
    onWaterTrackingToggled: (Boolean) -> Unit,
    onEditTargetClicked: () -> Unit,
    onRemindersToggled: (Boolean) -> Unit,
    onIntervalChanged: (String) -> Unit,
    onSnoozeChanged: (String) -> Unit,
    onScheduleSelected: (Schedule) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showIntervalDialog by rememberSaveable { mutableStateOf(false) }
    var showSnoozeDialog by rememberSaveable { mutableStateOf(false) }

    if (showIntervalDialog) {
        DurationPickerDialog(
            title = "Reminder Interval",
            description = "How often you want to be reminded to drink water.",
            initialTotalMinutes = uiState.settings.waterReminderIntervalMinutes,
            onDismissRequest = { showIntervalDialog = false },
            onConfirm = { totalMinutes ->
                onIntervalChanged(totalMinutes.toString())
                showIntervalDialog = false
            }
        )
    }

    if (showSnoozeDialog) {
        DurationPickerDialog(
            title = "Snooze Time",
            description = "How long to snooze a reminder for when you're busy.",
            initialTotalMinutes = uiState.settings.waterReminderSnoozeMinutes,
            onDismissRequest = { showSnoozeDialog = false },
            onConfirm = { totalMinutes ->
                onSnoozeChanged(totalMinutes.toString())
                showSnoozeDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SimpleTopAppBar(title = "Water Tracking", onNavigateUp = onNavigateUp, scrollBehavior = scrollBehavior)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(all = Dimens.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            FeatureToggleListItem(
                title = "Enable Water tracking",
                checked = uiState.settings.isWaterTrackingEnabled,
                onCheckedChange = onWaterTrackingToggled
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                Column {
                    SectionHeader("Daily Target")
                    NavigationSettingsListItem(
                        icon = Icons.Outlined.TrackChanges,
                        title = "Daily Target",
                        onClick = onEditTargetClicked,
                        enabled = uiState.settings.isWaterTrackingEnabled,
                        position = ListItemPosition.SEPARATE,
                        valueContent = {
                            Text(
                                text = "${uiState.settings.waterDailyTargetMl} ml",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                Column {
                    SectionHeader("Reminders")
                    Column(modifier = Modifier.clip(MaterialTheme.shapes.large)) {
                        ToggleSettingsListItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Enable reminders",
                            summary = "Get notifications to drink water.",
                            checked = uiState.settings.isWaterReminderEnabled,
                            onCheckedChange = onRemindersToggled,
                            enabled = uiState.settings.isWaterTrackingEnabled,
                            position = ListItemPosition.TOP
                        )
                        NavigationSettingsListItem(
                            icon = Icons.Outlined.Timer,
                            title = "Interval",
                            onClick = { showIntervalDialog = true },
                            enabled = uiState.settings.isWaterTrackingEnabled && uiState.settings.isWaterReminderEnabled,
                            position = ListItemPosition.MIDDLE,
                            valueContent = {
                                Text(
                                    text = FormatUtils.formatDuration(uiState.settings.waterReminderIntervalMinutes * 60_000L),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        NavigationSettingsListItem(
                            icon = Icons.Outlined.Snooze,
                            title = "Snooze",
                            onClick = { showSnoozeDialog = true },
                            enabled = uiState.settings.isWaterTrackingEnabled && uiState.settings.isWaterReminderEnabled,
                            position = ListItemPosition.MIDDLE,
                            valueContent = {
                                Text(
                                    text = FormatUtils.formatDuration(uiState.settings.waterReminderSnoozeMinutes * 60_000L),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        val selectedSchedule = remember(uiState.settings.waterReminderScheduleId, uiState.allSchedules) {
                            uiState.allSchedules.find { it.id == uiState.settings.waterReminderScheduleId }
                        }
                        ScheduleSelector(
                            schedules = uiState.allSchedules,
                            selectedSchedule = selectedSchedule,
                            onScheduleSelected = onScheduleSelected,
                            label = "Reminder Schedule",
                            enabled = uiState.settings.isWaterTrackingEnabled && uiState.settings.isWaterReminderEnabled,
                            position = ListItemPosition.BOTTOM
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun WaterSettingsScreenPreview() {
    HabitsTheme {
        WaterSettingsScreen(
            uiState = WaterSettingsUiState(settings = createPreviewPersistentSettings()),
            onNavigateUp = {},
            onWaterTrackingToggled = {},
            onEditTargetClicked = {},
            onRemindersToggled = {},
            onIntervalChanged = {},
            onSnoozeChanged = {},
            onScheduleSelected = {}
        )
    }
}