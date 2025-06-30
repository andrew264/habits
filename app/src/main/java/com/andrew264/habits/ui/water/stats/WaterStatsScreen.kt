package com.andrew264.habits.ui.water.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.domain.analyzer.DailyWaterIntake
import com.andrew264.habits.domain.analyzer.HourlyWaterIntake
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WaterStatsScreen(
    viewModel: WaterStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                        onCheckedChange = { viewModel.setTimeRange(range) },
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
            label = "Success Rate",
            value = "${(stats.successRate * 100).roundToInt()}%",
            modifier = Modifier.weight(1f)
        )
    }

    // Daily Intake Chart
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Daily Intake", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            SimpleBarChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                data = stats.dailyIntakes,
                labelFormatter = { it.date.format(DateTimeFormatter.ofPattern("MMM d")) }
            )
        }
    }

    // Hourly Breakdown Chart
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Peak Hours", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            SimpleBarChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                data = stats.hourlyBreakdown.filter { it.totalMl > 0 }, // Only show hours with intake
                labelFormatter = {
                    val hour = it.hour
                    when {
                        hour == 0 -> "12a"
                        hour == 12 -> "12p"
                        hour < 12 -> "${hour}a"
                        else -> "${hour - 12}p"
                    }
                }
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
fun <T> SimpleBarChart(
    modifier: Modifier = Modifier,
    data: List<T>,
    labelFormatter: (T) -> String,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val values = data.map {
        when (it) {
            is DailyWaterIntake -> it.totalMl
            is HourlyWaterIntake -> it.totalMl
            else -> 0
        }
    }
    val maxValue = values.maxOrNull()?.toFloat() ?: 0f

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val barWidth = (size.width / data.size) * 0.6f
        val barSpacing = (size.width / data.size) * 0.4f
        var currentX = barSpacing / 2

        data.forEachIndexed { index, item ->
            val barHeight = if (maxValue > 0) (values[index] / maxValue) * size.height else 0f
            drawRect(
                color = barColor,
                topLeft = Offset(x = currentX, y = size.height - barHeight),
                size = Size(width = barWidth, height = barHeight)
            )
            // Simple label drawing could be added here if needed
            currentX += barWidth + barSpacing
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