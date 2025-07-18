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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.ui.bedtime.components.toColor
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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
@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)

    val timeFormatter = remember { DateTimeFormatter.ofPattern("ha") }
    val groupedSegments = remember(segments, rangeInDays) {
        processAndGroupSegments(segments, getStartTimeMillis, getEndTimeMillis, getState)
    }

    val flatSegmentsToAnimate = remember(groupedSegments) {
        groupedSegments.values.flatten()
    }

    val animationProgress = remember(flatSegmentsToAnimate) {
        flatSegmentsToAnimate.map { Animatable(0f) }
    }

    val animationSpec: AnimationSpec<Float> = MaterialTheme.motionScheme.defaultSpatialSpec()

    LaunchedEffect(flatSegmentsToAnimate) {
        animationProgress.forEach { it.snapTo(0f) }
        animationProgress.forEachIndexed { index, animatable ->
            launch {
                delay(index * 15L)
                animatable.animateTo(1f, animationSpec)
            }
        }
    }


    val typography = MaterialTheme.typography
    val axisTextStyle = remember(typography, gridColor) {
        typography.bodySmall.copy(color = gridColor)
    }
    val yAxisTextStyle = remember(typography, gridColor) {
        typography.bodySmall.copy(color = gridColor, textAlign = TextAlign.End)
    }

    val yAxisLabelInfo = remember(timeFormatter) {
        val yAxisHours = listOf(18, 22, 2, 6, 10, 14, 18) // 6PM, 10PM, 2AM ... 6PM
        yAxisHours.mapIndexed { index, hour ->
            val time = LocalTime.of(hour, 0)
            YAxisLabelInfo(
                label = time.format(timeFormatter).lowercase(),
                positionFraction = index.toFloat() / (yAxisHours.size - 1)
            )
        }
    }

    val dates = remember(rangeInDays) {
        val today = LocalDate.now(ZoneId.systemDefault())
        (0 until rangeInDays).map { i ->
            today.minusDays((rangeInDays - 1) - i.toLong())
        }
    }
    val xAxisLabels = remember(dates) {
        dates.map { it.dayOfMonth.toString() }
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

        drawYAxis(yAxisLabelInfo, yAxisWidth, chartAreaHeight, textMeasurer, yAxisTextStyle, gridColor)
        drawXAxisWithVerticalGridLines(xAxisLabels, yAxisWidth, chartAreaHeight, dayColumnWidth, textMeasurer, axisTextStyle, gridColor, labelInterval)

        dates.forEachIndexed { i, date ->
            val columnStartX = yAxisWidth + (i * dayColumnWidth)
            groupedSegments[date]?.forEach { segment ->
                val animationIndex = flatSegmentsToAnimate.indexOf(segment)
                val progress = if (animationIndex != -1) animationProgress[animationIndex].value else 1f

                drawSleepBar(
                    segment = segment,
                    dayColumnX = columnStartX,
                    dayColumnWidth = dayColumnWidth,
                    chartAreaHeight = chartAreaHeight,
                    dayStartMillis = getDisplayDayStartMillis(date),
                    color = getColorForState(segment.state),
                    animationProgress = progress
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

/**
 * Processes raw segments, splitting any that cross the 6 PM display day boundary.
 * This ensures that a single sleep period is correctly represented across multiple columns if necessary.
 */
private fun <T> processAndGroupSegments(
    segments: List<T>,
    getStartTimeMillis: (T) -> Long,
    getEndTimeMillis: (T) -> Long,
    getState: (T) -> UserPresenceState,
): Map<LocalDate, List<ProcessedSegment>> {
    val grouped = mutableMapOf<LocalDate, MutableList<ProcessedSegment>>()

    val initialProcessed = segments.map {
        ProcessedSegment(
            startTimeMillis = getStartTimeMillis(it),
            endTimeMillis = getEndTimeMillis(it),
            state = getState(it)
        )
    }

    initialProcessed.forEach { segment ->
        var currentStartMillis = segment.startTimeMillis
        val segmentEndMillis = segment.endTimeMillis

        while (currentStartMillis < segmentEndMillis) {
            val displayDay = getDisplayDayForTimestamp(currentStartMillis)

            // A display day runs for 24 hours starting at 6 PM.
            val displayDayEndMillis = getDisplayDayStartMillis(displayDay) + TimeUnit.HOURS.toMillis(24)

            val partEndMillis = min(segmentEndMillis, displayDayEndMillis)

            val list = grouped.getOrPut(displayDay) { mutableListOf() }
            if (partEndMillis > currentStartMillis) { // Avoid adding zero-duration segments
                list.add(ProcessedSegment(currentStartMillis, partEndMillis, segment.state))
            }

            currentStartMillis = partEndMillis
        }
    }
    return grouped
}

/**
 * Determines which "display day" a timestamp belongs to. A display day starts at 6 PM.
 * For example, 5 AM on May 2nd belongs to the display day of May 1st.
 */
private fun getDisplayDayForTimestamp(timestamp: Long): LocalDate {
    val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    return if (ldt.hour < DAY_START_HOUR) {
        ldt.toLocalDate().minusDays(1)
    } else {
        ldt.toLocalDate()
    }
}

/**
 * Gets the start timestamp (in milliseconds) for a given display day, which is 6 PM on that calendar date.
 */
private fun getDisplayDayStartMillis(date: LocalDate): Long {
    val ldt = date.atTime(DAY_START_HOUR, 0)
    return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun DrawScope.drawSleepBar(
    segment: ProcessedSegment,
    dayColumnX: Float,
    dayColumnWidth: Float,
    chartAreaHeight: Float,
    dayStartMillis: Long,
    color: Color,
    animationProgress: Float
) {
    val fullDayMillis = TimeUnit.HOURS.toMillis(24)
    val barWidth = dayColumnWidth * 0.6f
    val barX = dayColumnX + (dayColumnWidth - barWidth) / 2

    val startOffsetMillis = max(0, segment.startTimeMillis - dayStartMillis)
    val endOffsetMillis = segment.endTimeMillis - dayStartMillis

    val y = (startOffsetMillis.toFloat() / fullDayMillis) * chartAreaHeight
    val height = ((endOffsetMillis - startOffsetMillis).toFloat() / fullDayMillis) * chartAreaHeight * animationProgress

    if (height > 0) {
        drawRoundRect(
            color = color,
            topLeft = Offset(barX, y),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth / 4),
        )
    }
}

@Preview(name = "Sleep Chart", showBackground = true)
@Composable
private fun SleepChartPreview() {
    val now = System.currentTimeMillis()
    val segments = remember {
        (0..6).map { day ->
            val dayStart = now - TimeUnit.DAYS.toMillis(day.toLong())
            ProcessedSegment(
                startTimeMillis = dayStart - TimeUnit.HOURS.toMillis(2), // 10 PM previous day
                endTimeMillis = dayStart + TimeUnit.HOURS.toMillis(6), // 6 AM this day
                state = UserPresenceState.SLEEPING
            )
        }
    }

    HabitsTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SleepChart(
                segments = segments,
                getStartTimeMillis = { it.startTimeMillis },
                getEndTimeMillis = { it.endTimeMillis },
                getState = { it.state },
                getColorForState = { it.toColor() },
                rangeInDays = 7,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
    }
}