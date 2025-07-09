package com.andrew264.habits.ui.common.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * A simple composable that draws a Drawable
 */
@Composable
fun DrawableImage(
    drawable: Drawable?,
    contentDescription: String?, // Included for accessibility, though not directly used in Canvas
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (drawable != null) {
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawIntoCanvas { canvas ->
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}