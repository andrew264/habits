package com.andrew264.habits.ui.usage

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.domain.model.AppSegment
import com.andrew264.habits.domain.model.ScreenOnPeriod
import com.andrew264.habits.domain.model.UsageTimelineModel
import com.andrew264.habits.ui.bedtime.BedtimeChartRange
import com.andrew264.habits.ui.common.charts.TimelineLabelStrategy
import com.andrew264.habits.ui.common.components.FeatureDisabledContent
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.MonitoringSettings
import com.andrew264.habits.ui.navigation.Whitelist
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.components.AppListItem
import com.andrew264.habits.ui.usage.components.AppUsageTimelineChart
import com.andrew264.habits.ui.usage.components.ColorPickerDialog
import com.andrew264.habits.ui.usage.components.StatisticsSummaryCard
import java.util.concurrent.TimeUnit

@Composable
fun UsageTimelineScreen(
    viewModel: UsageTimelineViewModel = hiltViewModel(),
    onNavigate: (AppRoute) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
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
                onShowColorPicker = viewModel::showColorPickerForApp,
                onDismissColorPicker = viewModel::dismissColorPicker,
                onSetAppColor = viewModel::setAppColor,
                onNavigate = onNavigate
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun UsageTimelineContent(
    uiState: UsageTimelineUiState,
    onSetTimeRange: (BedtimeChartRange) -> Unit,
    onShowColorPicker: (AppDetails) -> Unit,
    onDismissColorPicker: () -> Unit,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val view = LocalView.current

    if (uiState.appForColorPicker != null) {
        val app = uiState.appForColorPicker
        ColorPickerDialog(
            dialogTitle = "Select color for ${app.friendlyName}",
            selectedColorHex = app.color,
            onDismissRequest = onDismissColorPicker,
            onColorSelected = { colorHex ->
                onSetAppColor(app.packageName, colorHex)
                onDismissColorPicker()
            }
        )
    }

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
                                }
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

        FilledTonalButton(onClick = { onNavigate(Whitelist) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Manage Whitelisted Apps")
        }

        // Chart
        if (uiState.timelineModel != null) {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.PaddingLarge)
                ) {
                    Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.PaddingSmall))
                    AppUsageTimelineChart(
                        model = uiState.timelineModel,
                        labelStrategy = if (uiState.selectedRange == BedtimeChartRange.TWELVE_HOURS) TimelineLabelStrategy.TWELVE_HOURS else TimelineLabelStrategy.DAY,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            }
        }

        // App List
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "App Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = Dimens.PaddingSmall, top = Dimens.PaddingSmall)
            )

            // If the whitelist is empty, show a prompt to manage it.
            if (uiState.appDetails.isEmpty() && uiState.timelineModel?.screenOnPeriods?.any { it.appSegments.isNotEmpty() } == true) {
                Card(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(Whitelist) }) {
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
                    AppSegment("com.android.chrome", startTime, startTime + TimeUnit.HOURS.toMillis(1), "#4CAF50"),
                    AppSegment("com.google.android.gm", startTime + TimeUnit.HOURS.toMillis(1), startTime + TimeUnit.HOURS.toMillis(2), "#F44336")
                )
            ),
            ScreenOnPeriod(
                startTimestamp = startTime + TimeUnit.HOURS.toMillis(4),
                endTimestamp = startTime + TimeUnit.HOURS.toMillis(6),
                appSegments = listOf(
                    AppSegment("com.twitter.android", startTime + TimeUnit.HOURS.toMillis(4), startTime + TimeUnit.HOURS.toMillis(6), "#2196F3")
                )
            )
        ),
        pickupCount = 2,
        totalScreenOnTime = TimeUnit.HOURS.toMillis(4)
    )

    val fakeAppDetails = listOf(
        AppDetails("com.android.chrome", "Chrome", null, "#4CAF50", TimeUnit.HOURS.toMillis(1), 0.25f, 1),
        AppDetails("com.google.android.gm", "Gmail", null, "#F44336", TimeUnit.HOURS.toMillis(1), 0.25f, 1),
        AppDetails("com.twitter.android", "Twitter", null, "#2196F3", TimeUnit.HOURS.toMillis(2), 0.5f, 1),
    )

    HabitsTheme {
        UsageTimelineContent(
            uiState = UsageTimelineUiState(
                isLoading = false,
                isAppUsageTrackingEnabled = true,
                timelineModel = fakeTimelineModel,
                appDetails = fakeAppDetails,
                totalScreenOnTime = fakeTimelineModel.totalScreenOnTime,
                pickupCount = fakeTimelineModel.pickupCount,
                averageSessionMillis = fakeTimelineModel.totalScreenOnTime / fakeTimelineModel.pickupCount
            ),
            onSetTimeRange = {},
            onShowColorPicker = {},
            onDismissColorPicker = {},
            onSetAppColor = { _, _ -> },
            onNavigate = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageTimelineFeatureDisabledPreview() {
    HabitsTheme {
        UsageTimelineScreen(onNavigate = {})
    }
}