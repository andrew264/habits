package com.andrew264.habits.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.ui.common.components.ListItemPosition
import com.andrew264.habits.ui.common.components.NavigationSettingsListItem
import com.andrew264.habits.ui.common.components.ToggleSettingsListItem
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MonitoringSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MonitoringSettingsViewModel = hiltViewModel(),
    onRequestActivityPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAccessibilityDialog by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateAccessibilityStatus()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                MonitoringSettingsEvent.RequestActivityPermission -> {
                    onRequestActivityPermission()
                }

                MonitoringSettingsEvent.ShowAccessibilityDialog -> {
                    showAccessibilityDialog = true
                }
            }
        }
    }

    if (showAccessibilityDialog) {
        AccessibilityServiceDialog(
            onDismiss = { showAccessibilityDialog = false },
            onConfirm = {
                showAccessibilityDialog = false
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )
    }

    MonitoringSettingsScreen(
        modifier = modifier,
        uiState = uiState,
        onBedtimeToggled = viewModel::onBedtimeTrackingToggled,
        onUsageToggled = viewModel::onAppUsageTrackingToggled,
        onWaterToggled = viewModel::onWaterTrackingToggled,
        onOpenAppSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = android.net.Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        },
        onOpenAccessibilitySettings = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonitoringSettingsScreen(
    modifier: Modifier = Modifier,
    uiState: MonitoringSettingsUiState,
    onBedtimeToggled: (Boolean) -> Unit,
    onUsageToggled: (Boolean) -> Unit,
    onWaterToggled: (Boolean) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = Dimens.PaddingSmall,
                end = Dimens.PaddingSmall,
                top = Dimens.PaddingSmall,
                bottom = Dimens.PaddingSmall + navBarPadding
            )
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        item {
            SectionHeader("Features")
        }

        item {
            ToggleSettingsListItem(
                icon = Icons.Outlined.Alarm,
                title = "Bedtime Tracking",
                summary = "Monitor your sleep and bedtime habits",
                checked = uiState.settings.isBedtimeTrackingEnabled,
                onCheckedChange = onBedtimeToggled,
                position = ListItemPosition.TOP
            )
        }

        item {
            ToggleSettingsListItem(
                icon = Icons.Outlined.Timeline,
                title = "App Usage Tracking",
                summary = "Track screen time and set limits for apps.",
                checked = uiState.settings.isAppUsageTrackingEnabled,
                onCheckedChange = onUsageToggled,
                position = ListItemPosition.MIDDLE,
                isWarningVisible = uiState.settings.isAppUsageTrackingEnabled && !uiState.isAccessibilityServiceEnabled,
                warningText = "Service is not running. Tap to fix in accessibility settings.",
                onWarningClick = onOpenAccessibilitySettings
            )
        }

        item {
            ToggleSettingsListItem(
                icon = Icons.Outlined.WaterDrop,
                title = "Water Tracking",
                summary = "Track daily water intake and set reminders.",
                checked = uiState.settings.isWaterTrackingEnabled,
                onCheckedChange = onWaterToggled,
                position = ListItemPosition.BOTTOM
            )
        }

        item {
            Spacer(Modifier.height(Dimens.PaddingLarge))
        }

        item {
            SectionHeader("App Info")
        }

        item {
            NavigationSettingsListItem(
                icon = Icons.Outlined.Info,
                title = "App Permissions & Info",
                onClick = onOpenAppSettings,
                position = ListItemPosition.SEPARATE
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingSmall, vertical = Dimens.PaddingMedium)
    )
}

@Preview(name = "Settings - All Enabled", showBackground = true)
@Composable
private fun MonitoringSettingsScreenAllEnabledPreview() {
    val settings = createPreviewPersistentSettings()
    HabitsTheme {
        MonitoringSettingsScreen(
            uiState = MonitoringSettingsUiState(
                settings = settings,
                isAccessibilityServiceEnabled = true
            ),
            onBedtimeToggled = {},
            onUsageToggled = {},
            onWaterToggled = {},
            onOpenAppSettings = {},
            onOpenAccessibilitySettings = {}
        )
    }
}

@Preview(name = "Settings - Accessibility Warning", showBackground = true)
@Composable
private fun MonitoringSettingsScreenWarningPreview() {
    val settings = createPreviewPersistentSettings()
    HabitsTheme {
        MonitoringSettingsScreen(
            uiState = MonitoringSettingsUiState(
                settings = settings,
                isAccessibilityServiceEnabled = false
            ),
            onBedtimeToggled = {},
            onUsageToggled = {},
            onWaterToggled = {},
            onOpenAppSettings = {},
            onOpenAccessibilitySettings = {}
        )
    }
}