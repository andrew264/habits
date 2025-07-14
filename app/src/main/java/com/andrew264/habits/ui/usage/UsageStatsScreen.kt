package com.andrew264.habits.ui.usage

import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.domain.model.UsageStatistics
import com.andrew264.habits.domain.model.UsageTimeBin
import com.andrew264.habits.ui.common.charts.StackedBarChart
import com.andrew264.habits.ui.common.color_picker.ColorPickerDialog
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.color_picker.utils.toHexCode
import com.andrew264.habits.ui.common.components.FeatureDisabledContent
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.MonitoringSettings
import com.andrew264.habits.ui.navigation.Whitelist
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.components.AppListItem
import com.andrew264.habits.ui.usage.components.StatisticsSummaryCard
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(
    viewModel: UsageStatsViewModel = hiltViewModel(),
    onNavigate: (AppRoute) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInitialComposition = remember { mutableStateOf(true) }
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isInitialComposition.value) {
            isInitialComposition.value = false
        } else {
            viewModel.refresh()
        }
    }

    when {
        uiState.isLoading && uiState.stats == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        !uiState.isAppUsageTrackingEnabled -> {
            FeatureDisabledContent(
                title = "Usage Tracking Disabled",
                description = "This feature uses the Accessibility Service to show you how you spend time on your phone. You can enable it in the Monitoring settings.",
                buttonText = "Go to Settings",
                onEnableClicked = { onNavigate(MonitoringSettings) }
            )
        }

        else -> {
            UsageStatsContent(
                uiState = uiState,
                onSetTimeRange = viewModel::setTimeRange,
                onRefresh = viewModel::refresh,
                onShowColorPicker = viewModel::showColorPickerForApp,
                onDismissColorPicker = viewModel::dismissColorPicker,
                onSetAppColor = viewModel::setAppColor,
                onNavigateToWhitelist = { onNavigate(Whitelist) },
                onOpenAccessibilitySettings = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }
    }
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun UsageStatsContent(
    uiState: UsageStatsUiState,
    onSetTimeRange: (UsageTimeRange) -> Unit,
    onRefresh: () -> Unit,
    onShowColorPicker: (AppDetails) -> Unit,
    onDismissColorPicker: () -> Unit,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val view = LocalView.current

    if (uiState.appForColorPicker != null) {
        val app = uiState.appForColorPicker
        ColorPickerDialog(
            title = "Select color for ${app.friendlyName}",
            initialColor = app.color.toColorOrNull() ?: Color.Gray,
            onDismissRequest = onDismissColorPicker,
            onConfirmation = { newColor ->
                onSetAppColor(app.packageName, newColor.toHexCode())
                onDismissColorPicker()
            },
            showAlphaSlider = false
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading && uiState.stats != null,
        onRefresh = onRefresh
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(all = Dimens.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val ranges = UsageTimeRange.entries
                    ButtonGroup(
                        overflowIndicator = { menuState ->
                            IconButton(onClick = { menuState.show() }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                        },
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        ranges.forEachIndexed { index, range ->
                            customItem(
                                buttonGroupContent = {
                                    ElevatedToggleButton(
                                        checked = uiState.selectedRange == range,
                                        onCheckedChange = {
                                            onSetTimeRange(range)
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            ranges.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                    ) {
                                        Text(range.label)
                                    }
                                },
                                menuContent = { menuState ->
                                    DropdownMenuItem(
                                        text = { Text(range.label) },
                                        onClick = {
                                            onSetTimeRange(range)
                                            menuState.dismiss()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            uiState.stats?.let { stats ->
                item {
                    StatisticsSummaryCard(
                        totalScreenOnTime = stats.totalScreenOnTime,
                        pickupCount = stats.pickupCount,
                        averageSessionMillis = uiState.averageSessionMillis
                    )
                }
            }

            item {
                AnimatedVisibility(visible = uiState.isAppUsageTrackingEnabled && !uiState.isAccessibilityServiceEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenAccessibilitySettings()
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Dimens.PaddingLarge),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Usage tracking service is not running. Tap here to fix it.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (uiState.stats != null) {
                item {
                    StackedBarChart(
                        bins = uiState.stats.timeBins,
                        range = uiState.selectedRange,
                        whitelistedAppColors = uiState.whitelistedAppColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = { onNavigateToWhitelist() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Manage Whitelisted Apps")
                }
            }

            stickyHeader {
                Text(
                    text = "App Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(bottom = Dimens.PaddingSmall, top = Dimens.PaddingSmall)
                )
            }

            if (uiState.appDetails.isEmpty() && uiState.stats?.totalUsagePerApp?.isNotEmpty() == true) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { onNavigateToWhitelist() }) {
                        Text(
                            "No whitelisted apps with usage in this period. Tap 'Manage Whitelisted Apps' to add some.",
                            modifier = Modifier.padding(Dimens.PaddingLarge),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.appDetails, key = { it.packageName }) { app ->
                AppListItem(
                    appDetails = app,
                    onColorSwatchClick = {
                        onShowColorPicker(app)
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageStatsContentPreview() {
    val fakeStats = UsageStatistics(
        timeBins = (0..23).map {
            UsageTimeBin(0, 0, (10..60).random() * 60_000L, emptyMap())
        },
        totalScreenOnTime = TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(32),
        pickupCount = 58,
        totalUsagePerApp = emptyMap()
    )

    HabitsTheme {
        UsageStatsContent(
            uiState = UsageStatsUiState(
                isLoading = false,
                isAppUsageTrackingEnabled = true,
                isAccessibilityServiceEnabled = true,
                stats = fakeStats,
                averageSessionMillis = fakeStats.totalScreenOnTime / fakeStats.pickupCount
            ),
            onSetTimeRange = {},
            onRefresh = {},
            onShowColorPicker = {},
            onDismissColorPicker = {},
            onSetAppColor = { _, _ -> },
            onNavigateToWhitelist = {},
            onOpenAccessibilitySettings = {}
        )
    }
}