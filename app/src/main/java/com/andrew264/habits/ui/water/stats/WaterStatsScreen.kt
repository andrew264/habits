package com.andrew264.habits.ui.water.stats

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.ui.common.charts.BarChart
import com.andrew264.habits.ui.common.charts.BarChartEntry
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WaterStatsScreen(
    viewModel: WaterStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val ranges = StatsTimeRange.entries
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                ranges.forEachIndexed { index, range ->
                    ElevatedToggleButton(
                        checked = uiState.selectedRange == range,
                        onCheckedChange = {
                            viewModel.setTimeRange(range)
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
        } else if (uiState.stats == null || uiState.stats?.totalDays == 0) {
            EmptyStatsState()
        } else {
            StatsContent(stats = uiState.stats!!)
        }
    }
}

@Composable
private fun StatsContent(stats: com.andrew264.habits.domain.analyzer.WaterStatistics) {
    // Summary Cards
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
        Column(Modifier.padding(16.dp)) {
            Text("Daily Intake", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
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
        Column(Modifier.padding(16.dp)) {
            Text("Peak Hours", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
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
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = MaterialTheme.typography.titleLarge.fontWeight
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
        Spacer(Modifier.height(16.dp))
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