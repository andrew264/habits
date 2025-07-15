package com.andrew264.habits.ui.common.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

data class BarChartEntry(
    val value: Float,
    val label: String
)

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BarChart(
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
    topValue: Float? = null,
    yAxisLabelFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    if (entries.isEmpty()) {
        return
    }

    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animationProgress = remember(entries) {
        entries.map { Animatable(0f) }
    }

    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val typography = MaterialTheme.typography
    val valueTextStyle = remember(typography, onPrimaryColor) {
        typography.bodySmall.copy(color = onPrimaryColor, fontWeight = FontWeight.Bold)
    }
    val yAxisTextStyle = remember(typography, gridColor) {
        typography.bodySmall.copy(color = gridColor, textAlign = TextAlign.End)
    }
    val xAxisTextStyle = remember(typography, gridColor) {
        typography.bodySmall.copy(color = gridColor)
    }
    val animationSpec: AnimationSpec<Float> = MaterialTheme.motionScheme.defaultSpatialSpec()

    LaunchedEffect(entries) {
        animationProgress.forEachIndexed { index, animatable ->
            launch {
                delay(index * 50L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = animationSpec
                )
            }
        }
    }

    val maxValue = entries.maxOfOrNull { it.value } ?: 0f
    val yAxisLabelCount = 4 // Keep it even for better intervals
    val yAxisTopValue = topValue ?: (if (maxValue == 0f) yAxisLabelCount.toFloat() else ceil(maxValue / yAxisLabelCount) * yAxisLabelCount)

    val yAxisValues = (0..yAxisLabelCount).map {
        (yAxisTopValue / yAxisLabelCount) * it
    }

    Canvas(
        modifier = modifier
            .pointerInput(entries) {
                detectTapGestures(
                    onTap = { offset ->
                        val yAxisWidth = 60.dp.toPx()
                        if (offset.x > yAxisWidth) {
                            val chartAreaWidth = size.width - yAxisWidth
                            val barAreaWidth = chartAreaWidth / entries.size
                            val clickedIndex = ((offset.x - yAxisWidth) / barAreaWidth)
                                .toInt()
                                .coerceIn(0, entries.size - 1)
                            selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                        } else {
                            selectedIndex = null
                        }
                    }
                )
            }
    ) {
        val yAxisWidth = 60.dp.toPx()
        val xAxisHeight = 40.dp.toPx()
        val chartAreaWidth = size.width - yAxisWidth
        val chartAreaHeight = size.height - xAxisHeight

        if (chartAreaWidth <= 0 || chartAreaHeight <= 0) return@Canvas

        val barAreaWidth = chartAreaWidth / entries.size
        val barWidth = barAreaWidth * 0.6f
        val barSpacing = barAreaWidth * 0.4f

        drawYAxis(textMeasurer, yAxisValues, yAxisWidth, chartAreaHeight, gridColor, size.width, yAxisTextStyle, yAxisLabelFormatter)
        drawXAxis(textMeasurer, entries, yAxisWidth, chartAreaHeight, barAreaWidth, gridColor, xAxisTextStyle)

        entries.forEachIndexed { index, entry ->
            val barHeight = if (yAxisTopValue > 0) (entry.value / yAxisTopValue) * chartAreaHeight * animationProgress[index].value else 0f
            val barLeft = yAxisWidth + barSpacing / 2 + index * barAreaWidth

            val rect = Rect(
                left = barLeft,
                top = chartAreaHeight - barHeight,
                right = barLeft + barWidth,
                bottom = chartAreaHeight
            )

            val color = if (selectedIndex == index) barColor.copy(alpha = 0.8f) else barColor
            drawRoundRect(
                color = color,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(x = 6.dp.toPx(), y = 6.dp.toPx())
            )

            if (selectedIndex == index) {
                val valueText = yAxisLabelFormatter(entry.value)
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(valueText),
                    style = valueTextStyle
                )
                val textTopLeft = Offset(
                    x = rect.center.x - textLayoutResult.size.width / 2,
                    y = (rect.top - textLayoutResult.size.height - Dimens.PaddingSmall.toPx()).coerceAtLeast(0f)
                )

                val bgRect = Rect(
                    left = textTopLeft.x - 6.dp.toPx(),
                    top = textTopLeft.y - Dimens.PaddingExtraSmall.toPx(),
                    right = textTopLeft.x + textLayoutResult.size.width + 6.dp.toPx(),
                    bottom = textTopLeft.y + textLayoutResult.size.height + Dimens.PaddingExtraSmall.toPx()
                )
                drawRoundRect(
                    color = barColor,
                    topLeft = bgRect.topLeft,
                    size = bgRect.size,
                    cornerRadius = CornerRadius(Dimens.PaddingExtraSmall.toPx())
                )
                drawText(textLayoutResult, topLeft = textTopLeft)
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawYAxis(
    textMeasurer: TextMeasurer,
    yAxisValues: List<Float>,
    yAxisWidth: Float,
    chartAreaHeight: Float,
    gridColor: Color,
    totalWidth: Float,
    yAxisTextStyle: TextStyle,
    yAxisLabelFormatter: (Float) -> String
) {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    yAxisValues.forEach { value ->
        val y = if (yAxisValues.last() > 0) chartAreaHeight - (value / yAxisValues.last()) * chartAreaHeight else chartAreaHeight

        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString(yAxisLabelFormatter(value)),
            style = yAxisTextStyle,
            constraints = Constraints(maxWidth = yAxisWidth.toInt() - Dimens.PaddingSmall.toPx().toInt())
        )
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(yAxisWidth - textLayoutResult.size.width - Dimens.PaddingSmall.toPx(), y - textLayoutResult.size.height / 2)
        )

        if (value > 0) {
            drawLine(
                color = gridColor,
                start = Offset(yAxisWidth, y),
                end = Offset(totalWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawXAxis(
    textMeasurer: TextMeasurer,
    entries: List<BarChartEntry>,
    yAxisWidth: Float,
    chartAreaHeight: Float,
    barAreaWidth: Float,
    gridColor: Color,
    xAxisTextStyle: TextStyle
) {
    drawLine(
        color = gridColor,
        start = Offset(yAxisWidth, chartAreaHeight),
        end = Offset(yAxisWidth + entries.size * barAreaWidth, chartAreaHeight),
        strokeWidth = 1.dp.toPx()
    )

    entries.forEachIndexed { index, entry ->
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString(entry.label),
            style = xAxisTextStyle
        )
        val x = yAxisWidth + index * barAreaWidth + barAreaWidth / 2 - textLayoutResult.size.width / 2
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(x, chartAreaHeight + Dimens.PaddingSmall.toPx())
        )
    }
}

@Preview(name = "Bar Chart", showBackground = true)
@Composable
private fun BarChartPreview() {
    val sampleEntries = remember {
        listOf(
            BarChartEntry(2500f, "Mon"),
            BarChartEntry(1800f, "Tue"),
            BarChartEntry(3100f, "Wed"),
            BarChartEntry(2200f, "Thu"),
            BarChartEntry(2800f, "Fri"),
            BarChartEntry(1500f, "Sat"),
            BarChartEntry(2000f, "Sun")
        )
    }
    HabitsTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BarChart(
                entries = sampleEntries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        }
    }
}

@Preview(name = "Bar Chart - Empty", showBackground = true)
@Composable
private fun BarChartEmptyPreview() {
    HabitsTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BarChart(
                entries = emptyList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        }
    }
}