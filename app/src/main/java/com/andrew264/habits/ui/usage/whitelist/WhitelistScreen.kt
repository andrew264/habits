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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun WhitelistScreen(
    viewModel: WhitelistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.PaddingLarge)
    ) {
        // Search and Filter controls
        OutlinedTextField(
            value = uiState.searchText,
            onValueChange = viewModel::onSearchTextChanged,
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
                    viewModel.onToggleShowSystemApps(it)
                    view.performHapticFeedback(if (it) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
                }
            )
        }

        // App List
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Dimens.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.apps, key = { it.packageName }) { app ->
                    val isWhitelisted = app.packageName in uiState.whitelistedPackageNames
                    ListItem(
                        headlineContent = { Text(app.friendlyName) },
                        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = {
                            DrawableImage(
                                drawable = app.icon,
                                contentDescription = "${app.friendlyName} icon",
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isWhitelisted,
                                onCheckedChange = { _ ->
                                    viewModel.onToggleWhitelist(app, isWhitelisted)
                                    view.performHapticFeedback(if (!isWhitelisted) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}