package com.andrew264.habits.ui.usage.components

import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.charts.StackedBarChart
import com.andrew264.habits.ui.common.components.FilterButtonGroup
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.common.list_items.ListSectionHeader
import com.andrew264.habits.ui.common.list_items.containedItems
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.UsageSettings
import com.andrew264.habits.ui.navigation.Whitelist
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.usage.AppDetails
import com.andrew264.habits.ui.usage.UsageStatsUiState
import com.andrew264.habits.ui.usage.UsageTimeRange
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun UsageListContent(
    uiState: UsageStatsUiState,
    listState: LazyListState,
    onSetTimeRange: (UsageTimeRange) -> Unit,
    onRefresh: () -> Unit,
    onAppSelected: (AppDetails) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    isDetailPaneVisible: Boolean
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val expandedFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isDetailPaneVisible) {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.usage_list_content_title)) },
                    actions = {
                        val interactionSource = remember { MutableInteractionSource() }
                        HapticInteractionEffect(interactionSource)
                        IconButton(
                            onClick = { onNavigate(UsageSettings) },
                            interactionSource = interactionSource,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.usage_list_content_settings))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isDetailPaneVisible) {
                val fabInteractionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(fabInteractionSource)
                SmallExtendedFloatingActionButton(
                    text = { Text(text = stringResource(R.string.usage_list_content_manage_apps)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, stringResource(R.string.usage_list_content_manage_whitelisted_apps)) },
                    onClick = { onNavigate(Whitelist) },
                    expanded = expandedFab,
                    interactionSource = fabInteractionSource
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
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
                .padding(paddingValues)
                .fillMaxSize()
                .pullToRefresh(
                    state = state,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.PaddingLarge,
                    end = Dimens.PaddingLarge,
                ),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.PaddingMedium),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilterButtonGroup(
                            options = UsageTimeRange.entries,
                            selectedOption = uiState.selectedRange,
                            onOptionSelected = onSetTimeRange,
                            label = { Text(it.label) }
                        )
                    }
                }

                uiState.stats?.let { stats ->
                    item {
                        StatisticsSummaryCard(
                            totalScreenOnTime = stats.totalScreenOnTime,
                            pickupCount = stats.pickupCount,
                            averageSessionMillis = uiState.averageSessionMillis,
                            modifier = Modifier.padding(vertical = Dimens.PaddingMedium)
                        )
                    }
                }

                if (!uiState.isAccessibilityServiceEnabled) {
                    item {
                        Row(modifier = Modifier.padding(vertical = Dimens.PaddingMedium)) {
                            AccessibilityWarningCard(
                                onOpenAccessibilitySettings = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                            )
                        }
                    }
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
                                .padding(vertical = Dimens.PaddingMedium)
                        )
                    }
                }

                stickyHeader {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                        ListSectionHeader(stringResource(R.string.usage_list_app_breakdown))
                    }
                }

                if (uiState.appDetails.isEmpty() && uiState.stats?.totalUsagePerApp?.isNotEmpty() == true) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.PaddingMedium)
                        ) {
                            Text(
                                stringResource(R.string.usage_list_no_whitelisted_apps),
                                modifier = Modifier.padding(Dimens.PaddingLarge),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                containedItems(
                    items = uiState.appDetails,
                    key = { it.packageName }
                ) { app ->
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

                item {
                    Spacer(Modifier.height(80.dp))
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
}