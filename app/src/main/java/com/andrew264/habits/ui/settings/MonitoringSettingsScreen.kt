package com.andrew264.habits.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.ui.common.components.SettingsRow
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
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
        }
    )
}

@Composable
private fun MonitoringSettingsScreen(
    modifier: Modifier = Modifier,
    uiState: MonitoringSettingsUiState,
    onBedtimeToggled: (Boolean) -> Unit,
    onUsageToggled: (Boolean) -> Unit,
    onWaterToggled: (Boolean) -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    HapticInteractionEffect(interactionSource)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Enable or disable major features of the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

                SettingsRow(
                    text = "Bedtime Tracking",
                    description = "Uses the Sleep API and schedules to track sleep patterns. Requires Physical Activity permission.",
                    checked = uiState.settings.isBedtimeTrackingEnabled,
                    onCheckedChange = onBedtimeToggled
                )
                SettingsRow(
                    text = "App Usage Tracking",
                    description = "Uses the Accessibility Service to show how you spend time on your phone.",
                    checked = uiState.settings.isAppUsageTrackingEnabled,
                    onCheckedChange = onUsageToggled
                )


                AnimatedVisibility(visible = uiState.settings.isAppUsageTrackingEnabled && !uiState.isAccessibilityServiceEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.PaddingExtraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Service is not running. Please re-enable it in system settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                SettingsRow(
                    text = "Water Tracking",
                    description = "Track daily water intake and set reminders.",
                    checked = uiState.settings.isWaterTrackingEnabled,
                    onCheckedChange = onWaterToggled
                )
            }
        }


        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                Text(
                    text = "App Permissions & Info",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(
                    onClick = onOpenAppSettings,
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open App Info")
                }
            }
        }
    }
}

@Preview(name = "Settings - All Enabled", showBackground = true)
@Composable
private fun MonitoringSettingsScreenAllEnabledPreview() {
    val settings = PersistentSettings(
        selectedScheduleId = null,
        isBedtimeTrackingEnabled = true,
        isAppUsageTrackingEnabled = true,
        usageLimitNotificationsEnabled = true,
        isWaterTrackingEnabled = true,
        waterDailyTargetMl = 2500,
        isWaterReminderEnabled = true,
        waterReminderIntervalMinutes = 60,
        waterReminderSnoozeMinutes = 15,
        waterReminderScheduleId = null
    )
    HabitsTheme {
        MonitoringSettingsScreen(
            uiState = MonitoringSettingsUiState(
                settings = settings,
                isAccessibilityServiceEnabled = true
            ),
            onBedtimeToggled = {},
            onUsageToggled = {},
            onWaterToggled = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(name = "Settings - Accessibility Warning", showBackground = true)
@Composable
private fun MonitoringSettingsScreenWarningPreview() {
    val settings = PersistentSettings(
        selectedScheduleId = null,
        isBedtimeTrackingEnabled = true,
        isAppUsageTrackingEnabled = true,
        usageLimitNotificationsEnabled = true,
        isWaterTrackingEnabled = true,
        waterDailyTargetMl = 2500,
        isWaterReminderEnabled = true,
        waterReminderIntervalMinutes = 60,
        waterReminderSnoozeMinutes = 15,
        waterReminderScheduleId = null
    )
    HabitsTheme {
        MonitoringSettingsScreen(
            uiState = MonitoringSettingsUiState(
                settings = settings,
                isAccessibilityServiceEnabled = false
            ),
            onBedtimeToggled = {},
            onUsageToggled = {},
            onWaterToggled = {},
            onOpenAppSettings = {}
        )
    }
}