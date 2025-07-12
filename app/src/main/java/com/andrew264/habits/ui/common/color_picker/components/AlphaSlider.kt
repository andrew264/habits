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
import com.andrew264.habits.ui.common.color_picker.HsvColor
import com.andrew264.habits.ui.common.color_picker.utils.toColor
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.launch

@Composable
internal fun AlphaSlider(
    modifier: Modifier = Modifier,
    hsvColor: HsvColor,
    onAlphaChange: (Float) -> Unit,
    onInteractionEnd: () -> Unit
) {
    DrawAlphaSlider(
        modifier = modifier,
        hsvColor = hsvColor,
        onAlphaChange = onAlphaChange,
        onInteractionEnd = onInteractionEnd
    )
}

@Preview(widthDp = 300, heightDp = 40)
@Composable
internal fun AlphaSliderPreview() {
    HabitsTheme {
        var hsv by remember { mutableStateOf(HsvColor(0f, 1f, 1f, 0.5f)) }
        AlphaSlider(
            hsvColor = hsv,
            onAlphaChange = {},
            onInteractionEnd = {}
        )
    }
}

@Composable
fun DrawAlphaSlider(
    modifier: Modifier,
    hsvColor: HsvColor,
    onAlphaChange: (Float) -> Unit,
    onInteractionEnd: () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val color = hsvColor.copy(alpha = 1f).toColor()

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val alpha = (offset.x / size.width).coerceIn(0f, 1f)
                    onAlphaChange(alpha)
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
                    val alpha = (change.position.x / size.width).coerceIn(0f, 1f)
                    coroutineScope.launch {
                        onAlphaChange(alpha)
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                }
            }
    ) {
        drawCheckerboard()
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, color)))
        drawHorizontalSliderHandle(
            x = size.width * hsvColor.alpha,
            handleColor = color.copy(alpha = hsvColor.alpha)
        )
    }
}