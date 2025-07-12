package com.andrew264.habits.ui.usage

import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.domain.model.AppSegment
import com.andrew264.habits.domain.model.ScreenOnPeriod
import com.andrew264.habits.domain.model.UsageTimelineModel
import com.andrew264.habits.ui.bedtime.BedtimeChartRange
import com.andrew264.habits.ui.common.charts.DualTrackTimelineChart
import com.andrew264.habits.ui.common.charts.TimelineLabelStrategy
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
fun UsageTimelineScreen(
    viewModel: UsageTimelineViewModel = hiltViewModel(),
    onNavigate: (AppRoute) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInitialComposition = remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Refresh data automaticall
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isInitialComposition.value) {
            isInitialComposition.value = false
        } else {
            viewModel.refresh()
        }
    }

    when {
        // Show a loading spinner only on initial load, not on refresh
        uiState.isLoading && uiState.timelineModel == null -> {
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
            UsageTimelineContent(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun UsageTimelineContent(
    uiState: UsageTimelineUiState,
    onSetTimeRange: (BedtimeChartRange) -> Unit,
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
        isRefreshing = uiState.isLoading && uiState.timelineModel != null,
        onRefresh = onRefresh
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            // Time Range Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val ranges = BedtimeChartRange.entries.filter { it.isLinear }
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

            // Summary Stats
            StatisticsSummaryCard(
                totalScreenOnTime = uiState.totalScreenOnTime,
                pickupCount = uiState.pickupCount,
                averageSessionMillis = uiState.averageSessionMillis
            )

            // Warning message for accessibility service status
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


            // Chart
            if (uiState.timelineModel != null) {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.PaddingLarge),
                        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                    ) {
                        Text(
                            "Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        DualTrackTimelineChart(
                            model = uiState.timelineModel,
                            labelStrategy = if (uiState.selectedRange == BedtimeChartRange.TWELVE_HOURS) TimelineLabelStrategy.TWELVE_HOURS else TimelineLabelStrategy.DAY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

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

            // App List
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "App Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = Dimens.PaddingSmall, top = Dimens.PaddingSmall)
                )

                if (uiState.appDetails.isEmpty() && uiState.timelineModel?.screenOnPeriods?.any { it.appSegments.isNotEmpty() } == true) {
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

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraSmall)
                ) {
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
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageTimelineContentPreview() {
    val now = System.currentTimeMillis()
    val startTime = now - TimeUnit.HOURS.toMillis(12)

    val fakeTimelineModel = UsageTimelineModel(
        viewStart = startTime,
        viewEnd = now,
        screenOnPeriods = listOf(
            ScreenOnPeriod(
                startTimestamp = startTime,
                endTimestamp = startTime + TimeUnit.HOURS.toMillis(2),
                appSegments = listOf(
                    AppSegment(
                        "com.android.chrome",
                        startTime,
                        startTime + TimeUnit.HOURS.toMillis(1),
                        "#4CAF50"
                    ),
                    AppSegment(
                        "com.google.android.gm",
                        startTime + TimeUnit.HOURS.toMillis(1),
                        startTime + TimeUnit.HOURS.toMillis(2),
                        "#F44336"
                    )
                )
            ),
            ScreenOnPeriod(
                startTimestamp = startTime + TimeUnit.HOURS.toMillis(4),
                endTimestamp = startTime + TimeUnit.HOURS.toMillis(6),
                appSegments = listOf(
                    AppSegment(
                        "com.twitter.android",
                        startTime + TimeUnit.HOURS.toMillis(4),
                        startTime + TimeUnit.HOURS.toMillis(6),
                        "#2196F3"
                    )
                )
            )
        ),
        pickupCount = 2,
        totalScreenOnTime = TimeUnit.HOURS.toMillis(4)
    )

    val fakeAppDetails = listOf(
        AppDetails(
            "com.android.chrome",
            "Chrome",
            null,
            "#4CAF50",
            TimeUnit.HOURS.toMillis(1),
            0.25f,
            1
        ),
        AppDetails(
            "com.google.android.gm",
            "Gmail",
            null,
            "#F44336",
            TimeUnit.HOURS.toMillis(1),
            0.25f,
            1
        ),
        AppDetails(
            "com.twitter.android",
            "Twitter",
            null,
            "#2196F3",
            TimeUnit.HOURS.toMillis(2),
            0.5f,
            1
        ),
    )

    HabitsTheme {
        UsageTimelineContent(
            uiState = UsageTimelineUiState(
                isLoading = false,
                isAppUsageTrackingEnabled = true,
                isAccessibilityServiceEnabled = false,
                timelineModel = fakeTimelineModel,
                appDetails = fakeAppDetails,
                totalScreenOnTime = fakeTimelineModel.totalScreenOnTime,
                pickupCount = fakeTimelineModel.pickupCount,
                averageSessionMillis = fakeTimelineModel.totalScreenOnTime / fakeTimelineModel.pickupCount
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

@Preview(showBackground = true)
@Composable
private fun UsageTimelineFeatureDisabledPreview() {
    HabitsTheme {
        UsageTimelineContent(
            uiState = UsageTimelineUiState(
                isLoading = false,
                isAppUsageTrackingEnabled = true,
                isAccessibilityServiceEnabled = false,
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