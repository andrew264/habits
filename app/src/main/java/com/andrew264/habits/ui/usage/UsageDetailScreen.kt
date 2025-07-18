package com.andrew264.habits.ui.usage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.components.ColorConfigurationCard
import com.andrew264.habits.ui.usage.components.HeaderCard
import com.andrew264.habits.ui.usage.components.KeyMetricsCard
import com.andrew264.habits.ui.usage.components.UsageTrendCard

@Composable
fun UsageDetailScreen(
    app: AppDetails,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val isCompact = maxWidth < 600.dp
        if (isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                HeaderCard(app)
                KeyMetricsCard(app)
                if (app.historicalData.isNotEmpty()) {
                    UsageTrendCard(app)
                }
                ColorConfigurationCard(app, onSetAppColor)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(0.6f), // Give more space to the graph
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    HeaderCard(app)
                    if (app.historicalData.isNotEmpty()) {
                        UsageTrendCard(app)
                    }
                }
                Column(
                    modifier = Modifier.weight(0.4f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    KeyMetricsCard(app)
                    ColorConfigurationCard(app, onSetAppColor)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageDetailScreenPreview() {
    val sampleAppDetails = AppDetails(
        packageName = "com.example.app",
        friendlyName = "Sample Application",
        color = "#4CAF50",
        totalUsageMillis = (3600000L * 2) + (60000L * 33),
        usagePercentage = 0.35f,
        sessionCount = 12,
        averageSessionMillis = 1234567L,
        peakUsageTimeLabel = "Most used around 8 PM",
        historicalData = listOf(BarChartEntry(1f, "8a"), BarChartEntry(5f, "12p"), BarChartEntry(10f, "8p"))
    )
    HabitsTheme {
        Surface {
            UsageDetailScreen(app = sampleAppDetails, onSetAppColor = { _, _ -> })
        }
    }
}