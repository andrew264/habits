package com.andrew264.habits.ui.settings

import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.domain.model.PersistentSettings
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
        },
        onOpenAccessibilitySettings = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
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
    onOpenAccessibilitySettings: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            SectionHeader("Features")
        }

        item {
            SettingsListItem(
                icon = Icons.Outlined.Alarm,
                title = "Bedtime Tracking",
                summary = "Monitor your sleep and bedtime habits",
                checked = uiState.settings.isBedtimeTrackingEnabled,
                onCheckedChange = onBedtimeToggled
            )
            HorizontalDivider()
        }

        item {
            SettingsListItem(
                icon = Icons.Outlined.Timeline,
                title = "App Usage Tracking",
                summary = "Track screen time and set limits for apps.",
                checked = uiState.settings.isAppUsageTrackingEnabled,
                onCheckedChange = onUsageToggled
            )
        }

        item {
            AnimatedVisibility(visible = uiState.settings.isAppUsageTrackingEnabled && !uiState.isAccessibilityServiceEnabled) {
                ListItem(
                    headlineContent = { Text("Service is not running. Please re-enable it in accessibility settings.") },
                    leadingContent = { Icon(Icons.Outlined.Warning, contentDescription = "Warning") },
                    modifier = Modifier.clickable(onClick = onOpenAccessibilitySettings),
                    colors = ListItemDefaults.colors(
                        headlineColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            SettingsListItem(
                icon = Icons.Outlined.WaterDrop,
                title = "Water Tracking",
                summary = "Track daily water intake and set reminders.",
                checked = uiState.settings.isWaterTrackingEnabled,
                onCheckedChange = onWaterToggled
            )
        }

        item {
            Spacer(Modifier.height(Dimens.PaddingLarge))
        }

        item {
            SectionHeader("App Info")
        }

        item {
            ListItem(
                headlineContent = { Text("App Permissions & Info") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenAppSettings)
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
            .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium)
    )
}

@Composable
private fun SettingsListItem(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null, // Handled by clickable modifier on parent
                enabled = enabled,
                interactionSource = interactionSource
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = {
                    val newChecked = !checked
                    onCheckedChange(newChecked)
                    val feedback = if (newChecked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                    view.performHapticFeedback(feedback)
                }
            ),
        colors = ListItemDefaults.colors(
            headlineColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            supportingColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            leadingIconColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    )
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
            onOpenAppSettings = {},
            onOpenAccessibilitySettings = {}
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
            onOpenAppSettings = {},
            onOpenAccessibilitySettings = {}
        )
    }
}