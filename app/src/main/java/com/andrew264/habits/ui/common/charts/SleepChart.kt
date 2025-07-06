package com.andrew264.habits.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.ui.theme.Dimens
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

private const val DAY_START_HOUR = 18 // 6 PM

/**
 * A generic composable to display a sleep chart where each day is a vertical column
 * and the Y-axis represents a 24-hour cycle starting from 6 PM.
 *
 * @param T The type of data for each segment.
 * @param segments The list of data items to render as segments.
 * @param getStartTimeMillis A lambda to extract the start timestamp from a segment item.
 * @param getEndTimeMillis A lambda to extract the end timestamp from a segment item.
 * @param getState A lambda to extract the state (for color) from a segment item.
 * @param getColorForState A lambda to determine the color for a given state.
 * @param rangeInDays The number of days to display on the X-axis.
 * @param modifier The modifier to be applied to the chart.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun <T> SleepChart(
    segments: List<T>,
    getStartTimeMillis: (T) -> Long,
    getEndTimeMillis: (T) -> Long,
    getState: (T) -> UserPresenceState,
    getColorForState: (UserPresenceState) -> Color,
    rangeInDays: Int,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val timeFormatter = remember { DateTimeFormatter.ofPattern("ha") }
    val groupedSegments = remember(segments, rangeInDays) {
        processAndGroupSegments(segments, getStartTimeMillis, getEndTimeMillis, getState)
    }

    val typography = MaterialTheme.typography
    val axisTextStyle = remember(typography, gridColor) {
        typography.bodySmall.copy(color = gridColor)
    }
    val yAxisTextStyle = remember(typography, gridColor) {
        typography.bodySmall.copy(color = gridColor, textAlign = TextAlign.Start)
    }

    Canvas(modifier = modifier) {
        val yAxisWidth = 50.dp.toPx()
        val xAxisHeight = 30.dp.toPx()
        val chartAreaWidth = size.width - yAxisWidth
        val chartAreaHeight = size.height - xAxisHeight
        val dayColumnWidth = chartAreaWidth / rangeInDays

        if (chartAreaWidth <= 0 || chartAreaHeight <= 0) return@Canvas

        // Calculate the interval for drawing labels to avoid overlap
        val sampleLabelWidth = textMeasurer.measure(
            text = "30", // Use a two-digit number for a representative width
            style = axisTextStyle
        ).size.width
        val requiredWidthPerLabel = sampleLabelWidth + Dimens.PaddingSmall.toPx()

        val labelInterval = if (dayColumnWidth < requiredWidthPerLabel) {
            ceil(requiredWidthPerLabel / dayColumnWidth).toInt()
        } else {
            1
        }

        drawYAxis(textMeasurer, chartAreaHeight, size.width, yAxisWidth, gridColor, timeFormatter, yAxisTextStyle)

        val today = LocalDate.now(ZoneId.systemDefault())
        for (i in 0 until rangeInDays) {
            val date = today.minusDays((rangeInDays - 1) - i.toLong())
            val columnX = i * dayColumnWidth

            // Always draw the grid line for each day
            drawXAxisGridLine(columnX, chartAreaHeight, gridColor)

            // Conditionally draw the text label to prevent overlap
            if (i % labelInterval == 0) {
                drawXAxisTextLabel(textMeasurer, date, columnX, dayColumnWidth, chartAreaHeight, gridColor, axisTextStyle)
            }

            groupedSegments[date]?.forEach { segment ->
                drawSleepBar(
                    segment = segment,
                    dayColumnX = columnX,
                    dayColumnWidth = dayColumnWidth,
                    chartAreaHeight = chartAreaHeight,
                    dayStartMillis = getDisplayDayStartMillis(date),
                    color = getColorForState(segment.state)
                )
            }
        }
    }
}

private data class ProcessedSegment(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val state: UserPresenceState
)

private fun <T> processAndGroupSegments(
    segments: List<T>,
    getStartTimeMillis: (T) -> Long,
    getEndTimeMillis: (T) -> Long,
    getState: (T) -> UserPresenceState,
): Map<LocalDate, List<ProcessedSegment>> {
    return segments
        .map {
            ProcessedSegment(
                startTimeMillis = getStartTimeMillis(it),
                endTimeMillis = getEndTimeMillis(it),
                state = getState(it)
            )
        }
        .groupBy { getDisplayDayForTimestamp(it.startTimeMillis) }
}

private fun getDisplayDayForTimestamp(timestamp: Long): LocalDate {
    val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    return if (ldt.hour < DAY_START_HOUR) {
        ldt.toLocalDate().minusDays(1)
    } else {
        ldt.toLocalDate()
    }
}

private fun getDisplayDayStartMillis(date: LocalDate): Long {
    val ldt = date.atTime(DAY_START_HOUR, 0)
    return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawYAxis(
    textMeasurer: TextMeasurer,
    chartHeight: Float,
    totalWidth: Float,
    yAxisWidth: Float,
    gridColor: Color,
    timeFormatter: DateTimeFormatter,
    yAxisTextStyle: TextStyle
) {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val yAxisHours = listOf(18, 22, 2, 6, 10, 14, 18) // 6PM, 10PM, 2AM ... 6PM

    yAxisHours.forEachIndexed { index, hour ->
        val y = (index.toFloat() / (yAxisHours.size - 1)) * chartHeight
        val time = LocalTime.of(hour, 0)
        val label = time.format(timeFormatter).lowercase()

        val textLayoutResult = textMeasurer.measure(
            text = label,
            style = yAxisTextStyle,
            constraints = Constraints(maxWidth = yAxisWidth.toInt() - Dimens.PaddingSmall.toPx().toInt())
        )
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(totalWidth - yAxisWidth + Dimens.PaddingSmall.toPx(), y - textLayoutResult.size.height / 2)
        )

        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(totalWidth - yAxisWidth, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = pathEffect
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawXAxisTextLabel(
    textMeasurer: TextMeasurer,
    date: LocalDate,
    columnX: Float,
    columnWidth: Float,
    chartHeight: Float,
    gridColor: Color,
    axisTextStyle: TextStyle
) {
    val label = date.dayOfMonth.toString()
    val textLayoutResult = textMeasurer.measure(
        text = label,
        style = axisTextStyle
    )

    val textX = columnX + (columnWidth / 2) - (textLayoutResult.size.width / 2)
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(textX, chartHeight + Dimens.PaddingSmall.toPx())
    )
}

private fun DrawScope.drawXAxisGridLine(
    columnX: Float,
    chartHeight: Float,
    gridColor: Color
) {
    // Draw vertical grid line
    drawLine(
        color = gridColor,
        start = Offset(columnX, 0f),
        end = Offset(columnX, chartHeight),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
}

private fun DrawScope.drawSleepBar(
    segment: ProcessedSegment,
    dayColumnX: Float,
    dayColumnWidth: Float,
    chartAreaHeight: Float,
    dayStartMillis: Long,
    color: Color
) {
    val fullDayMillis = TimeUnit.HOURS.toMillis(24)
    val barWidth = dayColumnWidth * 0.6f
    val barX = dayColumnX + (dayColumnWidth - barWidth) / 2

    val startOffsetMillis = max(0, segment.startTimeMillis - dayStartMillis)
    val endOffsetMillis = segment.endTimeMillis - dayStartMillis

    val y = (startOffsetMillis.toFloat() / fullDayMillis) * chartAreaHeight
    val height = ((endOffsetMillis - startOffsetMillis).toFloat() / fullDayMillis) * chartAreaHeight

    if (height > 0) {
        drawRect(
            color = color,
            topLeft = Offset(barX, y),
            size = Size(barWidth, height)
        )
    }
}