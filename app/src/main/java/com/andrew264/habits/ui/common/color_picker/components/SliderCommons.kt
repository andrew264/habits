package com.andrew264.habits.ui.common.color_picker.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Draws a circular handle at a specific Offset. This is the base function for all slider handles.
 */
internal fun DrawScope.drawSliderHandle(
    position: Offset,
    handleColor: Color,
    handleRadius: Float = 12.dp.toPx(),
    strokeWidth: Float = 2.dp.toPx()
) {
    // Outer white stroke for contrast
    drawCircle(
        color = Color.White,
        radius = handleRadius,
        center = position,
        style = Stroke(width = strokeWidth)
    )

    // Inner colored circle
    drawCircle(
        color = handleColor,
        radius = handleRadius - (strokeWidth / 2),
        center = position
    )
}


/**
 * Draws a circular handle for a horizontal slider on a Canvas, centered vertically.
 */
internal fun DrawScope.drawHorizontalSliderHandle(
    x: Float,
    handleColor: Color,
    handleRadius: Float = 12.dp.toPx(),
    strokeWidth: Float = 2.dp.toPx()
) {
    val center = Offset(x, size.height / 2)
    drawSliderHandle(center, handleColor, handleRadius, strokeWidth)
}


/**
 * Draws a checkerboard pattern, typically used as a background for transparent elements.
 */
internal fun DrawScope.drawCheckerboard(squareSize: Float = 8.dp.toPx()) {
    val lightColor = Color.White
    val darkColor = Color(0xFFCBCBCB)

    val horizontalSteps = (size.width / squareSize).toInt() + 1
    val verticalSteps = (size.height / squareSize).toInt() + 1

    for (y in 0..verticalSteps) {
        for (x in 0..horizontalSteps) {
            val color = if ((x + y) % 2 == 0) lightColor else darkColor
            drawRect(
                color = color,
                topLeft = Offset(x * squareSize, y * squareSize),
                size = Size(squareSize, squareSize)
            )
        }
    }
}