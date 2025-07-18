package com.andrew264.habits.ui.common.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.andrew264.habits.domain.model.UsageTimeBin
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.UsageTimeRange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.min

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StackedBarChart(
    bins: List<UsageTimeBin>,
    range: UsageTimeRange,
    whitelistedAppColors: Map<String, String>,
    modifier: Modifier = Modifier,
    otherColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    gridColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
) {
    if (bins.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val yAxisTextStyle = remember(typography, gridColor) {
        typography.labelSmall.copy(color = gridColor, textAlign = TextAlign.End)
    }
    val xAxisTextStyle = remember(typography, gridColor) {
        typography.labelSmall.copy(color = gridColor)
    }

    val animationProgress = remember(bins) {
        bins.map { Animatable(0f) }
    }
    val animationSpec: AnimationSpec<Float> = MaterialTheme.motionScheme.defaultSpatialSpec()

    LaunchedEffect(bins) {
        animationProgress.forEachIndexed { index, animatable ->
            launch {
                delay(index * 20L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = animationSpec
                )
            }
        }
    }

    val topValue = when (range) {
        UsageTimeRange.DAY -> TimeUnit.HOURS.toMillis(1)
        UsageTimeRange.WEEK -> TimeUnit.HOURS.toMillis(24)
    }

    val yAxisLabelCount = 4
    val yAxisLabelInfo = remember(topValue, range) {
        (0..yAxisLabelCount).map { i ->
            val value = (topValue / yAxisLabelCount) * i
            val positionFraction = 1f - (i.toFloat() / yAxisLabelCount)
            val label = when (range) {
                UsageTimeRange.DAY -> {
                    val minutes = value / 60_000
                    if (minutes > 0) "${minutes}m" else "0"
                }

                UsageTimeRange.WEEK -> {
                    val hours = value / 3600_000
                    if (hours > 0) "${hours}h" else "0"
                }
            }
            YAxisLabelInfo(label, positionFraction)
        }
    }

    val xAxisLabels = remember(bins, range) {
        val formatter = when (range) {
            UsageTimeRange.DAY -> DateTimeFormatter.ofPattern("ha")
            UsageTimeRange.WEEK -> DateTimeFormatter.ofPattern("E")
        }
        bins.map { bin ->
            val dateTime = Instant.ofEpochMilli(bin.startTime).atZone(ZoneId.systemDefault())
            formatter.format(dateTime).let {
                if (range == UsageTimeRange.DAY) it.lowercase().removeSuffix("m") else it
            }
        }
    }
    val xAxisLabelInterval = when (range) {
        UsageTimeRange.DAY -> 6
        UsageTimeRange.WEEK -> 1
    }

    Canvas(modifier = modifier) {
        val yAxisWidth = 50.dp.toPx()
        val xAxisHeight = 30.dp.toPx()
        val chartAreaWidth = size.width - yAxisWidth
        val chartAreaHeight = size.height - xAxisHeight

        if (chartAreaWidth <= 0 || chartAreaHeight <= 0) return@Canvas

        val barAreaWidth = chartAreaWidth / bins.size
        val barWidth = barAreaWidth * 0.75f
        val barSpacing = barAreaWidth * 0.25f

        drawYAxis(yAxisLabelInfo, yAxisWidth, chartAreaHeight, textMeasurer, yAxisTextStyle, gridColor)
        drawXAxis(xAxisLabels, yAxisWidth, chartAreaHeight, barAreaWidth, textMeasurer, xAxisTextStyle, gridColor, labelInterval = xAxisLabelInterval, drawAxisLine = false)


        bins.forEachIndexed { index, bin ->
            if (bin.totalScreenOnTime <= 0) return@forEachIndexed

            val totalBarHeightCapped = (min(bin.totalScreenOnTime, topValue).toFloat() / topValue) * chartAreaHeight * animationProgress[index].value
            if (totalBarHeightCapped <= 0) return@forEachIndexed

            val barLeft = yAxisWidth + barSpacing / 2 + index * barAreaWidth

            // Define the path for the entire rounded bar, clipped at the top if needed
            val barRect = Rect(
                left = barLeft,
                top = chartAreaHeight - totalBarHeightCapped,
                right = barLeft + barWidth,
                bottom = chartAreaHeight
            )
            val path = Path().apply {
                addRoundRect(RoundRect(barRect, cornerRadius = CornerRadius(barWidth * 0.15f)))
            }

            // Clip all drawing within this path
            clipPath(path) {
                var currentY = chartAreaHeight
                var remainingScreenTime = bin.totalScreenOnTime

                val sortedApps = bin.appUsage.entries
                    .filter { whitelistedAppColors.containsKey(it.key) }
                    .sortedBy { whitelistedAppColors[it.key] } // Sort for consistent color order

                // Draw colored segments from bottom up
                sortedApps.forEach { (packageName, duration) ->
                    whitelistedAppColors[packageName]?.let { colorHex ->
                        val segmentHeight = (duration.toFloat() / topValue) * chartAreaHeight
                        drawRect(
                            color = Color(colorHex.toColorInt()),
                            topLeft = Offset(barLeft, currentY - segmentHeight),
                            size = Size(barWidth, segmentHeight)
                        )
                        currentY -= segmentHeight
                        remainingScreenTime -= duration
                    }
                }

                // Draw "other" usage on top
                if (remainingScreenTime > 0) {
                    val segmentHeight = (remainingScreenTime.toFloat() / topValue) * chartAreaHeight
                    drawRect(
                        color = otherColor,
                        topLeft = Offset(barLeft, currentY - segmentHeight),
                        size = Size(barWidth, segmentHeight)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 250)
@Composable
private fun StackedBarChartDayPreview() {
    val bins = (0..23).map {
        UsageTimeBin(
            startTime = 0,
            endTime = 0,
            totalScreenOnTime = (5..60).random() * 60_000L,
            appUsage = mapOf(
                "com.a" to (1..10).random() * 60_000L,
                "com.b" to (1..10).random() * 60_000L,
                "com.c" to (1..10).random() * 60_000L,
            )
        )
    }
    val colors = mapOf("com.a" to "#4CAF50", "com.b" to "#2196F3", "com.c" to "#FFC107")
    HabitsTheme {
        Box(Modifier.padding(16.dp)) {
            StackedBarChart(
                bins = bins,
                range = UsageTimeRange.DAY,
                whitelistedAppColors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 250)
@Composable
private fun StackedBarChartWeekPreview() {
    val bins = (0..6).map {
        UsageTimeBin(
            startTime = 0,
            endTime = 0,
            totalScreenOnTime = (1..12).random() * 3600_000L,
            appUsage = mapOf(
                "com.a" to (1..2).random() * 3600_000L,
                "com.b" to (1..2).random() * 3600_000L,
            )
        )
    }
    val colors = mapOf("com.a" to "#E91E63", "com.b" to "#00BCD4")
    HabitsTheme {
        Box(Modifier.padding(16.dp)) {
            StackedBarChart(
                bins = bins,
                range = UsageTimeRange.WEEK,
                whitelistedAppColors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}