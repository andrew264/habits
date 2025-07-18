package com.andrew264.habits.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.ui.bedtime.components.toColor
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * A generic composable to display a horizontal timeline with colored segments.
 *
 * @param T The type of data for each segment.
 * @param segments The list of data items to render as segments.
 * @param getStartTimeMillis A lambda to extract the start timestamp from a segment item.
 * @param getEndTimeMillis A lambda to extract the end timestamp from a segment item.
 * @param getColor A lambda to determine the color for a segment item.
 * @param viewStartTimeMillis The timestamp for the start of the visible timeline window.
 * @param viewEndTimeMillis The timestamp for the end of the visible timeline window.
 * @param modifier The modifier to be applied to the chart.
 * @param labelStrategy The strategy to use for drawing time labels below the chart.
 * @param barHeight The height of the main timeline bar.
 * @param labelSpacing The vertical space between the timeline bar and the labels.
 * @param tickHeight The height of the small tick marks above the labels.
 */
@Composable
fun <T> TimelineChart(
    segments: List<T>,
    getStartTimeMillis: (T) -> Long,
    getEndTimeMillis: (T) -> Long,
    getColor: @Composable (T) -> Color,
    viewStartTimeMillis: Long,
    viewEndTimeMillis: Long,
    labelStrategy: TimelineLabelStrategy,
    modifier: Modifier = Modifier,
    barHeight: Dp = Dimens.PaddingExtraLarge,
    labelSpacing: Dp = 6.dp,
    tickHeight: Dp = Dimens.PaddingExtraSmall
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    val labelTextStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val tickColor = MaterialTheme.colorScheme.outline

    val barOutlineColor = MaterialTheme.colorScheme.outline
    val barBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val cornerRadius = CornerRadius(barHeight.value / 3)

    // Since getColor is now a composable, we need to resolve colors before drawing
    val resolvedColors = segments.map { getColor(it) }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val mainBarHeightPx = barHeight.toPx()
        val barTopY = (canvasHeight - mainBarHeightPx - labelSpacing.toPx() * 2 - tickHeight.toPx()) / 2f
        val totalDurationMillis = viewEndTimeMillis - viewStartTimeMillis

        if (totalDurationMillis <= 0) return@Canvas

        val mainBarRect = Rect(Offset(0f, barTopY), Size(canvasWidth, mainBarHeightPx))
        val roundedRectPath = Path().apply {
            addRoundRect(RoundRect(mainBarRect, cornerRadius))
        }

        // Draw the segments clipped to the rounded shape
        clipPath(path = roundedRectPath) {
            // Draw a base background color first. This will be visible for any gaps.
            drawRect(
                color = barBackgroundColor,
                topLeft = mainBarRect.topLeft,
                size = mainBarRect.size
            )

            // Draw the actual segments
            segments.forEachIndexed { index, segment ->
                val segmentStartTime = getStartTimeMillis(segment)
                val segmentEndTime = getEndTimeMillis(segment)

                val startOffset = max(0L, segmentStartTime - viewStartTimeMillis)
                val endOffset = segmentEndTime - viewStartTimeMillis
                val segmentWidth = ((endOffset - startOffset).toFloat() / totalDurationMillis.toFloat()) * canvasWidth
                val currentX = (startOffset.toFloat() / totalDurationMillis.toFloat()) * canvasWidth

                if (segmentWidth > 0f) {
                    drawRect(
                        color = resolvedColors[index],
                        topLeft = Offset(currentX, barTopY),
                        size = Size(segmentWidth, mainBarHeightPx)
                    )
                }
            }
        }

        // Draw the outline over the clipped content
        drawRoundRect(
            color = barOutlineColor,
            topLeft = mainBarRect.topLeft,
            size = mainBarRect.size,
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx())
        )

        val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(viewStartTimeMillis), ZoneId.systemDefault())
        val roundedStartDateTime = startDateTime.truncatedTo(labelStrategy.chronoUnit)
        val roundedStartTimeMillis = roundedStartDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        var labelTimeMillis = roundedStartTimeMillis
        val drawnLabels = mutableListOf<Pair<TextLayoutResult, Float>>()

        while (labelTimeMillis <= viewEndTimeMillis) {
            if (labelTimeMillis >= viewStartTimeMillis) {
                val labelText = FormatUtils.formatTimestamp(labelTimeMillis, labelStrategy.formatterPattern)
                val textLayoutResult = textMeasurer.measure(text = labelText, style = labelTextStyle)

                val labelX = (labelTimeMillis - viewStartTimeMillis).toFloat() / totalDurationMillis * canvasWidth
                val textX = (labelX - textLayoutResult.size.width / 2f).coerceIn(0f, canvasWidth - textLayoutResult.size.width)

                val canDraw = drawnLabels.none { (prevLayout, prevX) ->
                    textX < prevX + prevLayout.size.width && textX + textLayoutResult.size.width > prevX
                }

                if (canDraw) {
                    drawnLabels.add(textLayoutResult to textX)

                    // Draw tick mark
                    drawLine(
                        color = tickColor,
                        start = Offset(labelX, barTopY + mainBarHeightPx),
                        end = Offset(labelX, barTopY + mainBarHeightPx + tickHeight.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw text
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(textX, barTopY + mainBarHeightPx + labelSpacing.toPx() + tickHeight.toPx())
                    )
                }
            }
            labelTimeMillis += labelStrategy.timeIncrement
        }
    }
}

@Preview(name = "Timeline Chart - Day", showBackground = true)
@Composable
private fun TimelineChartDayPreview() {
    val now = System.currentTimeMillis()
    val startTime = now - TimeUnit.DAYS.toMillis(1)

    data class SampleSegment(
        val start: Long,
        val end: Long,
        val state: UserPresenceState
    )

    val segments = remember {
        listOf(
            SampleSegment(startTime, startTime + TimeUnit.HOURS.toMillis(8), UserPresenceState.SLEEPING),
            SampleSegment(startTime + TimeUnit.HOURS.toMillis(8), startTime + TimeUnit.HOURS.toMillis(18), UserPresenceState.AWAKE),
            SampleSegment(startTime + TimeUnit.HOURS.toMillis(18), now, UserPresenceState.UNKNOWN)
        )
    }

    HabitsTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TimelineChart(
                segments = segments,
                getStartTimeMillis = { it.start },
                getEndTimeMillis = { it.end },
                getColor = { it.state.toColor() },
                viewStartTimeMillis = startTime,
                viewEndTimeMillis = now,
                labelStrategy = TimelineLabelStrategy.DAY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
    }
}

@Preview(name = "Timeline Chart - 12 Hours", showBackground = true)
@Composable
private fun TimelineChart12HourPreview() {
    val now = System.currentTimeMillis()
    val startTime = now - TimeUnit.HOURS.toMillis(12)

    data class SampleSegment(
        val start: Long,
        val end: Long,
        val state: UserPresenceState
    )

    val segments = remember {
        listOf(
            SampleSegment(startTime, startTime + TimeUnit.HOURS.toMillis(4), UserPresenceState.AWAKE),
            SampleSegment(startTime + TimeUnit.HOURS.toMillis(4), startTime + TimeUnit.HOURS.toMillis(8), UserPresenceState.SLEEPING),
            SampleSegment(startTime + TimeUnit.HOURS.toMillis(8), now, UserPresenceState.AWAKE)
        )
    }

    HabitsTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TimelineChart(
                segments = segments,
                getStartTimeMillis = { it.start },
                getEndTimeMillis = { it.end },
                getColor = { it.state.toColor() },
                viewStartTimeMillis = startTime,
                viewEndTimeMillis = now,
                labelStrategy = TimelineLabelStrategy.TWELVE_HOURS,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
    }
}