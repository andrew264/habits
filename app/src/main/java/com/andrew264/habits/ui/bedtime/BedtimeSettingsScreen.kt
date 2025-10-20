package com.andrew264.habits.ui.bedtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.components.FeatureToggleListItem
import com.andrew264.habits.ui.common.components.ScheduleSelector
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.common.list_items.InfoListItem
import com.andrew264.habits.ui.common.list_items.ListItemPosition
import com.andrew264.habits.ui.common.list_items.ListSectionHeader
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings

@Composable
fun BedtimeSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: BedtimeSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BedtimeSettingsScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onBedtimeTrackingToggled = viewModel::onBedtimeTrackingToggled,
        onScheduleSelected = viewModel::onScheduleSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BedtimeSettingsScreen(
    uiState: BedtimeSettingsUiState,
    onNavigateUp: () -> Unit,
    onBedtimeTrackingToggled: (Boolean) -> Unit,
    onScheduleSelected: (Schedule) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SimpleTopAppBar(
                title = stringResource(id = R.string.bedtime_settings_title),
                onNavigateUp = onNavigateUp,
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(all = Dimens.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            FeatureToggleListItem(
                title = stringResource(id = R.string.bedtime_settings_enable_bedtime_tracking),
                checked = uiState.settings.isBedtimeTrackingEnabled,
                onCheckedChange = onBedtimeTrackingToggled
            )

            if (uiState.settings.isBedtimeTrackingEnabled) {
                Column {
                    ListSectionHeader(stringResource(id = R.string.bedtime_settings_schedule_section_header))
                    Column(
                        modifier = Modifier.clip(MaterialTheme.shapes.large)
                    ) {
                        val selectedSchedule = remember(uiState.settings.selectedScheduleId, uiState.allSchedules) {
                            uiState.allSchedules.find { it.id == uiState.settings.selectedScheduleId }
                                ?: DefaultSchedules.defaultSleepSchedule
                        }
                        ScheduleSelector(
                            schedules = uiState.allSchedules,
                            selectedSchedule = selectedSchedule,
                            onScheduleSelected = onScheduleSelected,
                            label = stringResource(id = R.string.bedtime_settings_active_schedule),
                            enabled = uiState.settings.isBedtimeTrackingEnabled,
                            position = ListItemPosition.TOP
                        )
                        InfoListItem(
                            text = stringResource(id = R.string.bedtime_settings_schedule_info),
                            icon = Icons.Outlined.Info,
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
private fun BedtimeSettingsScreenPreview() {
    HabitsTheme {
        BedtimeSettingsScreen(
            uiState = BedtimeSettingsUiState(settings = createPreviewPersistentSettings()),
            onNavigateUp = {},
            onBedtimeTrackingToggled = {},
            onScheduleSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BedtimeSettingsScreenDisabledPreview() {
    HabitsTheme {
        BedtimeSettingsScreen(
            uiState = BedtimeSettingsUiState(settings = createPreviewPersistentSettings(isBedtimeTrackingEnabled = false)),
            onNavigateUp = {},
            onBedtimeTrackingToggled = {},
            onScheduleSelected = {}
        )
    }
}