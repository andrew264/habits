package com.andrew264.habits.ui.usage.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.common.utils.rememberAppIcon
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.AppDetails

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppListItem(
    appDetails: AppDetails,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        val icon = rememberAppIcon(packageName = appDetails.packageName)
        DrawableImage(
            drawable = icon,
            contentDescription = "${appDetails.friendlyName} icon",
            modifier = Modifier.size(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appDetails.friendlyName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = FormatUtils.formatDuration(appDetails.totalUsageMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Dimens.PaddingExtraSmall))
            LinearProgressIndicator(
                progress = { appDetails.usagePercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(appDetails.color.toColorInt()),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppListItemPreview() {
    val sampleAppDetails = AppDetails(
        packageName = "com.example.app",
        friendlyName = "Sample Application",
        color = "#4CAF50",
        dailyLimitMinutes = 90,
        sessionLimitMinutes = 20,
        totalUsageMillis = (3600000L * 2) + (60000L * 33),
        usagePercentage = 0.35f,
        averageSessionMillis = 1234567L,
        screenTimeHistoricalData = listOf(
            BarChartEntry(1f, "8a"),
            BarChartEntry(5f, "12p"),
            BarChartEntry(10f, "8p")
        ),
        timesOpened = 23,
        timesOpenedHistoricalData = emptyList(),
        peakUsageTimeLabel = "Most used around 8 PM"
    )
    HabitsTheme {
        AppListItem(
            appDetails = sampleAppDetails,
            modifier = Modifier.padding(Dimens.PaddingLarge)
        )
    }
}