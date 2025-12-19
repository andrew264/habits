package com.andrew264.habits.ui.water

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import com.andrew264.habits.ui.water.components.InputSection
import com.andrew264.habits.ui.water.components.ProgressSection
import com.andrew264.habits.ui.water.components.WaterFeatureDisabledContent
import com.andrew264.habits.ui.water.components.WaterTopAppBar
import com.andrew264.habits.ui.water.components.dialogs.TargetSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    viewModel: WaterViewModel = hiltViewModel(),
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showTargetDialog) {
        TargetSettingsDialog(
            settings = uiState.settings,
            onDismiss = viewModel::onDismissTargetDialog,
            onSave = viewModel::saveTargetSettings
        )
    }

    if (!uiState.settings.isWaterTrackingEnabled) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.water_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) { paddingValues ->
            WaterFeatureDisabledContent(
                modifier = Modifier.padding(paddingValues),
                onEnableClicked = viewModel::onShowTargetDialog
            )
        }
    } else {
        WaterScreen(
            uiState = uiState,
            onLogWater = viewModel::logWater,
            onNavigateToStats = onNavigateToStats,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

@Composable
private fun WaterScreen(
    modifier: Modifier = Modifier,
    uiState: WaterScreenUiState,
    onLogWater: (Int) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WaterTopAppBar(
                onNavigateToStats = onNavigateToStats,
                onWaterReminderClick = onNavigateToSettings
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
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
                        onEditTarget = onNavigateToSettings,
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
                        onEditTarget = onNavigateToSettings,
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
}

@Preview(name = "Water Home - Halfway", showBackground = true)
@Composable
private fun WaterScreenHalfwayPreview() {
    val settings = createPreviewPersistentSettings(waterDailyTargetMl = 2500)
    HabitsTheme {
        WaterScreen(
            uiState = WaterScreenUiState(
                settings = settings,
                todaysIntakeMl = 1250,
                progress = 0.5f
            ),
            onLogWater = {},
            onNavigateToStats = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "Water Home - Complete", showBackground = true)
@Composable
private fun WaterScreenCompletePreview() {
    val settings = createPreviewPersistentSettings(waterDailyTargetMl = 2000)
    HabitsTheme {
        WaterScreen(
            uiState = WaterScreenUiState(
                settings = settings,
                todaysIntakeMl = 2400,
                progress = 1.0f // Progress caps at 1.0
            ),
            onLogWater = {},
            onNavigateToStats = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "Water Home - Landscape", widthDp = 800, heightDp = 400, showBackground = true)
@Composable
private fun WaterScreenLandscapePreview() {
    val settings = createPreviewPersistentSettings(waterDailyTargetMl = 2500)
    HabitsTheme {
        WaterScreen(
            uiState = WaterScreenUiState(
                settings = settings,
                todaysIntakeMl = 1250,
                progress = 0.5f
            ),
            onLogWater = {},
            onNavigateToStats = {},
            onNavigateToSettings = {}
        )
    }
}