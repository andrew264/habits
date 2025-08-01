package com.andrew264.habits.ui.usage

import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.andrew264.habits.ui.common.components.*
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.navigation.*
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.components.AccessibilityWarningCard
import com.andrew264.habits.ui.usage.components.AppListItem
import com.andrew264.habits.ui.usage.components.StatisticsSummaryCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun UsageStatsScreen(
    viewModel: UsageStatsViewModel = hiltViewModel(),
    onNavigate: (AppRoute) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInitialComposition = remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isInitialComposition.value) {
            isInitialComposition.value = false
        } else {
            viewModel.refresh()
        }
    }

    UsageStatsScreen(
        uiState = uiState,
        onSetTimeRange = viewModel::setTimeRange,
        onRefresh = viewModel::refresh,
        onNavigate = onNavigate,
        onSetAppColor = viewModel::setAppColor,
        onSetAppBlockingEnabled = viewModel::setAppBlockingEnabled,
        onSetUsageLimitNotificationsEnabled = viewModel::setUsageLimitNotificationsEnabled,
        onSaveLimits = { pkg, daily, session -> viewModel.saveAppLimits(pkg, daily, session) }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun UsageStatsScreen(
    uiState: UsageStatsUiState,
    onSetTimeRange: (UsageTimeRange) -> Unit,
    onRefresh: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit,
    onSetAppBlockingEnabled: (Boolean) -> Unit,
    onSetUsageLimitNotificationsEnabled: (Boolean) -> Unit,
    onSaveLimits: (packageName: String, dailyMinutes: Int?, sessionMinutes: Int?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<UsageSelection>()

    when {
        uiState.isLoading && uiState.stats == null -> {
            ContainedLoadingIndicator()
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
                            onSetTimeRange = onSetTimeRange,
                            onRefresh = onRefresh,
                            onAppSelected = { app ->
                                scope.launch {
                                    scaffoldNavigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        UsageSelection(app.packageName)
                                    )
                                }
                            },
                            onNavigateToWhitelist = { onNavigate(Whitelist) },
                            onSetAppBlockingEnabled = onSetAppBlockingEnabled,
                            onSetUsageLimitNotificationsEnabled = onSetUsageLimitNotificationsEnabled,
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
                                UsageDetailScreen(
                                    app = selectedApp,
                                    onSetAppColor = onSetAppColor,
                                    onSaveLimits = { daily, session -> onSaveLimits(selectedApp.packageName, daily, session) }
                                )
                            }
                        } else {
                            EmptyState(
                                icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                                title = "Select an App",
                                description = "Select an app from the list to see its detailed usage statistics."
                            )
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UsageListContent(
    uiState: UsageStatsUiState,
    onSetTimeRange: (UsageTimeRange) -> Unit,
    onRefresh: () -> Unit,
    onAppSelected: (AppDetails) -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onSetAppBlockingEnabled: (Boolean) -> Unit,
    onSetUsageLimitNotificationsEnabled: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val isRefreshing = uiState.isLoading && uiState.stats != null
    val state = rememberPullToRefreshState()

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f)
    }

    val view = LocalView.current
    LaunchedEffect(state) {
        var wasBeyondThreshold = state.distanceFraction >= 1.0f
        snapshotFlow { state.distanceFraction >= 1.0f }
            .distinctUntilChanged()
            .collect { isBeyondThreshold ->
                if (isBeyondThreshold) {
                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
                } else {
                    if (wasBeyondThreshold) {
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE)
                    }
                }
                wasBeyondThreshold = isBeyondThreshold
            }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = state,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
            )
    ) {
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.PaddingMedium,
                end = Dimens.PaddingMedium,
                top = Dimens.PaddingMedium,
                bottom = Dimens.PaddingMedium + navBarPadding
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterButtonGroup(
                        options = UsageTimeRange.entries,
                        selectedOption = uiState.selectedRange,
                        onOptionSelected = onSetTimeRange,
                        getLabel = { it.label }
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

            if (!uiState.isAccessibilityServiceEnabled) {
                item {
                    AnimatedVisibility(visible = uiState.isAppUsageTrackingEnabled && !uiState.isAccessibilityServiceEnabled) {
                        AccessibilityWarningCard(onOpenAccessibilitySettings)
                    }
                }
            }

            item {
                ToggleSettingsListItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Enable Limit Notifications",
                    summary = "Get notified when you exceed a usage limit.",
                    checked = uiState.usageLimitNotificationsEnabled,
                    onCheckedChange = onSetUsageLimitNotificationsEnabled,
                    position = ListItemPosition.TOP
                )
                ToggleSettingsListItem(
                    icon = Icons.Outlined.Block,
                    title = "Enable App Blocker",
                    summary = "Show an overlay when a usage limit is reached.",
                    checked = uiState.isAppBlockingEnabled,
                    onCheckedChange = onSetAppBlockingEnabled,
                    position = ListItemPosition.BOTTOM
                )
            }

            if (uiState.stats != null) {
                item {
                    StackedBarChart(
                        bins = uiState.stats.timeBins,
                        range = uiState.selectedRange,
                        whitelistedAppColors = uiState.whitelistedApps.associate { it.packageName to it.colorHex },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            item {
                val interactionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(interactionSource)
                FilledTonalButton(
                    onClick = onNavigateToWhitelist,
                    interactionSource = interactionSource,
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
                    val interactionSource = remember { MutableInteractionSource() }
                    HapticInteractionEffect(interactionSource)
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
                val interactionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(interactionSource)
                AppListItem(
                    appDetails = app,
                    modifier = Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = { onAppSelected(app) }
                    )
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                }
        ) {
            PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = isRefreshing)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageListContentPreview() {
    val sampleAppDetails = listOf(
        AppDetails(
            packageName = "com.google.android.youtube",
            friendlyName = "YouTube",
            color = "#F44336",
            dailyLimitMinutes = 120,
            sessionLimitMinutes = 30,
            totalUsageMillis = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(15),
            usagePercentage = 0.45f,
            averageSessionMillis = TimeUnit.MINUTES.toMillis(25),
            screenTimeHistoricalData = emptyList(),
            timesOpened = 5,
            timesOpenedHistoricalData = emptyList(),
            peakUsageTimeLabel = "Most used around 9 PM"
        ),
        AppDetails(
            packageName = "com.instagram.android",
            friendlyName = "Instagram",
            color = "#E91E63",
            dailyLimitMinutes = 60,
            sessionLimitMinutes = null,
            totalUsageMillis = TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(5),
            usagePercentage = 0.25f,
            averageSessionMillis = TimeUnit.MINUTES.toMillis(10),
            screenTimeHistoricalData = emptyList(),
            timesOpened = 12,
            timesOpenedHistoricalData = emptyList(),
            peakUsageTimeLabel = "Most used around 1 PM"
        )
    )

    val sampleStats = UsageStatistics(
        totalScreenOnTime = TimeUnit.HOURS.toMillis(5),
        pickupCount = 42,
        totalUsagePerApp = mapOf(
            "com.google.android.youtube" to TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(15),
            "com.instagram.android" to TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(5)
        ),
        timeBins = (0..23).map {
            UsageTimeBin(
                startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis((23 - it).toLong()),
                endTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis((22 - it).toLong()),
                totalScreenOnTime = (5..15).random() * 60_000L,
                appUsage = mapOf(
                    "com.google.android.youtube" to (1..5).random() * 60_000L,
                    "com.instagram.android" to (1..5).random() * 60_000L
                )
            )
        },
        timesOpenedPerBin = (0..23).map {
            mapOf(
                "com.google.android.youtube" to (0..2).random(),
                "com.instagram.android" to (0..3).random()
            )
        }
    )

    HabitsTheme {
        Surface {
            UsageListContent(
                uiState = UsageStatsUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = true,
                    isAccessibilityServiceEnabled = true,
                    usageLimitNotificationsEnabled = true,
                    selectedRange = UsageTimeRange.DAY,
                    stats = sampleStats,
                    whitelistedApps = emptyList(),
                    appDetails = sampleAppDetails,
                    averageSessionMillis = 123456L
                ),
                onSetTimeRange = {},
                onRefresh = {},
                onAppSelected = {},
                onNavigateToWhitelist = {},
                onSetUsageLimitNotificationsEnabled = {},
                onOpenAccessibilitySettings = {},
                onSetAppBlockingEnabled = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Usage List With Warning")
@Composable
private fun UsageListContentWithWarningPreview() {
    val sampleAppDetails = listOf(
        AppDetails(
            packageName = "com.google.android.youtube",
            friendlyName = "YouTube",
            color = "#F44336",
            dailyLimitMinutes = 120,
            sessionLimitMinutes = 30,
            totalUsageMillis = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(15),
            usagePercentage = 0.45f,
            averageSessionMillis = TimeUnit.MINUTES.toMillis(25),
            screenTimeHistoricalData = emptyList(),
            timesOpened = 5,
            timesOpenedHistoricalData = emptyList(),
            peakUsageTimeLabel = "Most used around 9 PM"
        )
    )

    HabitsTheme {
        Surface {
            UsageListContent(
                uiState = UsageStatsUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = true,
                    isAccessibilityServiceEnabled = false,
                    usageLimitNotificationsEnabled = true,
                    selectedRange = UsageTimeRange.DAY,
                    stats = UsageStatistics(
                        totalScreenOnTime = TimeUnit.HOURS.toMillis(5), pickupCount = 42, totalUsagePerApp = emptyMap(), timesOpenedPerBin = emptyList(),
                        timeBins = emptyList()
                    ),
                    whitelistedApps = emptyList(),
                    appDetails = sampleAppDetails,
                    averageSessionMillis = 123456L
                ),
                onSetTimeRange = {},
                onRefresh = {},
                onAppSelected = {},
                onNavigateToWhitelist = {},
                onSetUsageLimitNotificationsEnabled = {},
                onOpenAccessibilitySettings = {},
                onSetAppBlockingEnabled = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Usage Screen Disabled")
@Composable
private fun UsageStatsScreenDisabledPreview() {
    HabitsTheme {
        Surface {
            UsageStatsScreen(
                uiState = UsageStatsUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = false
                ),
                onSetTimeRange = {},
                onRefresh = {},
                onNavigate = {},
                onSetAppColor = { _, _ -> },
                onSetUsageLimitNotificationsEnabled = {},
                onSaveLimits = { _, _, _ -> },
                onSetAppBlockingEnabled = {}
            )
        }
    }
}