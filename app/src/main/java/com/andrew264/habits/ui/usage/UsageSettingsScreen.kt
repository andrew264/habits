package com.andrew264.habits.ui.usage

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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.ui.common.components.ListItemPosition
import com.andrew264.habits.ui.common.components.NavigationSettingsListItem
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.common.components.ToggleSettingsListItem
import com.andrew264.habits.ui.common.duration_picker.DurationPickerDialog
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.Whitelist
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun UsageSettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    viewModel: UsageStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    UsageSettingsScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onNavigate = onNavigate,
        onSetAppBlockingEnabled = viewModel::setAppBlockingEnabled,
        onSetUsageLimitNotificationsEnabled = viewModel::setUsageLimitNotificationsEnabled,
        onSetSharedDailyLimit = viewModel::setSharedDailyLimit
    )
}

@Preview
@Composable
fun UsageSettingsScreenPreview() {
    val uiState = UsageStatsUiState(sharedDailyUsageLimitMinutes = 60, usageLimitNotificationsEnabled = true, isAppBlockingEnabled = false)
    UsageSettingsScreen(uiState = uiState, onNavigateUp = {}, onNavigate = {}, onSetAppBlockingEnabled = {}, onSetUsageLimitNotificationsEnabled = {}, onSetSharedDailyLimit = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsageSettingsScreen(
    uiState: UsageStatsUiState,
    onNavigateUp: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSetAppBlockingEnabled: (Boolean) -> Unit,
    onSetUsageLimitNotificationsEnabled: (Boolean) -> Unit,
    onSetSharedDailyLimit: (minutes: Int?) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            SimpleTopAppBar(title = "Usage Settings", onNavigateUp = onNavigateUp, scrollBehavior = scrollBehavior)
        }
    ) { paddingValues ->
        var showSharedLimitDialog by rememberSaveable { mutableStateOf(false) }

        if (showSharedLimitDialog) {
            DurationPickerDialog(
                title = "Set Shared Daily Limit",
                description = "Set a total time limit for all whitelisted apps. This limit will reset at midnight. Set to 0 to clear.",
                initialTotalMinutes = uiState.sharedDailyUsageLimitMinutes ?: 0,
                onDismissRequest = { showSharedLimitDialog = false },
                onConfirm = { totalMinutes ->
                    onSetSharedDailyLimit(if (totalMinutes > 0) totalMinutes else null)
                    showSharedLimitDialog = false
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(all = Dimens.PaddingSmall)
        ) {
            item {
                SectionHeader("Limits")
            }
            item {
                NavigationSettingsListItem(
                    icon = Icons.Outlined.Timer,
                    title = "Shared Daily Limit",
                    onClick = { showSharedLimitDialog = true },
                    position = ListItemPosition.TOP,
                    valueContent = {
                        Text(
                            text = if (uiState.sharedDailyUsageLimitMinutes != null) FormatUtils.formatDuration(uiState.sharedDailyUsageLimitMinutes * 60_000L) else "Not set",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            item {
                ToggleSettingsListItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Enable Limit Notifications",
                    summary = "Get notified when you exceed a usage limit.",
                    checked = uiState.usageLimitNotificationsEnabled,
                    onCheckedChange = onSetUsageLimitNotificationsEnabled,
                    position = ListItemPosition.MIDDLE
                )
            }
            item {
                ToggleSettingsListItem(
                    icon = Icons.Outlined.Block,
                    title = "Enable App Blocker",
                    summary = "Show an overlay when a usage limit is reached.",
                    checked = uiState.isAppBlockingEnabled,
                    onCheckedChange = onSetAppBlockingEnabled,
                    position = ListItemPosition.BOTTOM
                )
            }
            item {
                Spacer(Modifier.height(Dimens.PaddingLarge))
            }
            item {
                SectionHeader("Apps")
            }
            item {
                NavigationSettingsListItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                    title = "Manage Whitelisted Apps",
                    onClick = { onNavigate(Whitelist) },
                    position = ListItemPosition.SEPARATE
                )
            }
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
