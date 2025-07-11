package com.andrew264.habits.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.andrew264.habits.domain.model.AppSegment
import com.andrew264.habits.domain.model.ScreenOnPeriod
import com.andrew264.habits.domain.model.UsageTimelineModel
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
fun DualTrackTimelineChart(
    model: UsageTimelineModel,
    modifier: Modifier = Modifier,
    labelStrategy: TimelineLabelStrategy = TimelineLabelStrategy.DAY,
    trackHeight: Dp = 40.dp,
    labelAreaHeight: Dp = 24.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    val labelTextStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val screenOffColor = MaterialTheme.colorScheme.primaryContainer
    val screenOnColor = MaterialTheme.colorScheme.onPrimaryContainer

    val timeFormatter = remember(labelStrategy.formatterPattern) {
        SimpleDateFormat(labelStrategy.formatterPattern, Locale.getDefault())
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight + labelAreaHeight)
        ) {
            val canvasWidth = size.width
            val trackHeightPx = trackHeight.toPx()

            val totalDurationMillis = model.viewEnd - model.viewStart
            if (totalDurationMillis <= 0) return@Canvas

            val trackCornerRadius = CornerRadius(trackHeightPx / 4)

            // 1. Draw the base "screen off" track
            drawRoundRect(
                color = screenOffColor,
                topLeft = Offset.Zero,
                size = Size(canvasWidth, trackHeightPx),
                cornerRadius = trackCornerRadius
            )

            // 2. Draw screen-on periods
            val screenOnTrackHeight = trackHeightPx * 0.75f
            val screenOnTrackY = (trackHeightPx - screenOnTrackHeight) / 2f
            val screenOnCornerRadius = CornerRadius(screenOnTrackHeight / 4)

            model.screenOnPeriods.forEach { period ->
                val startOffset = max(0L, period.startTimestamp - model.viewStart)
                val endOffset = period.endTimestamp - model.viewStart
                val periodWidth = ((endOffset - startOffset).toFloat() / totalDurationMillis) * canvasWidth
                val currentX = (startOffset.toFloat() / totalDurationMillis) * canvasWidth

                if (periodWidth > 0f) {
                    drawRoundRect(
                        color = screenOnColor,
                        topLeft = Offset(currentX, screenOnTrackY),
                        size = Size(periodWidth, screenOnTrackHeight),
                        cornerRadius = screenOnCornerRadius
                    )
                }
            }

            // 3. Draw the app usage segments
            val appTrackHeight = trackHeightPx / 2.5f
            val appTrackY = (trackHeightPx - appTrackHeight) / 2
            val appTrackCornerRadius = CornerRadius(appTrackHeight / 4)

            model.screenOnPeriods.forEach { period ->
                period.appSegments.forEach { segment ->
                    segment.color?.let { colorHex ->
                        val startOffset = max(0L, segment.startTimestamp - model.viewStart)
                        val endOffset = segment.endTimestamp - model.viewStart
                        val segmentWidth = ((endOffset - startOffset).toFloat() / totalDurationMillis) * canvasWidth
                        val currentX = (startOffset.toFloat() / totalDurationMillis) * canvasWidth

                        if (segmentWidth > 0f) {
                            drawRoundRect(
                                color = Color(colorHex.toColorInt()),
                                topLeft = Offset(currentX, appTrackY),
                                size = Size(segmentWidth, appTrackHeight),
                                cornerRadius = appTrackCornerRadius
                            )
                        }
                    }
                }
            }

            // 4. Draw Time Labels
            val labelY = trackHeightPx + 4.dp.toPx()
            val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(model.viewStart), ZoneId.systemDefault())
            val roundedStartDateTime = startDateTime.truncatedTo(labelStrategy.chronoUnit)
            val roundedStartTimeMillis = roundedStartDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            var labelTimeMillis = roundedStartTimeMillis
            val drawnLabels = mutableListOf<Pair<TextLayoutResult, Float>>()

            while (labelTimeMillis <= model.viewEnd) {
                if (labelTimeMillis >= model.viewStart) {
                    val labelText = timeFormatter.format(Date(labelTimeMillis)).lowercase()
                    val textLayoutResult = textMeasurer.measure(text = labelText, style = labelTextStyle)

                    val labelX = (labelTimeMillis - model.viewStart).toFloat() / totalDurationMillis * canvasWidth
                    val textX = (labelX - textLayoutResult.size.width / 2f).coerceIn(0f, canvasWidth - textLayoutResult.size.width)

                    val canDraw = drawnLabels.none { (prevLayout, prevX) ->
                        textX < prevX + prevLayout.size.width && textX + textLayoutResult.size.width > prevX
                    }

                    if (canDraw) {
                        drawnLabels.add(textLayoutResult to textX)
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(textX, labelY)
                        )
                    }
                }
                labelTimeMillis += labelStrategy.timeIncrement
            }
        }

        UsageTimelineLegend()
    }
}

@Composable
private fun UsageTimelineLegend(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primaryContainer, label = "Screen Off")
        LegendItem(color = MaterialTheme.colorScheme.onPrimaryContainer, label = "Screen On")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Preview(name = "Layered Usage Timeline Chart", showBackground = true)
@Composable
private fun DualTrackTimelineChartPreview() {
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
                    // This segment has no color, so it will not be drawn on the app track
                    AppSegment("com.untracked.app", startTime + TimeUnit.HOURS.toMillis(6), startTime + TimeUnit.HOURS.toMillis(7), null)
                )
            )
        ),
        pickupCount = 2,
        totalScreenOnTime = TimeUnit.HOURS.toMillis(5)
    )

    HabitsTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DualTrackTimelineChart(
                model = fakeModel,
                labelStrategy = TimelineLabelStrategy.TWELVE_HOURS,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Layered Usage Timeline Chart (Dark)", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DualTrackTimelineChartDarkPreview() {
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
                    AppSegment("com.twitter.android", startTime + TimeUnit.HOURS.toMillis(5), startTime + TimeUnit.HOURS.toMillis(8), "#2196F3")
                )
            )
        ),
        pickupCount = 2,
        totalScreenOnTime = TimeUnit.HOURS.toMillis(5)
    )

    HabitsTheme(isDarkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            DualTrackTimelineChart(
                model = fakeModel,
                labelStrategy = TimelineLabelStrategy.TWELVE_HOURS,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}