package com.andrew264.habits.ui.common.charts

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.Dimens
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveLineGraph(
    entries: List<BarChartEntry>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelectionChanged: (Int?) -> Unit = {},
    yAxisLabelFormatter: (Float) -> String = { it.toInt().toString() }
) {
    if (entries.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dotColor = MaterialTheme.colorScheme.surfaceContainer
    val typography = MaterialTheme.typography
    val textStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold
    )

    val yAxisTextStyle = remember(typography, gridColor) {
        typography.labelSmall.copy(color = textColor, textAlign = TextAlign.End)
    }
    val xAxisTextStyle = remember(typography, gridColor) {
        typography.labelSmall.copy(color = textColor)
    }

    val maxVisibleItems = entries.size.toFloat().coerceAtLeast(3f)
    val minVisibleItems = 3f

    var visibleItems by remember { mutableFloatStateOf(min(7f, maxVisibleItems)) }
    var offset by remember { mutableFloatStateOf(max(0f, entries.size - visibleItems)) }

    val startIdx = max(0, floor(offset).toInt())
    val endIdx = min(entries.size - 1, ceil(offset + visibleItems - 1).toInt())

    val visibleMax = remember(startIdx, endIdx, entries) {
        (startIdx..endIdx).maxOfOrNull { entries[it].value } ?: 1f
    }

    val targetMaxY = remember(visibleMax) { calculateNiceMax(visibleMax) }

    val animatedMaxY by animateFloatAsState(
        targetValue = targetMaxY,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = ""
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(entries) {
                detectTapGestures { tapOffset ->
                    val yAxisWidth = 50.dp.toPx()
                    val rightPadding = 24.dp.toPx()
                    val verticalPadding = 24.dp.toPx()
                    val chartAreaWidth = size.width - yAxisWidth - rightPadding
                    val xAxisHeight = 30.dp.toPx()
                    val chartAreaHeight = size.height - xAxisHeight - verticalPadding * 2
                    val itemSpacing = chartAreaWidth / max(1f, visibleItems - 1)

                    val clickedContinuousIndex = (tapOffset.x - yAxisWidth) / itemSpacing + offset
                    val closestIndex = clickedContinuousIndex.roundToInt()

                    val newSelection = if (closestIndex in entries.indices) {
                        val pointX = yAxisWidth + (closestIndex - offset) * itemSpacing
                        val pointY = verticalPadding + chartAreaHeight - (entries[closestIndex].value / animatedMaxY) * chartAreaHeight

                        val dx = tapOffset.x - pointX
                        val dy = tapOffset.y - pointY
                        if (dx * dx + dy * dy < 48.dp.toPx() * 48.dp.toPx()) {
                            if (selectedIndex == closestIndex) null else closestIndex
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                    if (selectedIndex != newSelection) {
                        onSelectionChanged(newSelection)
                        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                    }
                }
            }
            .pointerInput(entries) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (entries.size <= 1) return@detectTransformGestures
                    onSelectionChanged(null)

                    val oldVisibleItems = visibleItems
                    visibleItems = (visibleItems / zoom).coerceIn(minVisibleItems, maxVisibleItems)

                    val centroidFractionX = centroid.x / size.width
                    val centerIndex = offset + centroidFractionX * (oldVisibleItems - 1)
                    var newOffset = centerIndex - centroidFractionX * (visibleItems - 1)

                    val itemSpacing = size.width / (visibleItems - 1)
                    val panDeltaIndices = -pan.x / itemSpacing
                    newOffset += panDeltaIndices

                    offset = newOffset.coerceIn(0f, max(0f, entries.size - visibleItems))
                }
            }
    ) {
        val yAxisWidth = 50.dp.toPx()
        val rightPadding = 24.dp.toPx()
        val verticalPadding = 24.dp.toPx()
        val xAxisHeight = 30.dp.toPx()
        val chartAreaWidth = size.width - yAxisWidth - rightPadding
        val chartAreaHeight = size.height - xAxisHeight - verticalPadding * 2

        if (chartAreaWidth <= 0 || chartAreaHeight <= 0) return@Canvas

        val itemSpacing = chartAreaWidth / max(1f, visibleItems - 1)

        val yAxisLabelCount = 4
        for (i in 0..yAxisLabelCount) {
            val fraction = i.toFloat() / yAxisLabelCount
            val value = animatedMaxY * fraction
            val yPos = verticalPadding + chartAreaHeight - (chartAreaHeight * fraction)

            if (i > 0) {
                drawLine(
                    color = gridColor,
                    start = Offset(yAxisWidth, yPos),
                    end = Offset(size.width - rightPadding, yPos),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val textLayoutResult = textMeasurer.measure(
                text = yAxisLabelFormatter(value),
                style = yAxisTextStyle
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(yAxisWidth - textLayoutResult.size.width - Dimens.PaddingSmall.toPx(), yPos - textLayoutResult.size.height / 2)
            )
        }

        drawLine(
            color = gridColor,
            start = Offset(yAxisWidth, verticalPadding + chartAreaHeight),
            end = Offset(size.width - rightPadding, verticalPadding + chartAreaHeight),
            strokeWidth = 1.dp.toPx()
        )

        val points = entries.mapIndexed { index, entry ->
            val x = yAxisWidth + (index - offset) * itemSpacing
            val y = verticalPadding + chartAreaHeight - (entry.value / animatedMaxY) * chartAreaHeight
            Offset(x, y)
        }

        clipRect(left = yAxisWidth, top = 0f, right = size.width - rightPadding, bottom = verticalPadding + chartAreaHeight + 4.dp.toPx()) {
            if (points.size > 1) {
                val linePath = Path()
                points.forEachIndexed { i, point ->
                    if (i == 0) linePath.moveTo(point.x, point.y) else linePath.lineTo(point.x, point.y)
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, verticalPadding + chartAreaHeight)
                    lineTo(points.first().x, verticalPadding + chartAreaHeight)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.4f), Color.Transparent),
                        startY = verticalPadding,
                        endY = verticalPadding + chartAreaHeight
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            points.forEach { point ->
                drawCircle(color = dotColor, radius = 6.dp.toPx(), center = point)
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = point)
            }
        }

        val sampleLabelWidth = textMeasurer.measure("Www", style = xAxisTextStyle).size.width
        val requiredWidthPerLabel = sampleLabelWidth + Dimens.PaddingLarge.toPx()
        val labelInterval = max(1, ceil(requiredWidthPerLabel / itemSpacing).toInt())

        entries.forEachIndexed { index, entry ->
            if (index % labelInterval == 0) {
                val x = yAxisWidth + (index - offset) * itemSpacing
                if (x in (yAxisWidth - sampleLabelWidth)..(size.width)) {
                    val labelText = if (visibleItems < 7f && entry.timestamp != null) {
                        FormatUtils.formatChartDayLabel(entry.timestamp)
                    } else if (entry.timestamp != null) {
                        FormatUtils.formatShortDateLocaleAware(entry.timestamp)
                    } else {
                        entry.label
                    }

                    val textLayoutResult = textMeasurer.measure(labelText, style = xAxisTextStyle)
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(x - textLayoutResult.size.width / 2, verticalPadding + chartAreaHeight + Dimens.PaddingSmall.toPx())
                    )
                }
            }
        }

        selectedIndex?.let { index ->
            if (index in entries.indices) {
                val point = points[index]
                val entry = entries[index]
                val valueText = yAxisLabelFormatter(entry.value)

                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(valueText),
                    style = textStyle
                )

                val textTopLeft = Offset(
                    x = point.x - textLayoutResult.size.width / 2,
                    y = (point.y - textLayoutResult.size.height - Dimens.PaddingMedium.toPx()).coerceAtLeast(0f)
                )

                val bgRect = Rect(
                    left = textTopLeft.x - 8.dp.toPx(),
                    top = textTopLeft.y - 4.dp.toPx(),
                    right = textTopLeft.x + textLayoutResult.size.width + 8.dp.toPx(),
                    bottom = textTopLeft.y + textLayoutResult.size.height + 4.dp.toPx()
                )

                drawRoundRect(
                    color = lineColor,
                    topLeft = bgRect.topLeft,
                    size = bgRect.size,
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                drawText(textLayoutResult, topLeft = textTopLeft)
            }
        }
    }
}

private fun calculateNiceMax(maxVal: Float): Float {
    if (maxVal <= 0) return 4f
    val ticks = 4
    val rawSpacing = maxVal / ticks
    val magnitude = 10.0.pow(floor(log10(rawSpacing.toDouble()))).toFloat()
    val residual = rawSpacing / magnitude

    val niceResidual = when {
        residual < 1.5f -> 1f
        residual < 3.0f -> 2f
        residual < 7.0f -> 5f
        else -> 10f
    }

    val niceSpacing = niceResidual * magnitude
    return niceSpacing * ticks
}