package com.andrew264.habits.ui.usage.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.charts.BarChart
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.usage.AppDetails
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun UsageTrendCard(app: AppDetails) {
    val maxUsageMillis = app.historicalData.maxOfOrNull { it.value } ?: 0f
    val maxUsageMinutes = ceil(maxUsageMillis / 60000f).toInt()
    val topValueMinutes = when {
        maxUsageMinutes <= 1 -> 1
        maxUsageMinutes <= 2 -> 2
        maxUsageMinutes <= 5 -> 5
        maxUsageMinutes <= 10 -> 10
        maxUsageMinutes <= 15 -> 15
        maxUsageMinutes <= 30 -> 30
        maxUsageMinutes <= 45 -> 45
        maxUsageMinutes <= 60 -> 60
        else -> (ceil(maxUsageMinutes / 60f).toInt() * 60)
    }
    val topValueMillis = topValueMinutes * 60000f

    val yAxisLabelFormatter: (Float) -> String = { valueInMillis ->
        val minutes = (valueInMillis / 60000f).roundToInt()
        "${minutes}m"
    }

    Card {
        Column(
            modifier = Modifier.padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Usage Trend",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = app.peakUsageTimeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.PaddingLarge)
            )
            BarChart(
                entries = app.historicalData,
                barColor = app.color.toColorOrNull() ?: MaterialTheme.colorScheme.primary,
                topValue = topValueMillis,
                yAxisLabelFormatter = yAxisLabelFormatter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}