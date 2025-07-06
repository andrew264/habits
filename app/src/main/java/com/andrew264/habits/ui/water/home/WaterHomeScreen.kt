package com.andrew264.habits.ui.water.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

@Composable
fun WaterHomeScreen(
    viewModel: WaterHomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val showTargetDialog by viewModel.showTargetDialog.collectAsState()
    val showReminderDialog by viewModel.showReminderDialog.collectAsState()

    if (showTargetDialog) {
        TargetSettingsDialog(
            settings = uiState.settings,
            onDismiss = viewModel::onDismissTargetDialog,
            onSave = viewModel::saveTargetSettings
        )
    }

    if (showReminderDialog) {
        ReminderSettingsDialog(
            settings = uiState.settings,
            allSchedules = uiState.allSchedules,
            onDismiss = viewModel::onDismissReminderDialog,
            onSave = viewModel::saveReminderSettings
        )
    }

    if (!uiState.settings.isWaterTrackingEnabled) {
        FeatureDisabledContent(
            onEnableClicked = viewModel::onShowTargetDialog
        )
    } else {
        WaterTrackingContent(
            uiState = uiState,
            onLogWater = viewModel::logWater,
            onEditTarget = viewModel::onShowTargetDialog
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
private fun WaterTrackingContent(
    modifier: Modifier = Modifier,
    uiState: WaterHomeUiState,
    onLogWater: (Int) -> Unit,
    onEditTarget: () -> Unit
) {
    val sliderState = rememberSliderState(
        value = 250f,
        valueRange = 50f..1000f,
        steps = (1000 / 50) - 2 // 50ml increments
    )
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        label = "WaterProgressAnimation",
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec
    )
    val view = LocalView.current
    val textMeasurer = rememberTextMeasurer()

    val strokeWidth = 8.dp
    val stroke = with(LocalDensity.current) {
        remember { Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round) }
    }

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
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- Progress Section ---
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(384.dp),
                stroke = stroke,
                trackStroke = stroke,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        onEditTarget()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    .padding(Dimens.PaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${uiState.todaysIntakeMl} ml",
                    style = MaterialTheme.typography.displayLargeEmphasized
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraSmall)
                ) {
                    Text(
                        text = "of ${uiState.settings.waterDailyTargetMl} ml",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null, // Click action is on the parent Column
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Input Section ---
        Column(
            modifier = Modifier.padding(bottom = Dimens.PaddingExtraLarge),
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
            Button(
                onClick = {
                    onLogWater(sliderState.value.roundToInt())
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = sliderState.value.roundToInt() > 0,
                shape = MaterialTheme.shapes.large,
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
                Text("Drink", style = MaterialTheme.typography.titleLargeEmphasized)
            }
        }
    }
}


@Composable
private fun FeatureDisabledContent(
    modifier: Modifier = Modifier,
    onEnableClicked: () -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Text(
            text = "Water Tracking is Disabled",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Text(
            text = "Enable this feature to start tracking your daily water intake.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimens.PaddingExtraLarge))
        Button(onClick = {
            onEnableClicked()
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }) {
            Text("Enable Water Tracking")
        }
    }
}