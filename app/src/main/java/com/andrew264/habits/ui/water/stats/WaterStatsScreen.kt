package com.andrew264.habits.ui.water.stats

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.domain.analyzer.DailyWaterIntake
import com.andrew264.habits.domain.analyzer.HourlyWaterIntake
import com.andrew264.habits.ui.common.charts.BarChart
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WaterStatsScreenContent(
    uiState: WaterStatsUiState,
    onSetTimeRange: (StatsTimeRange) -> Unit,
) {
    val view = LocalView.current

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
            val ranges = StatsTimeRange.entries
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

        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.stats == null || uiState.stats.totalDays == 0) {
            EmptyStatsState()
        } else {
            StatsContent(stats = uiState.stats)
        }
    }
}

@Composable
private fun StatsContent(stats: com.andrew264.habits.domain.analyzer.WaterStatistics) {
    // Summary Cards
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        StatCard(
            label = "Daily Avg",
            value = "${stats.dailyAverage} ml",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Goal Met",
            value = "${stats.daysGoalMet} days",
            modifier = Modifier.weight(1f)
        )
    }

    // Daily Intake Chart
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.PaddingLarge)) {
            Text("Daily Intake", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Dimens.PaddingLarge))
            val dailyEntries = remember(stats.dailyIntakes) {
                stats.dailyIntakes.map {
                    BarChartEntry(
                        value = it.totalMl.toFloat(),
                        label = it.date.format(DateTimeFormatter.ofPattern("d MMM"))
                    )
                }
            }
            BarChart(
                entries = dailyEntries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }

    // Hourly Breakdown Chart
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.PaddingLarge)) {
            Text("Peak Hours", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Dimens.PaddingLarge))
            val hourlyEntries = remember(stats.hourlyBreakdown) {
                stats.hourlyBreakdown.filter { it.totalMl > 0 }.map {
                    BarChartEntry(
                        value = it.totalMl.toFloat(),
                        label = when {
                            it.hour == 0 -> "12a"
                            it.hour == 12 -> "12p"
                            it.hour < 12 -> "${it.hour}a"
                            else -> "${it.hour - 12}p"
                        }
                    )
                }
            }
            BarChart(
                entries = hourlyEntries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun EmptyStatsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.BarChart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Text(
            text = "Not Enough Data",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Log some water intake for a few days to see your statistics here.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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