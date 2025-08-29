package com.andrew264.habits.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.ui.common.components.ListItemPosition
import com.andrew264.habits.ui.common.components.NavigationSettingsListItem
import com.andrew264.habits.ui.common.components.SectionHeader
import com.andrew264.habits.ui.common.components.ToggleSettingsListItem
import com.andrew264.habits.ui.navigation.*
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onRequestActivityPermission: () -> Unit,
    onNavigate: (AppRoute) -> Unit
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
                SettingsEvent.RequestActivityPermission -> {
                    onRequestActivityPermission()
                }

                SettingsEvent.ShowAccessibilityDialog -> {
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

    SettingsScreen(
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
        },
        onNavigate = onNavigate
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    uiState: SettingsUiState,
    onBedtimeToggled: (Boolean) -> Unit,
    onUsageToggled: (Boolean) -> Unit,
    onWaterToggled: (Boolean) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(text = "Settings") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(all = Dimens.PaddingSmall)
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
                    onClick = { onNavigate(BedtimeSettings) },
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
                    onClick = { onNavigate(UsageSettings) },
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
                    onClick = { onNavigate(WaterSettings) },
                    position = ListItemPosition.BOTTOM
                )
            }

            item {
                Spacer(Modifier.height(Dimens.PaddingLarge))
            }

            item {
                SectionHeader("Management")
            }

            item {
                NavigationSettingsListItem(
                    icon = Icons.Outlined.Schedule,
                    title = "Create and manage schedules",
                    onClick = { onNavigate(Schedules) },
                    position = ListItemPosition.SEPARATE
                )
            }

            item {
                Spacer(Modifier.height(Dimens.PaddingLarge))
            }

            item {
                SectionHeader("Data & Privacy")
            }

            item {
                NavigationSettingsListItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Delete Data",
                    onClick = { onNavigate(Privacy) },
                    position = ListItemPosition.TOP
                )
            }

            item {
                NavigationSettingsListItem(
                    icon = Icons.Outlined.Info,
                    title = "App Permissions & Info",
                    onClick = onOpenAppSettings,
                    position = ListItemPosition.BOTTOM
                )
            }
        }
    }
}

@Preview(name = "Settings - All Enabled", showBackground = true)
@Composable
private fun SettingsScreenAllEnabledPreview() {
    val settings = createPreviewPersistentSettings()
    HabitsTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                settings = settings,
                isAccessibilityServiceEnabled = true
            ),
            onBedtimeToggled = {},
            onUsageToggled = {},
            onWaterToggled = {},
            onOpenAppSettings = {},
            onOpenAccessibilitySettings = {},
            onNavigate = {}
        )
    }
}

@Preview(name = "Settings - Accessibility Warning", showBackground = true)
@Composable
private fun SettingsScreenWarningPreview() {
    val settings = createPreviewPersistentSettings()
    HabitsTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                settings = settings,
                isAccessibilityServiceEnabled = false
            ),
            onBedtimeToggled = {},
            onUsageToggled = {},
            onWaterToggled = {},
            onOpenAppSettings = {},
            onOpenAccessibilitySettings = {},
            onNavigate = {}
        )
    }
}