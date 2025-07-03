package com.andrew264.habits.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Defines the labeling strategy for the timeline chart based on the total duration being displayed.
 * @param timeIncrement The interval between each label in milliseconds.
 * @param formatterPattern The date format pattern for `SimpleDateFormat`.
 * @param chronoUnit The unit to which the start time should be truncated for alignment.
 */
enum class TimelineLabelStrategy(
    val timeIncrement: Long,
    val formatterPattern: String,
    val chronoUnit: ChronoUnit
) {
    TWELVE_HOURS(TimeUnit.HOURS.toMillis(2), "ha", ChronoUnit.HOURS),
    DAY(TimeUnit.HOURS.toMillis(4), "ha", ChronoUnit.HOURS),
    WEEK(TimeUnit.DAYS.toMillis(1), "E", ChronoUnit.DAYS)
}

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
    getColor: (T) -> Color,
    viewStartTimeMillis: Long,
    viewEndTimeMillis: Long,
    labelStrategy: TimelineLabelStrategy,
    modifier: Modifier = Modifier,
    barHeight: Dp = 24.dp,
    labelSpacing: Dp = 6.dp,
    tickHeight: Dp = 4.dp
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val labelTextStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    val barOutlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val barBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cornerRadius = CornerRadius(barHeight.value / 3)

    val timeFormatter = remember(labelStrategy.formatterPattern) {
        SimpleDateFormat(labelStrategy.formatterPattern, Locale.getDefault())
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val mainBarHeightPx = barHeight.toPx()
        val barTopY = (canvasHeight - mainBarHeightPx - labelSpacing.toPx() * 2 - tickHeight.toPx()) / 2f
        val totalDurationMillis = viewEndTimeMillis - viewStartTimeMillis

        if (totalDurationMillis <= 0) return@Canvas

        // Draw background
        drawRoundRect(
            color = barBackgroundColor,
            topLeft = Offset(0f, barTopY),
            size = Size(canvasWidth, mainBarHeightPx),
            cornerRadius = cornerRadius
        )

        // Draw segments
        segments.forEach { segment ->
            val segmentStartTime = getStartTimeMillis(segment)
            val segmentEndTime = getEndTimeMillis(segment)

            val startOffset = max(0L, segmentStartTime - viewStartTimeMillis)
            val endOffset = segmentEndTime - viewStartTimeMillis
            val segmentWidth = ((endOffset - startOffset).toFloat() / totalDurationMillis.toFloat()) * canvasWidth
            val currentX = (startOffset.toFloat() / totalDurationMillis.toFloat()) * canvasWidth

            if (segmentWidth > 0f) {
                drawRect(
                    color = getColor(segment),
                    topLeft = Offset(currentX, barTopY),
                    size = Size(segmentWidth, mainBarHeightPx)
                )
            }
        }

        // Clip drawing to the rounded rectangle shape for the outline
        drawRoundRect(
            color = barOutlineColor,
            topLeft = Offset(0f, barTopY),
            size = Size(canvasWidth, mainBarHeightPx),
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
                val labelText = timeFormatter.format(Date(labelTimeMillis)).lowercase()
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