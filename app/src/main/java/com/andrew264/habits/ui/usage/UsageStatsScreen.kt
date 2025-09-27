package com.andrew264.habits.ui.usage

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.domain.model.UsageStatistics
import com.andrew264.habits.domain.model.UsageTimeBin
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.components.FeatureDisabledContent
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.Settings
import com.andrew264.habits.ui.navigation.sharedAxisXEnter
import com.andrew264.habits.ui.navigation.sharedAxisXExit
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.components.UsageListContent
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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
        onSaveLimits = { pkg, session -> viewModel.saveAppLimits(pkg, session) }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UsageStatsScreen(
    uiState: UsageStatsUiState,
    onSetTimeRange: (UsageTimeRange) -> Unit,
    onRefresh: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit,
    onSaveLimits: (packageName: String, sessionMinutes: Int?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<UsageSelection>(
        adaptStrategies =
            ListDetailPaneScaffoldDefaults.adaptStrategies(
                extraPaneAdaptStrategy =
                    AdaptStrategy.Reflow(reflowUnder = ListDetailPaneScaffoldRole.Detail)
            )
    )
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val slideDistance = remember(density) {
        with(density) { 30.dp.toPx() }.roundToInt()
    }


    when {
        uiState.isLoading && uiState.stats == null -> {
            ContainedLoadingIndicator()
        }

        !uiState.isAppUsageTrackingEnabled -> {
            FeatureDisabledContent(
                title = "Usage Tracking Disabled",
                description = "This feature uses the Accessibility Service to show you how you spend time on your phone. You can enable it in the Monitoring settings.",
                buttonText = "Go to Settings",
                onEnableClicked = { onNavigate(Settings) }
            )
        }

        else -> {
            NavigableListDetailPaneScaffold(
                navigator = scaffoldNavigator,
                listPane = {
                    AnimatedPane(
                        enterTransition = sharedAxisXEnter(forward = false, slideDistance = slideDistance),
                        exitTransition = sharedAxisXExit(forward = true, slideDistance = slideDistance)
                    ) {
                        UsageListContent(
                            uiState = uiState,
                            listState = listState,
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
                            onNavigate = onNavigate,
                            isDetailPaneVisible = scaffoldNavigator.currentDestination?.contentKey != null
                        )
                    }
                },
                detailPane = {
                    AnimatedPane(
                        enterTransition = sharedAxisXEnter(forward = true, slideDistance = slideDistance),
                        exitTransition = sharedAxisXExit(forward = false, slideDistance = slideDistance)
                    ) {
                        val selection = scaffoldNavigator.currentDestination?.contentKey
                        if (selection?.packageName != null) {
                            val selectedApp =
                                uiState.appDetails.find { it.packageName == selection.packageName }
                            if (selectedApp != null) {
                                UsageDetailScreen(
                                    app = selectedApp,
                                    onSetAppColor = onSetAppColor,
                                    onSaveLimits = { session ->
                                        onSaveLimits(
                                            selectedApp.packageName,
                                            session
                                        )
                                    },
                                    onNavigateUp = {
                                        scope.launch {
                                            scaffoldNavigator.navigateBack()
                                        }
                                    }
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

@Preview(showBackground = true)
@Composable
private fun UsageStatsScreenPreview() {
    val sampleAppDetails = listOf(
        AppDetails(
            packageName = "com.google.android.youtube",
            friendlyName = "YouTube",
            color = "#F44336",
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
            UsageStatsScreen(
                uiState = UsageStatsUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = true,
                    sharedDailyUsageLimitMinutes = 345,
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
                onNavigate = {},
                onSetAppColor = { _, _ -> },
                onSaveLimits = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "Usage Screen With Warning")
@Composable
private fun UsageStatsScreenWithWarningPreview() {
    val sampleAppDetails = listOf(
        AppDetails(
            packageName = "com.google.android.youtube",
            friendlyName = "YouTube",
            color = "#F44336",
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
            UsageStatsScreen(
                uiState = UsageStatsUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = true,
                    isAccessibilityServiceEnabled = false,
                    usageLimitNotificationsEnabled = true,
                    selectedRange = UsageTimeRange.DAY,
                    stats = UsageStatistics(
                        totalScreenOnTime = TimeUnit.HOURS.toMillis(5),
                        pickupCount = 42,
                        totalUsagePerApp = emptyMap(),
                        timesOpenedPerBin = emptyList(),
                        timeBins = emptyList()
                    ),
                    whitelistedApps = emptyList(),
                    appDetails = sampleAppDetails,
                    averageSessionMillis = 123456L
                ),
                onSetTimeRange = {},
                onRefresh = {},
                onNavigate = {},
                onSetAppColor = { _, _ -> },
                onSaveLimits = { _, _ -> }
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
                onSaveLimits = { _, _ -> }
            )
        }
    }
}