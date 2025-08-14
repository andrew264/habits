package com.andrew264.habits.ui.bedtime.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.andrew264.habits.ui.bedtime.BedtimeChartRange
import com.andrew264.habits.ui.bedtime.BedtimeUiState
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BedtimeContent(
    modifier: Modifier = Modifier,
    uiState: BedtimeUiState,
    onSetTimelineRange: (BedtimeChartRange) -> Unit,
    onRefresh: () -> Unit,
    paddingValues: PaddingValues
) {
    val isRefreshing = uiState.isLoading
    val pullToRefreshState = rememberPullToRefreshState()
    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
    }

    val view = LocalView.current
    LaunchedEffect(pullToRefreshState) {
        var wasBeyondThreshold = pullToRefreshState.distanceFraction >= 1.0f
        snapshotFlow { pullToRefreshState.distanceFraction >= 1.0f }
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
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            SleepHistoryCard(
                uiState = uiState,
                onSetTimelineRange = onSetTimelineRange,
            )
        }

        PullToRefreshDefaults.LoadingIndicator(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                }
        )
    }
}