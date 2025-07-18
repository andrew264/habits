package com.andrew264.habits.ui.usage

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrew264.habits.ui.common.charts.StackedBarChart
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.components.FeatureDisabledContent
import com.andrew264.habits.ui.common.components.FilterButtonGroup
import com.andrew264.habits.ui.navigation.*
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.usage.components.AccessibilityWarningCard
import com.andrew264.habits.ui.usage.components.AppListItem
import com.andrew264.habits.ui.usage.components.StatisticsSummaryCard
import kotlinx.coroutines.launch

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
        onSetAppColor = viewModel::setAppColor
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
                                    onSetAppColor = onSetAppColor
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
    onOpenAccessibilitySettings: () -> Unit,
) {
    val isRefreshing = uiState.isLoading && uiState.stats != null
    val state = rememberPullToRefreshState()

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f)
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

            item {
                AnimatedVisibility(visible = uiState.isAppUsageTrackingEnabled && !uiState.isAccessibilityServiceEnabled) {
                    AccessibilityWarningCard(onOpenAccessibilitySettings)
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