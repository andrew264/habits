package com.andrew264.habits.ui.common.color_picker.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.launch

@Composable
internal fun HueSlider(
    modifier: Modifier = Modifier,
    hue: Float,
    onHueChange: (Float) -> Unit,
    onInteractionEnd: () -> Unit
) {
    DrawHorizontalSlider(
        modifier = modifier,
        initialValue = hue / 360f,
        onValueChange = { onHueChange(it * 360f) },
        onInteractionEnd = onInteractionEnd,
        colors = listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        ),
        valueToColor = { Color.hsv(it * 360f, 1f, 1f) }
    )
}

@Preview(widthDp = 300, heightDp = 40)
@Composable
private fun HorizontalHueSliderPreview() {
    HabitsTheme {
        var hue by remember { mutableFloatStateOf(180f) }
        HueSlider(
            hue = hue,
            onHueChange = { hue = it },
            onInteractionEnd = {}
        )
    }
}

@Composable
fun DrawHorizontalSlider(
    modifier: Modifier,
    initialValue: Float,
    onValueChange: (Float) -> Unit,
    onInteractionEnd: () -> Unit,
    colors: List<Color>,
    valueToColor: (Float) -> Color
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val value = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(value)
                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                    onInteractionEnd()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                    },
                    onDragEnd = {
                        onInteractionEnd()
                    },
                    onDragCancel = {
                        onInteractionEnd()
                    }
                ) { change, _ ->
                    change.consume()
                    val value = (change.position.x / size.width).coerceIn(0f, 1f)
                    coroutineScope.launch {
                        onValueChange(value)
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                }
            }
    ) {
        drawRect(Brush.horizontalGradient(colors))
        drawHorizontalSliderHandle(
            x = size.width * initialValue,
            handleColor = valueToColor(initialValue)
        )
    }
}