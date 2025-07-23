package com.andrew264.habits.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.components.AppCustomizationSection
import com.andrew264.habits.ui.usage.components.HeaderCard
import com.andrew264.habits.ui.usage.components.KeyMetricsCard
import com.andrew264.habits.ui.usage.components.UsageTrendCard

@Composable
fun UsageDetailScreen(
    app: AppDetails,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit,
    onSaveLimits: (dailyMinutes: Int?, sessionMinutes: Int?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        item {
            HeaderCard(app)
        }
        item {
            KeyMetricsCard(app)
        }
        if (app.historicalData.isNotEmpty()) {
            item {
                UsageTrendCard(app)
            }
        }
        item {
            AppCustomizationSection(
                app = app,
                onSetAppColor = onSetAppColor,
                onSaveLimits = onSaveLimits
            )
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
        dailyLimitMinutes = 90,
        sessionLimitMinutes = 20,
        totalUsageMillis = (3600000L * 2) + (60000L * 33),
        usagePercentage = 0.35f,
        sessionCount = 12,
        averageSessionMillis = 1234567L,
        peakUsageTimeLabel = "Most used around 8 PM",
        historicalData = emptyList()
    )
    HabitsTheme {
        Surface {
            UsageDetailScreen(app = sampleAppDetails, onSetAppColor = { _, _ -> }, onSaveLimits = { _, _ -> }
            )
        }
    }
}