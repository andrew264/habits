package com.andrew264.habits.ui.usage.whitelist

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.common.components.IconSwitch
import com.andrew264.habits.ui.common.list_items.ContainedLazyColumn
import com.andrew264.habits.ui.common.utils.rememberAppIcon
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.whitelist.components.WhitelistTopAppBar

@Composable
fun WhitelistScreen(
    viewModel: WhitelistViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    WhitelistScreen(
        uiState = uiState,
        onSearchTextChanged = viewModel::onSearchTextChanged,
        onToggleShowSystemApps = viewModel::onToggleShowSystemApps,
        onToggleWhitelist = viewModel::onToggleWhitelist,
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhitelistScreen(
    uiState: WhitelistUiState,
    onSearchTextChanged: (String) -> Unit,
    onToggleShowSystemApps: () -> Unit,
    onToggleWhitelist: (app: InstalledAppInfo, isWhitelisted: Boolean) -> Unit,
    onNavigateUp: () -> Unit
) {
    val view = LocalView.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            WhitelistTopAppBar(
                searchText = uiState.searchText,
                onSearchTextChanged = onSearchTextChanged,
                showSystemApps = uiState.showSystemApps,
                onToggleShowSystemApps = onToggleShowSystemApps,
                onNavigateUp = onNavigateUp,
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                ContainedLoadingIndicator()
            } else {
                ContainedLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.PaddingLarge),
                    items = uiState.apps,
                    key = { it.packageName }
                ) { app ->
                    val isWhitelisted = app.packageName in uiState.whitelistedPackageNames
                    ListItem(
                        headlineContent = { Text(app.friendlyName) },
                        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = {
                            val icon = rememberAppIcon(packageName = app.packageName)
                            DrawableImage(
                                drawable = icon,
                                contentDescription = "${app.friendlyName} icon",
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        trailingContent = {
                            IconSwitch(
                                checked = isWhitelisted,
                                onCheckedChange = { _ ->
                                    onToggleWhitelist(app, isWhitelisted)
                                    val feedback = if (!isWhitelisted) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                                    view.performHapticFeedback(feedback)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WhitelistScreenPreview() {
    val sampleApps = listOf(
        InstalledAppInfo("com.google.android.youtube", "YouTube", false),
        InstalledAppInfo("com.google.android.gm", "Gmail", false),
        InstalledAppInfo("com.android.settings", "Settings", true)
    )
    val whitelisted = setOf("com.google.android.youtube")

    HabitsTheme {
        WhitelistScreen(
            uiState = WhitelistUiState(
                isLoading = false,
                searchText = "",
                showSystemApps = false,
                apps = sampleApps.filter { !it.isSystemApp },
                whitelistedPackageNames = whitelisted
            ),
            onToggleShowSystemApps = {},
            onToggleWhitelist = { _, _ -> },
            onSearchTextChanged = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WhitelistScreenSystemAppsPreview() {
    val sampleApps = listOf(
        InstalledAppInfo("com.google.android.youtube", "YouTube", false),
        InstalledAppInfo("com.google.android.gm", "Gmail", false),
        InstalledAppInfo("com.android.settings", "Settings", true)
    )
    val whitelisted = setOf("com.google.android.youtube")

    HabitsTheme {
        WhitelistScreen(
            uiState = WhitelistUiState(
                isLoading = false,
                searchText = "",
                showSystemApps = true,
                apps = sampleApps,
                whitelistedPackageNames = whitelisted
            ),
            onToggleShowSystemApps = {},
            onToggleWhitelist = { _, _ -> },
            onSearchTextChanged = {},
            onNavigateUp = {}
        )
    }
}