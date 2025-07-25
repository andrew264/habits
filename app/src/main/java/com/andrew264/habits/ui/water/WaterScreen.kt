package com.andrew264.habits.ui.water

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import com.andrew264.habits.ui.water.components.InputSection
import com.andrew264.habits.ui.water.components.ProgressSection
import com.andrew264.habits.ui.water.components.WaterFeatureDisabledContent
import com.andrew264.habits.ui.water.components.dialogs.ReminderSettingsDialog
import com.andrew264.habits.ui.water.components.dialogs.TargetSettingsDialog

@Composable
fun WaterScreen(viewModel: WaterViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val showTargetDialog by viewModel.showTargetDialog.collectAsState()
    val showReminderDialog by viewModel.showReminderDialog.collectAsState()
    val isInitialComposition = remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isInitialComposition.value) {
            isInitialComposition.value = false
        } else {
            viewModel.refresh()
        }
    }

    if (showTargetDialog) {
        TargetSettingsDialog(
            settings = uiState.settings,
            onDismiss = viewModel::onDismissTargetDialog,
            onSave = viewModel::saveTargetSettings
        )
    }

    if (showReminderDialog) {
        ReminderSettingsDialog(
            settings = uiState.settings,
            allSchedules = uiState.allSchedules,
            onDismiss = viewModel::onDismissReminderDialog,
            onSave = viewModel::saveReminderSettings
        )
    }

    if (!uiState.settings.isWaterTrackingEnabled) {
        WaterFeatureDisabledContent(
            onEnableClicked = viewModel::onShowTargetDialog
        )
    } else {
        WaterScreenContent(
            uiState = uiState,
            onLogWater = viewModel::logWater,
            onEditTarget = viewModel::onShowTargetDialog
        )
    }
}

@Composable
private fun WaterScreenContent(
    modifier: Modifier = Modifier,
    uiState: WaterUiState,
    onLogWater: (Int) -> Unit,
    onEditTarget: () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProgressSection(
                    uiState = uiState,
                    onEditTarget = onEditTarget,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                InputSection(
                    onLogWater = onLogWater,
                    modifier = Modifier.weight(1f)
                )
            }
        } else { // Portrait
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.PaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProgressSection(
                    uiState = uiState,
                    onEditTarget = onEditTarget,
                    modifier = Modifier.weight(1f)
                )
                InputSection(
                    onLogWater = onLogWater,
                    modifier = Modifier.padding(bottom = Dimens.PaddingExtraLarge)
                )
            }
        }
    }
}

@Preview(name = "Water Home - Halfway", showBackground = true)
@Composable
private fun WaterScreenHalfwayPreview() {
    val settings = createPreviewPersistentSettings(waterDailyTargetMl = 2500)
    HabitsTheme {
        WaterScreenContent(
            uiState = WaterUiState(
                settings = settings,
                todaysIntakeMl = 1250,
                progress = 0.5f
            ),
            onLogWater = {},
            onEditTarget = {}
        )
    }
}

@Preview(name = "Water Home - Complete", showBackground = true)
@Composable
private fun WaterScreenCompletePreview() {
    val settings = createPreviewPersistentSettings(waterDailyTargetMl = 2000)
    HabitsTheme {
        WaterScreenContent(
            uiState = WaterUiState(
                settings = settings,
                todaysIntakeMl = 2400,
                progress = 1.0f // Progress caps at 1.0
            ),
            onLogWater = {},
            onEditTarget = {}
        )
    }
}

@Preview(name = "Water Home - Landscape", widthDp = 800, heightDp = 400, showBackground = true)
@Composable
private fun WaterScreenLandscapePreview() {
    val settings = createPreviewPersistentSettings(waterDailyTargetMl = 2500)
    HabitsTheme {
        WaterScreenContent(
            uiState = WaterUiState(
                settings = settings,
                todaysIntakeMl = 1250,
                progress = 0.5f
            ),
            onLogWater = {},
            onEditTarget = {}
        )
    }
}