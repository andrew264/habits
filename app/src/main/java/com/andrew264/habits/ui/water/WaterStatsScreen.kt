package com.andrew264.habits.ui.water

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.domain.analyzer.DailyWaterIntake
import com.andrew264.habits.domain.analyzer.HourlyWaterIntake
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.components.FilterButtonGroup
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.water.components.StatsContent
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterStatsScreen(
    viewModel: WaterStatsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            SimpleTopAppBar(title = stringResource(R.string.water_stats_hydration_statistics), onNavigateUp = onNavigateUp, scrollBehavior = scrollBehavior)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        WaterStatsScreen(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            uiState = uiState,
            onSetTimeRange = viewModel::setTimeRange
        )
    }
}

@Composable
private fun WaterStatsScreen(
    modifier: Modifier = Modifier,
    uiState: WaterStatsUiState,
    onSetTimeRange: (StatsTimeRange) -> Unit,
) {
    Column(
        modifier = modifier
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
                label = { Text(it.label) }
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
                title = stringResource(R.string.water_stats_not_enough_data),
                description = stringResource(R.string.water_stats_not_enough_data_description)
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
        WaterStatsScreen(
            uiState = WaterStatsUiState(isLoading = false, stats = fakeStats),
            onSetTimeRange = {}
        )
    }
}

@Preview(name = "Water Stats - Empty", showBackground = true)
@Composable
private fun WaterStatsScreenEmptyPreview() {
    HabitsTheme {
        WaterStatsScreen(
            uiState = WaterStatsUiState(isLoading = false, stats = null),
            onSetTimeRange = {}
        )
    }
}

@Preview(name = "Water Stats - Loading", showBackground = true)
@Composable
private fun WaterStatsScreenLoadingPreview() {
    HabitsTheme {
        WaterStatsScreen(
            uiState = WaterStatsUiState(isLoading = true, stats = null),
            onSetTimeRange = {}
        )
    }
}