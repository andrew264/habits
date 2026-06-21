package com.andrew264.habits.ui.usage.whitelist

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.common.components.IconSwitch
import com.andrew264.habits.ui.common.components.list.ContainedLazyColumn
import com.andrew264.habits.ui.common.components.list.ListItemPosition
import com.andrew264.habits.ui.common.components.list.toShapes
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
                ) { app, position ->
                    val isWhitelisted = app.packageName in uiState.whitelistedPackageNames

                    Column {
                        val toggleAction: (Boolean) -> Unit = { newChecked: Boolean ->
                            onToggleWhitelist(app, isWhitelisted)
                            val feedback = if (newChecked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                            view.performHapticFeedback(feedback)
                        }

                        val colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            selectedContainerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            selectedContentColor = MaterialTheme.colorScheme.onSurface,
                            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLeadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTrailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val leadingNode: @Composable () -> Unit = {
                            val icon = rememberAppIcon(packageName = app.packageName)
                            DrawableImage(
                                drawable = icon,
                                contentDescription = stringResource(R.string.whitelist_icon_content_description, app.friendlyName),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        val contentNode: @Composable () -> Unit = { Text(app.friendlyName) }
                        val supportingNode: @Composable () -> Unit = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) }
                        val trailingNode: @Composable () -> Unit = {
                            IconSwitch(
                                checked = isWhitelisted,
                                onCheckedChange = null, // Row toggles the state natively
                                enabled = true
                            )
                        }

                        // Because the action is purely a checkbox toggle, we use the toggleable overload
                        if (position == ListItemPosition.SEPARATE) {
                            ListItem(
                                checked = isWhitelisted,
                                onCheckedChange = toggleAction,
                                shapes = position.toShapes(),
                                colors = colors,
                                leadingContent = leadingNode,
                                content = contentNode,
                                supportingContent = supportingNode,
                                trailingContent = trailingNode
                            )
                        } else {
                            SegmentedListItem(
                                checked = isWhitelisted,
                                onCheckedChange = toggleAction,
                                shapes = position.toShapes(),
                                colors = colors,
                                leadingContent = leadingNode,
                                content = contentNode,
                                supportingContent = supportingNode,
                                trailingContent = trailingNode
                            )
                        }

                        if (position == ListItemPosition.TOP || position == ListItemPosition.MIDDLE) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest, thickness = 2.dp)
                        }
                    }
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