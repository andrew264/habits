package com.andrew264.habits.ui.bedtime.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.ui.bedtime.BedtimeChartRange
import com.andrew264.habits.ui.bedtime.BedtimeUiState
import com.andrew264.habits.ui.common.charts.SleepChart
import com.andrew264.habits.ui.common.charts.TimelineChart
import com.andrew264.habits.ui.common.charts.TimelineLabelStrategy
import com.andrew264.habits.ui.common.components.FilterButtonGroup
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SleepHistoryCard(
    uiState: BedtimeUiState,
    onSetTimelineRange: (BedtimeChartRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Sleep History",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterButtonGroup(
                    options = BedtimeChartRange.entries,
                    selectedOption = uiState.selectedTimelineRange,
                    onOptionSelected = onSetTimelineRange,
                    getLabel = { it.label }
                )
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
            if (uiState.timelineSegments.isEmpty()) {
                Text(
                    text = "No presence data available for this time range.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    if (uiState.selectedTimelineRange.isLinear) {
                        val timelineLabelStrategy = remember(uiState.selectedTimelineRange) {
                            when (uiState.selectedTimelineRange) {
                                BedtimeChartRange.TWELVE_HOURS -> TimelineLabelStrategy.TWELVE_HOURS
                                else -> TimelineLabelStrategy.DAY
                            }
                        }
                        TimelineChart(
                            segments = uiState.timelineSegments,
                            getStartTimeMillis = { it.startTimeMillis },
                            getEndTimeMillis = { it.endTimeMillis },
                            getColor = { it.state.toColor() },
                            viewStartTimeMillis = uiState.viewStartTimeMillis,
                            viewEndTimeMillis = uiState.viewEndTimeMillis,
                            labelStrategy = timelineLabelStrategy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp),
                            barHeight = 64.dp
                        )
                        PresenceLegend()
                    } else {
                        val sleepSegments = remember(uiState.timelineSegments) {
                            uiState.timelineSegments.filter { it.state == UserPresenceState.SLEEPING }
                        }
                        val rangeInDays =
                            if (uiState.selectedTimelineRange == BedtimeChartRange.WEEK) 7 else 30
                        SleepChart(
                            segments = sleepSegments,
                            getStartTimeMillis = { it.startTimeMillis },
                            getEndTimeMillis = { it.endTimeMillis },
                            getState = { it.state },
                            getColorForState = { it.toColor() },
                            rangeInDays = rangeInDays,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                }
            }
        }
    }
}