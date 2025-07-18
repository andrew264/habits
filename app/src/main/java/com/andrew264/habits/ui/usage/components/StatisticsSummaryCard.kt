package com.andrew264.habits.ui.usage.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.common.components.Statistic
import com.andrew264.habits.ui.common.components.StatisticCard
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.HabitsTheme
import java.util.concurrent.TimeUnit

@Composable
fun StatisticsSummaryCard(
    totalScreenOnTime: Long,
    pickupCount: Int,
    averageSessionMillis: Long,
    modifier: Modifier = Modifier
) {
    val statistics = listOf(
        Statistic(label = "Screen Time", value = FormatUtils.formatDuration(totalScreenOnTime)),
        Statistic(label = "Unlocks", value = pickupCount.toString()),
        Statistic(label = "Avg. Session", value = FormatUtils.formatDuration(averageSessionMillis))
    )

    StatisticCard(statistics = statistics, modifier = modifier)
}

@Preview
@Composable
private fun StatisticsSummaryCardPreview() {
    HabitsTheme {
        StatisticsSummaryCard(
            totalScreenOnTime = TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(32),
            pickupCount = 58,
            averageSessionMillis = (TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(32)) / 58
        )
    }
}