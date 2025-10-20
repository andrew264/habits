package com.andrew264.habits.ui.usage

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.components.FeatureToggleListItem
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.common.duration_picker.DurationPickerDialog
import com.andrew264.habits.ui.common.list_items.ListItemPosition
import com.andrew264.habits.ui.common.list_items.ListSectionHeader
import com.andrew264.habits.ui.common.list_items.NavigationListItem
import com.andrew264.habits.ui.common.list_items.ToggleListItem
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.Whitelist
import com.andrew264.habits.ui.settings.AccessibilityServiceDialog
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.flow.collectLatest

@Composable
fun UsageSettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    viewModel: UsageStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAccessibilityDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UsageSettingsEvent.ShowAccessibilityDialog -> {
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

    UsageSettingsScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onNavigate = onNavigate,
        onUsageTrackingToggled = viewModel::onAppUsageTrackingToggled,
        onSetAppBlockingEnabled = viewModel::setAppBlockingEnabled,
        onSetUsageLimitNotificationsEnabled = viewModel::setUsageLimitNotificationsEnabled,
        onSetSharedDailyLimit = viewModel::setSharedDailyLimit
    )
}

@Preview
@Composable
fun UsageSettingsScreenPreview() {
    val uiState = UsageStatsUiState(sharedDailyUsageLimitMinutes = 60, usageLimitNotificationsEnabled = true, isAppBlockingEnabled = false)
    UsageSettingsScreen(uiState = uiState, onNavigateUp = {}, onNavigate = {}, onUsageTrackingToggled = {}, onSetAppBlockingEnabled = {}, onSetUsageLimitNotificationsEnabled = {}, onSetSharedDailyLimit = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsageSettingsScreen(
    uiState: UsageStatsUiState,
    onNavigateUp: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onUsageTrackingToggled: (Boolean) -> Unit,
    onSetAppBlockingEnabled: (Boolean) -> Unit,
    onSetUsageLimitNotificationsEnabled: (Boolean) -> Unit,
    onSetSharedDailyLimit: (minutes: Int?) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showSharedLimitDialog by rememberSaveable { mutableStateOf(false) }

    if (showSharedLimitDialog) {
        DurationPickerDialog(
            title = stringResource(R.string.usage_set_shared_daily_limit_title),
            description = stringResource(R.string.usage_set_shared_daily_limit_description),
            initialTotalMinutes = uiState.sharedDailyUsageLimitMinutes ?: 0,
            onDismissRequest = { showSharedLimitDialog = false },
            onConfirm = { totalMinutes ->
                onSetSharedDailyLimit(if (totalMinutes > 0) totalMinutes else null)
                showSharedLimitDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            SimpleTopAppBar(title = stringResource(R.string.usage_settings_title), onNavigateUp = onNavigateUp, scrollBehavior = scrollBehavior)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(all = Dimens.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            item {
                FeatureToggleListItem(
                    title = stringResource(R.string.usage_enable_usage_tracking),
                    checked = uiState.isAppUsageTrackingEnabled,
                    onCheckedChange = onUsageTrackingToggled
                )
            }
            item {
                Column {
                    ListSectionHeader(stringResource(R.string.usage_limits))
                    Column(modifier = Modifier.clip(MaterialTheme.shapes.large)) {
                        NavigationListItem(
                            icon = Icons.Outlined.Timer,
                            title = stringResource(R.string.usage_shared_daily_limit),
                            onClick = { showSharedLimitDialog = true },
                            position = ListItemPosition.TOP,
                            enabled = uiState.isAppUsageTrackingEnabled,
                            valueContent = {
                                Text(
                                    text = if (uiState.sharedDailyUsageLimitMinutes != null) FormatUtils.formatDuration(uiState.sharedDailyUsageLimitMinutes * 60_000L) else stringResource(
                                        R.string.usage_detail_not_set
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        ToggleListItem(
                            icon = Icons.Outlined.Notifications,
                            title = stringResource(R.string.usage_enable_limit_notifications),
                            summary = stringResource(R.string.usage_enable_limit_notifications_summary),
                            checked = uiState.usageLimitNotificationsEnabled,
                            onCheckedChange = onSetUsageLimitNotificationsEnabled,
                            enabled = uiState.isAppUsageTrackingEnabled,
                            position = ListItemPosition.MIDDLE
                        )
                        ToggleListItem(
                            icon = Icons.Outlined.Block,
                            title = stringResource(R.string.usage_enable_app_blocker),
                            summary = stringResource(R.string.usage_enable_app_blocker_summary),
                            checked = uiState.isAppBlockingEnabled,
                            onCheckedChange = onSetAppBlockingEnabled,
                            enabled = uiState.isAppUsageTrackingEnabled,
                            position = ListItemPosition.BOTTOM
                        )
                    }
                }
            }
            item {
                Column {
                    ListSectionHeader(stringResource(R.string.usage_apps))
                    NavigationListItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        title = stringResource(R.string.usage_manage_whitelisted_apps),
                        onClick = { onNavigate(Whitelist) },
                        enabled = uiState.isAppUsageTrackingEnabled,
                        position = ListItemPosition.SEPARATE
                    )
                }
            }
        }
    }
}