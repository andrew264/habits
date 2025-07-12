package com.andrew264.habits.ui.common.color_picker.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.color_picker.HsvColor
import com.andrew264.habits.ui.common.color_picker.utils.toColor
import kotlinx.coroutines.launch

@Composable
internal fun SatValPlane(
    modifier: Modifier = Modifier,
    hsvColor: HsvColor,
    onSaturationValueChange: (Float, Float) -> Unit
) {
    DrawSaturationValuePlane(
        modifier = modifier,
        hsvColor = hsvColor,
        onSaturationValueChange = onSaturationValueChange
    )
}

@Preview
@Composable
internal fun SatValPlanePreview() {
    var hsv by remember { mutableStateOf(HsvColor(260f, 0.5f, 0.8f, 1f)) }
    SatValPlane(
        hsvColor = hsv,
        onSaturationValueChange = { s, v -> hsv = hsv.copy(saturation = s, value = v) }
    )
}


@Composable
fun DrawSaturationValuePlane(
    modifier: Modifier,
    hsvColor: HsvColor,
    onSaturationValueChange: (Float, Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSaturationValueChange(s, v)
                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                    }
                ) { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    coroutineScope.launch {
                        onSaturationValueChange(s, v)
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                }
            }
    ) {
        val planePath = Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(12f)))
        }

        clipPath(planePath) {
            val saturationGradient = Brush.horizontalGradient(
                colors = listOf(Color.White, Color.hsv(hsvColor.hue, 1f, 1f))
            )
            val valueGradient = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )
            drawRect(saturationGradient)
            drawRect(valueGradient)
        }

        drawSliderHandle(
            position = Offset(
                x = size.width * hsvColor.saturation,
                y = size.height * (1f - hsvColor.value)
            ),
            handleColor = hsvColor.toColor()
        )
    }
}