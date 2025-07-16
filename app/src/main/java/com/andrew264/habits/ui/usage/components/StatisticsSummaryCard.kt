package com.andrew264.habits.ui.usage.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import java.util.concurrent.TimeUnit

@Composable
fun StatisticsSummaryCard(
    totalScreenOnTime: Long,
    pickupCount: Int,
    averageSessionMillis: Long,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 360.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.PaddingLarge, horizontal = Dimens.PaddingExtraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    StatItem(label = "Screen Time", value = formatDuration(totalScreenOnTime))
                    StatItem(label = "Unlocks", value = pickupCount.toString())
                    StatItem(label = "Avg. Session", value = formatDuration(averageSessionMillis))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.PaddingLarge, horizontal = Dimens.PaddingExtraLarge),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(label = "Screen Time", value = formatDuration(totalScreenOnTime))
                    StatItem(label = "Unlocks", value = pickupCount.toString())
                    StatItem(label = "Avg. Session", value = formatDuration(averageSessionMillis))
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0m"

    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
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

@Preview(widthDp = 320)
@Composable
private fun StatisticsSummaryCardNarrowPreview() {
    HabitsTheme {
        StatisticsSummaryCard(
            totalScreenOnTime = TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(32),
            pickupCount = 58,
            averageSessionMillis = (TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(32)) / 58
        )
    }
}