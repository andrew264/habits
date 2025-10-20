package com.andrew264.habits.ui.water.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
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
        Statistic(label = stringResource(R.string.water_stats_content_daily_avg), value = stringResource(id = R.string.water_input_section_ml, stats.dailyAverage)),
        Statistic(label = stringResource(R.string.water_stats_content_goal_met), value = stringResource(id = R.string.water_stats_content_days, stats.daysGoalMet))
    )
    StatisticCard(statistics = summaryStatistics)

    // Daily Intake Chart
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.PaddingLarge)) {
            Text(stringResource(R.string.water_stats_content_daily_intake), style = MaterialTheme.typography.titleMedium)
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
            Text(stringResource(R.string.water_stats_content_peak_hours), style = MaterialTheme.typography.titleMedium)
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