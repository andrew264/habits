package com.andrew264.habits.ui.water.stats.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andrew264.habits.domain.analyzer.WaterStatistics
import com.andrew264.habits.ui.common.charts.BarChart
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.common.components.Statistic
import com.andrew264.habits.ui.common.components.StatisticCard
import com.andrew264.habits.ui.theme.Dimens
import java.time.format.DateTimeFormatter

@Composable
internal fun StatsContent(stats: WaterStatistics) {
    // Summary Cards
    val summaryStatistics = listOf(
        Statistic(label = "Daily Avg", value = "${stats.dailyAverage} ml"),
        Statistic(label = "Goal Met", value = "${stats.daysGoalMet} days")
    )
    StatisticCard(statistics = summaryStatistics)

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