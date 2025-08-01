package com.andrew264.habits.ui.common.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.graphics.shapes.RoundedPolygon

/**
 * A simple composable that draws a Drawable, with an optional shape mask.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DrawableImage(
    drawable: Drawable?,
    contentDescription: String?, // Included for accessibility, though not directly used in Canvas
    modifier: Modifier = Modifier,
    mask: RoundedPolygon = MaterialShapes.Circle
) {
    Canvas(modifier = modifier.clip(mask.toShape())) {
        if (drawable != null) {
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawIntoCanvas { canvas ->
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}