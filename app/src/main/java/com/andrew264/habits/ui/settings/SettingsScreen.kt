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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.common.list_items.ListItemPosition
import com.andrew264.habits.ui.common.list_items.ListSectionHeader
import com.andrew264.habits.ui.common.list_items.NavigationListItem
import com.andrew264.habits.ui.common.list_items.ToggleListItem
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
            SimpleTopAppBar(stringResource(R.string.app_route_settings), scrollBehavior = scrollBehavior)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = Dimens.PaddingLarge))
        ) {
            item {
                ListSectionHeader(stringResource(R.string.settings_features))
            }

            item {
                ToggleListItem(
                    icon = Icons.Outlined.Alarm,
                    title = stringResource(R.string.settings_bedtime_tracking),
                    summary = stringResource(R.string.settings_bedtime_tracking_summary),
                    checked = uiState.settings.isBedtimeTrackingEnabled,
                    onCheckedChange = onBedtimeToggled,
                    onClick = { onNavigate(BedtimeSettings) },
                    position = ListItemPosition.TOP
                )
            }

            item {
                ToggleListItem(
                    icon = Icons.Outlined.Timeline,
                    title = stringResource(R.string.settings_app_usage_tracking),
                    summary = stringResource(R.string.settings_app_usage_tracking_summary),
                    checked = uiState.settings.isAppUsageTrackingEnabled,
                    onCheckedChange = onUsageToggled,
                    onClick = { onNavigate(UsageSettings) },
                    position = ListItemPosition.MIDDLE,
                    isWarningVisible = uiState.settings.isAppUsageTrackingEnabled && !uiState.isAccessibilityServiceEnabled,
                    warningText = stringResource(R.string.settings_service_not_running),
                    onWarningClick = onOpenAccessibilitySettings
                )
            }

            item {
                ToggleListItem(
                    icon = Icons.Outlined.WaterDrop,
                    title = stringResource(R.string.settings_water_tracking),
                    summary = stringResource(R.string.settings_water_tracking_summary),
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
                ListSectionHeader(stringResource(R.string.settings_management))
            }

            item {
                NavigationListItem(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.settings_create_and_manage_schedules),
                    onClick = { onNavigate(Schedules) },
                    position = ListItemPosition.SEPARATE
                )
            }

            item {
                Spacer(Modifier.height(Dimens.PaddingLarge))
            }

            item {
                ListSectionHeader(stringResource(R.string.settings_data_and_privacy))
            }

            item {
                NavigationListItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = stringResource(R.string.data_management_delete_data),
                    onClick = { onNavigate(Privacy) },
                    position = ListItemPosition.TOP
                )
            }

            item {
                NavigationListItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_app_permissions_and_info),
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