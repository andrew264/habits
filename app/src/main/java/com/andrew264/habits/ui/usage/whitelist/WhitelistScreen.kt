package com.andrew264.habits.ui.usage.whitelist

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.common.utils.rememberAppIcon
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
fun WhitelistScreen(viewModel: WhitelistViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    WhitelistScreen(
        uiState = uiState,
        onSearchTextChanged = viewModel::onSearchTextChanged,
        onToggleShowSystemApps = viewModel::onToggleShowSystemApps,
        onToggleWhitelist = viewModel::onToggleWhitelist
    )
}

@Composable
private fun WhitelistScreen(
    uiState: WhitelistUiState,
    onSearchTextChanged: (String) -> Unit,
    onToggleShowSystemApps: (Boolean) -> Unit,
    onToggleWhitelist: (app: InstalledAppInfo, isWhitelisted: Boolean) -> Unit
) {
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.PaddingLarge)
    ) {

        OutlinedTextField(
            value = uiState.searchText,
            onValueChange = onSearchTextChanged,
            label = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.PaddingLarge),
            singleLine = true
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text("Show system apps", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(Dimens.PaddingSmall))
            Switch(
                checked = uiState.showSystemApps,
                onCheckedChange = {
                    onToggleShowSystemApps(it)
                    val feedback = if (it) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                    view.performHapticFeedback(feedback)
                }
            )
        }


        if (uiState.isLoading) {
            ContainedLoadingIndicator()
        } else {
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.PaddingMedium,
                    end = Dimens.PaddingMedium,
                    top = Dimens.PaddingMedium,
                    bottom = Dimens.PaddingMedium + navBarPadding
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.apps, key = { it.packageName }) { app ->
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
                            Switch(
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
            onSearchTextChanged = {},
            onToggleShowSystemApps = {},
            onToggleWhitelist = { _, _ -> }
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
            onSearchTextChanged = {},
            onToggleShowSystemApps = {},
            onToggleWhitelist = { _, _ -> }
        )
    }
}