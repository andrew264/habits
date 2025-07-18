package com.andrew264.habits.ui.usage.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andrew264.habits.ui.common.components.Statistic
import com.andrew264.habits.ui.common.components.StatisticCard
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.usage.AppDetails

@Composable
internal fun KeyMetricsCard(
    app: AppDetails,
    modifier: Modifier = Modifier
) {
    val statistics = listOf(
        Statistic(label = "Total Time", value = FormatUtils.formatDuration(app.totalUsageMillis)),
        Statistic(label = "Avg. Session", value = FormatUtils.formatDuration(app.averageSessionMillis)),
        Statistic(label = "Sessions", value = app.sessionCount.toString())
    )
    StatisticCard(statistics = statistics, modifier = modifier)
}