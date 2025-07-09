package com.andrew264.habits.ui.usage.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.andrew264.habits.domain.model.AppSegment
import com.andrew264.habits.domain.model.ScreenOnPeriod
import com.andrew264.habits.domain.model.UsageTimelineModel
import com.andrew264.habits.ui.common.charts.TimelineLabelStrategy
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

@Composable
fun AppUsageTimelineChart(
    model: UsageTimelineModel,
    modifier: Modifier = Modifier,
    labelStrategy: TimelineLabelStrategy = TimelineLabelStrategy.DAY,
    barHeight: Dp = Dimens.PaddingExtraLarge,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelTextStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val screenOffColor = MaterialTheme.colorScheme.surfaceContainer
    val screenOnColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val timeFormatter = remember(labelStrategy.formatterPattern) {
        SimpleDateFormat(labelStrategy.formatterPattern, Locale.getDefault())
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val mainBarHeightPx = barHeight.toPx()
        val totalDurationMillis = model.viewEnd - model.viewStart

        if (totalDurationMillis <= 0) return@Canvas

        // 1. Draw the base "screen off" background
        drawRect(
            color = screenOffColor,
            topLeft = Offset(0f, 0f),
            size = Size(canvasWidth, mainBarHeightPx)
        )

        // 2. Draw screen-on periods
        model.screenOnPeriods.forEach { period ->
            val startOffset = max(0L, period.startTimestamp - model.viewStart)
            val endOffset = period.endTimestamp - model.viewStart
            val periodWidth = ((endOffset - startOffset).toFloat() / totalDurationMillis) * canvasWidth
            val currentX = (startOffset.toFloat() / totalDurationMillis) * canvasWidth

            if (periodWidth > 0f) {
                drawRect(
                    color = screenOnColor,
                    topLeft = Offset(currentX, 0f),
                    size = Size(periodWidth, mainBarHeightPx)
                )
            }
        }

        // 3. Draw whitelisted app usage segments on top
        model.screenOnPeriods.forEach { period ->
            period.appSegments.forEach { segment ->
                segment.color?.let { colorHex ->
                    val startOffset = max(0L, segment.startTimestamp - model.viewStart)
                    val endOffset = segment.endTimestamp - model.viewStart
                    val segmentWidth = ((endOffset - startOffset).toFloat() / totalDurationMillis) * canvasWidth
                    val currentX = (startOffset.toFloat() / totalDurationMillis) * canvasWidth

                    if (segmentWidth > 0f) {
                        drawRect(
                            color = Color(colorHex.toColorInt()),
                            topLeft = Offset(currentX, 0f),
                            size = Size(segmentWidth, mainBarHeightPx)
                        )
                    }
                }
            }
        }

        // 4. Draw time labels below the chart
        val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(model.viewStart), ZoneId.systemDefault())
        val roundedStartDateTime = startDateTime.truncatedTo(labelStrategy.chronoUnit)
        val roundedStartTimeMillis = roundedStartDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        var labelTimeMillis = roundedStartTimeMillis
        while (labelTimeMillis <= model.viewEnd) {
            if (labelTimeMillis >= model.viewStart) {
                val labelText = timeFormatter.format(Date(labelTimeMillis)).lowercase()
                val textLayoutResult = textMeasurer.measure(text = labelText, style = labelTextStyle)

                val labelX = (labelTimeMillis - model.viewStart).toFloat() / totalDurationMillis * canvasWidth
                val textX = (labelX - textLayoutResult.size.width / 2f).coerceIn(0f, canvasWidth - textLayoutResult.size.width)

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(textX, mainBarHeightPx + 4.dp.toPx())
                )
            }
            labelTimeMillis += labelStrategy.timeIncrement
        }
    }
}

@Preview(name = "App Usage Timeline Chart", showBackground = true)
@Composable
private fun AppUsageTimelineChartPreview() {
    val now = System.currentTimeMillis()
    val startTime = now - TimeUnit.HOURS.toMillis(12)
    val fakeModel = UsageTimelineModel(
        viewStart = startTime,
        viewEnd = now,
        screenOnPeriods = listOf(
            ScreenOnPeriod(
                startTimestamp = startTime + TimeUnit.HOURS.toMillis(1),
                endTimestamp = startTime + TimeUnit.HOURS.toMillis(3),
                appSegments = listOf(
                    AppSegment("com.android.chrome", startTime + TimeUnit.HOURS.toMillis(1), startTime + TimeUnit.HOURS.toMillis(2), "#4CAF50"),
                    AppSegment("com.google.android.gm", startTime + TimeUnit.HOURS.toMillis(2), startTime + TimeUnit.HOURS.toMillis(3), "#F44336")
                )
            ),
            ScreenOnPeriod(
                startTimestamp = startTime + TimeUnit.HOURS.toMillis(5),
                endTimestamp = startTime + TimeUnit.HOURS.toMillis(8),
                appSegments = listOf(
                    AppSegment("com.twitter.android", startTime + TimeUnit.HOURS.toMillis(5), startTime + TimeUnit.HOURS.toMillis(8), "#2196F3"),
                    // This segment has no color, so it will appear as the base "screen on" color
                    AppSegment("com.untracked.app", startTime + TimeUnit.HOURS.toMillis(6), startTime + TimeUnit.HOURS.toMillis(7), null)
                )
            )
        ),
        pickupCount = 2,
        totalScreenOnTime = TimeUnit.HOURS.toMillis(5)
    )

    HabitsTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AppUsageTimelineChart(
                model = fakeModel,
                labelStrategy = TimelineLabelStrategy.TWELVE_HOURS,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
    }
}