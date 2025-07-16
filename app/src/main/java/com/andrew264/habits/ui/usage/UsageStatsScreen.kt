package com.andrew264.habits.ui.usage

import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
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
import com.andrew264.habits.ui.common.charts.BarChart
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.common.color_picker.ColorPicker
import com.andrew264.habits.ui.common.color_picker.ColorPickerState
import com.andrew264.habits.ui.common.color_picker.utils.toColor
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.common.components.FeatureDisabledContent
import com.andrew264.habits.ui.navigation.*
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.usage.components.AppListItem
import com.andrew264.habits.ui.usage.components.StatisticsSummaryCard
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun UsageStatsScreen(
    viewModel: UsageStatsViewModel = hiltViewModel(),
    onNavigate: (AppRoute) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInitialComposition = remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<UsageSelection>()

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
            NavigableListDetailPaneScaffold(
                navigator = scaffoldNavigator,
                listPane = {
                    AnimatedPane(
                        enterTransition = sharedAxisXEnter(forward = false),
                        exitTransition = sharedAxisXExit(forward = true)
                    ) {
                        UsageListContent(
                            uiState = uiState,
                            onSetTimeRange = viewModel::setTimeRange,
                            onRefresh = viewModel::refresh,
                            onAppSelected = { app ->
                                scope.launch {
                                    scaffoldNavigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        UsageSelection(app.packageName)
                                    )
                                }
                            },
                            onNavigateToWhitelist = { onNavigate(Whitelist) },
                            onOpenAccessibilitySettings = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                },
                detailPane = {
                    AnimatedPane(
                        enterTransition = sharedAxisXEnter(forward = true),
                        exitTransition = sharedAxisXExit(forward = false)
                    ) {
                        val selection = scaffoldNavigator.currentDestination?.contentKey
                        if (selection?.packageName != null) {
                            val selectedApp = uiState.appDetails.find { it.packageName == selection.packageName }
                            if (selectedApp != null) {
                                UsageDetailContent(
                                    app = selectedApp,
                                    onSetAppColor = viewModel::setAppColor
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(Dimens.PaddingLarge),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Select an app to see its details and set a color.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun UsageListContent(
    uiState: UsageStatsUiState,
    onSetTimeRange: (UsageTimeRange) -> Unit,
    onRefresh: () -> Unit,
    onAppSelected: (AppDetails) -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
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
                    TimeRangeButtonGroup(
                        selectedRange = uiState.selectedRange,
                        onSetTimeRange = onSetTimeRange
                    )
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
                    AccessibilityWarningCard(onOpenAccessibilitySettings)
                }
            }

            if (uiState.stats != null) {
                item {
                    com.andrew264.habits.ui.common.charts.StackedBarChart(
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
                    onClick = onNavigateToWhitelist,
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
                    Card(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToWhitelist) {
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
                    modifier = Modifier.clickable { onAppSelected(app) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
private fun TimeRangeButtonGroup(
    selectedRange: UsageTimeRange,
    onSetTimeRange: (UsageTimeRange) -> Unit
) {
    val view = LocalView.current
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
                        checked = selectedRange == range,
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

@Composable
private fun AccessibilityWarningCard(onOpenAccessibilitySettings: () -> Unit) {
    val view = LocalView.current
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

@Composable
private fun UsageDetailContent(
    app: AppDetails,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val isCompact = maxWidth < 600.dp
        if (isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                HeaderCard(app)
                KeyMetricsCard(app)
                if (app.historicalData.isNotEmpty()) {
                    UsageTrendCard(app)
                }
                ColorConfigurationCard(app, onSetAppColor)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(0.6f), // Give more space to the graph
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    HeaderCard(app)
                    if (app.historicalData.isNotEmpty()) {
                        UsageTrendCard(app)
                    }
                }
                Column(
                    modifier = Modifier.weight(0.4f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    KeyMetricsCard(app)
                    ColorConfigurationCard(app, onSetAppColor)
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(app: AppDetails) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            DrawableImage(drawable = app.icon, contentDescription = null, modifier = Modifier.size(56.dp))
            Column {
                Text(app.friendlyName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KeyMetricsCard(app: AppDetails) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingLarge),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "Total Time", value = formatDuration(app.totalUsageMillis))
            StatItem(label = "Avg. Session", value = formatDuration(app.averageSessionMillis))
            StatItem(label = "Sessions", value = app.sessionCount.toString())
        }
    }
}

@Composable
private fun UsageTrendCard(app: AppDetails) {
    val maxUsageMillis = app.historicalData.maxOfOrNull { it.value } ?: 0f
    val maxUsageMinutes = ceil(maxUsageMillis / 60000f).toInt()
    val topValueMinutes = when {
        maxUsageMinutes <= 1 -> 1
        maxUsageMinutes <= 2 -> 2
        maxUsageMinutes <= 5 -> 5
        maxUsageMinutes <= 10 -> 10
        maxUsageMinutes <= 15 -> 15
        maxUsageMinutes <= 30 -> 30
        maxUsageMinutes <= 45 -> 45
        maxUsageMinutes <= 60 -> 60
        else -> (ceil(maxUsageMinutes / 60f).toInt() * 60)
    }
    val topValueMillis = topValueMinutes * 60000f

    val yAxisLabelFormatter: (Float) -> String = { valueInMillis ->
        val minutes = (valueInMillis / 60000f).roundToInt()
        "${minutes}m"
    }

    Card {
        Column(
            modifier = Modifier.padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Usage Trend",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = app.peakUsageTimeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.PaddingLarge)
            )
            BarChart(
                entries = app.historicalData,
                barColor = app.color.toColorOrNull() ?: MaterialTheme.colorScheme.primary,
                topValue = topValueMillis,
                yAxisLabelFormatter = yAxisLabelFormatter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
private fun ColorConfigurationCard(
    app: AppDetails,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val colorPickerState = remember(app.packageName, app.color) {
        ColorPickerState(initialColor = app.color.toColorOrNull() ?: Color.Gray)
    }
    val view = LocalView.current
    var hasChanges by remember { mutableStateOf(false) }

    // Track if color has changed from initial
    LaunchedEffect(colorPickerState.hsvColor) {
        hasChanges = app.color.toColorOrNull() != colorPickerState.hsvColor.toColor()
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingLarge)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Display Color", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = colorPickerState.hsvColor.toColor(),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
                    TextButton(onClick = { isExpanded = !isExpanded }) {
                        Text(if (isExpanded) "Cancel" else "Change")
                    }
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = Dimens.PaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    ColorPicker(state = colorPickerState)
                    Button(
                        onClick = {
                            onSetAppColor(app.packageName, colorPickerState.hexCode)
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            isExpanded = false
                        },
                        enabled = colorPickerState.isValidHex && hasChanges,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set Color")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0m"
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    if (totalMinutes < 1) return "<1m"
    if (totalMinutes < 60) return "${totalMinutes}m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
}

@Preview(showBackground = true)
@Composable
private fun UsageDetailContentPreview() {
    val sampleAppDetails = AppDetails(
        packageName = "com.example.app",
        friendlyName = "Sample Application",
        icon = null,
        color = "#4CAF50",
        totalUsageMillis = (3600000L * 2) + (60000L * 33),
        usagePercentage = 0.35f,
        sessionCount = 12,
        averageSessionMillis = 1234567L,
        peakUsageTimeLabel = "Most used around 8 PM",
        historicalData = listOf(BarChartEntry(1f, "8a"), BarChartEntry(5f, "12p"), BarChartEntry(10f, "8p"))
    )
    MaterialTheme {
        Surface {
            UsageDetailContent(app = sampleAppDetails, onSetAppColor = { _, _ -> })
        }
    }
}