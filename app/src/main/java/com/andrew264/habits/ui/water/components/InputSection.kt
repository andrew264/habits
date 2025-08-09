package com.andrew264.habits.ui.water.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun InputSection(
    onLogWater: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderState = rememberSliderState(
        value = 250f,
        valueRange = 50f..1000f,
        steps = (1000 / 50) - 2 // 50ml increments
    )
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(sliderState) {
        snapshotFlow { sliderState.value }
            .map { (it / 50f).roundToInt() }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
            }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraLarge)
    ) {
        Slider(
            state = sliderState,
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = sliderInteractionSource,
                    thumbSize = DpSize(4.dp, 144.dp)
                )
            },
            track = { currentSliderState ->
                val activeTrackColor = MaterialTheme.colorScheme.primary
                val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                val activeTextColor = MaterialTheme.colorScheme.onPrimary
                val inactiveTextColor = MaterialTheme.colorScheme.primary

                val activeTextStyle = MaterialTheme.typography.displaySmall.copy(
                    color = activeTextColor,
                    fontWeight = FontWeight.Bold
                )
                val inactiveTextStyle = MaterialTheme.typography.displaySmall.copy(
                    color = inactiveTextColor,
                    fontWeight = FontWeight.Bold
                )

                SliderDefaults.Track(
                    sliderState = currentSliderState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .drawWithContent {
                            val sliderValue = currentSliderState.value
                            val valueText = "${sliderValue.roundToInt()} ml"

                            // Draw the track first
                            drawContent()

                            // Calculate track widths
                            val activeTrackEnd = size.width * currentSliderState.coercedValueAsFraction
                            val activeTrackWidth = activeTrackEnd
                            val inactiveTrackWidth = size.width - activeTrackEnd

                            // Text positioning padding
                            val textPadding = Dimens.PaddingExtraExtraLarge.toPx()

                            // Try to place text on the inactive side first
                            val measuredText = textMeasurer.measure(
                                text = valueText,
                                style = inactiveTextStyle
                            )

                            val thumbWidthPx = 4.dp.toPx()
                            val halfThumbWidth = thumbWidthPx / 2
                            val textWidth = measuredText.size.width.toFloat()
                            val canFitOnInactiveSide = textWidth + textPadding * 2 < inactiveTrackWidth - halfThumbWidth

                            if (canFitOnInactiveSide) {
                                val y = (size.height - measuredText.size.height) / 2
                                val x = activeTrackEnd + halfThumbWidth + textPadding
                                drawText(measuredText, topLeft = Offset(x, y))
                            } else {
                                // Otherwise, place it on the active side
                                val measuredTextOnActive = textMeasurer.measure(
                                    text = valueText,
                                    style = activeTextStyle
                                )
                                val textWidthOnActive =
                                    measuredTextOnActive.size.width.toFloat()
                                val canFitOnActiveSide =
                                    textWidthOnActive + textPadding * 2 < activeTrackWidth - halfThumbWidth

                                if (canFitOnActiveSide) {
                                    val y =
                                        (size.height - measuredTextOnActive.size.height) / 2
                                    val x =
                                        activeTrackEnd - halfThumbWidth - textWidthOnActive - textPadding
                                    drawText(measuredTextOnActive, topLeft = Offset(x, y))
                                }
                            }
                        },
                    colors = SliderDefaults.colors(
                        activeTrackColor = activeTrackColor,
                        inactiveTrackColor = inactiveTrackColor,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    trackCornerSize = 16.dp,
                )
            }
        )

        ElevatedButton(
            onClick = {
                onLogWater(sliderState.value.roundToInt())
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            enabled = sliderState.value.roundToInt() > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Icon(
                Icons.Filled.WaterDrop,
                contentDescription = "Log Water",
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Drink", style = MaterialTheme.typography.headlineMediumEmphasized)
        }
    }
}

@Preview
@Composable
internal fun InputSectionPreview() {
    InputSection(onLogWater = {})
}