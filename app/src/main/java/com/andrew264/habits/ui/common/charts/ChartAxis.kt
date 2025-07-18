package com.andrew264.habits.ui.common.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens

/**
 * Represents a single label on the Y-axis.
 * @param label The text to display.
 * @param positionFraction The vertical position of the label, from 0.0 (top) to 1.0 (bottom).
 */
data class YAxisLabelInfo(
    val label: String,
    val positionFraction: Float
)

/**
 * Draws Y-axis labels and optional horizontal grid lines.
 */
fun DrawScope.drawYAxis(
    labels: List<YAxisLabelInfo>,
    yAxisWidth: Float,
    chartAreaHeight: Float,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    gridColor: Color,
    drawGridLines: Boolean = true
) {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    labels.forEach { (label, positionFraction) ->
        val y = chartAreaHeight * positionFraction
        val textLayoutResult = textMeasurer.measure(
            text = label,
            style = textStyle,
            constraints = Constraints(maxWidth = yAxisWidth.toInt() - Dimens.PaddingSmall.toPx().toInt())
        )
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(yAxisWidth - textLayoutResult.size.width - Dimens.PaddingSmall.toPx(), y - textLayoutResult.size.height / 2)
        )

        // Don't draw a grid line at the very top (position 0.0)
        if (drawGridLines && positionFraction > 0.001f) {
            drawLine(
                color = gridColor,
                start = Offset(yAxisWidth, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect
            )
        }
    }
}

/**
 * Draws X-axis labels and an optional horizontal axis line.
 */
fun DrawScope.drawXAxis(
    labels: List<String>,
    yAxisWidth: Float,
    chartAreaHeight: Float,
    barAreaWidth: Float,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    gridColor: Color,
    labelInterval: Int = 1,
    drawAxisLine: Boolean = true
) {
    if (drawAxisLine) {
        drawLine(
            color = gridColor,
            start = Offset(yAxisWidth, chartAreaHeight),
            end = Offset(size.width, chartAreaHeight),
            strokeWidth = 1.dp.toPx()
        )
    }

    labels.forEachIndexed { index, label ->
        if (index % labelInterval == 0) {
            val textLayoutResult = textMeasurer.measure(
                text = label,
                style = textStyle
            )
            val x = yAxisWidth + index * barAreaWidth + barAreaWidth / 2 - textLayoutResult.size.width / 2
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x, chartAreaHeight + Dimens.PaddingSmall.toPx())
            )
        }
    }
}


/**
 * Draws X-axis labels with vertical grid lines for each column. Specific to charts like SleepChart.
 */
fun DrawScope.drawXAxisWithVerticalGridLines(
    labels: List<String>,
    yAxisWidth: Float,
    chartAreaHeight: Float,
    barAreaWidth: Float,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    gridColor: Color,
    labelInterval: Int = 1,
) {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    labels.forEachIndexed { index, label ->
        val columnStartX = yAxisWidth + (index * barAreaWidth)

        // Draw vertical grid line for every column
        drawLine(
            color = gridColor,
            start = Offset(columnStartX, 0f),
            end = Offset(columnStartX, chartAreaHeight),
            strokeWidth = 1.dp.toPx(),
            pathEffect = pathEffect
        )

        if (index % labelInterval == 0) {
            val textLayoutResult = textMeasurer.measure(
                text = label,
                style = textStyle
            )
            val x = columnStartX + barAreaWidth / 2 - textLayoutResult.size.width / 2
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x, chartAreaHeight + Dimens.PaddingSmall.toPx())
            )
        }
    }
}