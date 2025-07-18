package com.andrew264.habits.ui.water.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.domain.analyzer.DailyWaterIntake
import com.andrew264.habits.domain.analyzer.HourlyWaterIntake
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.components.FilterButtonGroup
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.water.stats.components.StatsContent
import java.time.LocalDate

@Composable
fun WaterStatsScreen(
    viewModel: WaterStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    WaterStatsScreenContent(
        uiState = uiState,
        onSetTimeRange = viewModel::setTimeRange
    )
}

@Composable
private fun WaterStatsScreenContent(
    uiState: WaterStatsUiState,
    onSetTimeRange: (StatsTimeRange) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterButtonGroup(
                options = StatsTimeRange.entries,
                selectedOption = uiState.selectedRange,
                onOptionSelected = onSetTimeRange,
                getLabel = { it.label }
            )
        }

        if (uiState.isLoading) {
            ContainedLoadingIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        } else if (uiState.stats == null || uiState.stats.totalDays == 0) {
            EmptyState(
                icon = Icons.Outlined.BarChart,
                title = "Not Enough Data",
                description = "Log some water intake for a few days to see your statistics here."
            )
        } else {
            StatsContent(stats = uiState.stats)
        }
    }
}

@Preview(name = "Water Stats - With Data", showBackground = true)
@Composable
private fun WaterStatsScreenWithDataPreview() {
    val today = LocalDate.now()
    val fakeStats = com.andrew264.habits.domain.analyzer.WaterStatistics(
        dailyIntakes = (0..6).map {
            DailyWaterIntake(today.minusDays(it.toLong()), (1500..2800).random())
        }.reversed(),
        hourlyBreakdown = (8..22).map {
            HourlyWaterIntake(it, (100..400).random())
        },
        dailyAverage = 2150,
        totalDays = 7,
        daysGoalMet = 4
    )
    HabitsTheme {
        WaterStatsScreenContent(
            uiState = WaterStatsUiState(isLoading = false, stats = fakeStats),
            onSetTimeRange = {}
        )
    }
}

@Preview(name = "Water Stats - Empty", showBackground = true)
@Composable
private fun WaterStatsScreenEmptyPreview() {
    HabitsTheme {
        WaterStatsScreenContent(
            uiState = WaterStatsUiState(isLoading = false, stats = null),
            onSetTimeRange = {}
        )
    }
}

@Preview(name = "Water Stats - Loading", showBackground = true)
@Composable
private fun WaterStatsScreenLoadingPreview() {
    HabitsTheme {
        WaterStatsScreenContent(
            uiState = WaterStatsUiState(isLoading = true, stats = null),
            onSetTimeRange = {}
        )
    }
}