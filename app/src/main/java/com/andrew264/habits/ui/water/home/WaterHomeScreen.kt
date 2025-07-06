package com.andrew264.habits.ui.water.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WaterTrackingContent(
    modifier: Modifier = Modifier,
    uiState: WaterHomeUiState,
    onLogWater: (Int) -> Unit,
    onEditTarget: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(250f) }
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        label = "WaterProgressAnimation",
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec
    )
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val view = LocalView.current

    val strokeWidth = 6.dp
    val stroke = with(LocalDensity.current) {
        remember { Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        // --- Progress Section ---
        item {
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(200.dp),
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
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraSmall)
                    ) {
                        Text(
                            text = "of ${uiState.settings.waterDailyTargetMl} ml",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null, // Click action is on the parent Column
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- Input Section ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(Dimens.PaddingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                ) {
                    Text(
                        "Log Intake: ${sliderValue.roundToInt()} ml",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            val stepSize = 50f
                            val currentStep = (newValue / stepSize).roundToInt()
                            val previousStep = (sliderValue / stepSize).roundToInt()

                            if (currentStep != previousStep) {
                                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                            }
                            sliderValue = newValue
                        },
                        valueRange = 50f..1000f,
                        steps = (1000 / 50) - 2 // 50ml increments
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall, Alignment.CenterHorizontally)
                    ) {
                        Button(onClick = {
                            sliderValue = 250f
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }) { Text("250ml") }
                        Button(onClick = {
                            sliderValue = 500f
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }) { Text("500ml") }
                        Button(onClick = {
                            sliderValue = 750f
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }) { Text("750ml") }
                    }
                    Button(
                        onClick = {
                            onLogWater(sliderValue.roundToInt())
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = sliderValue.roundToInt() > 0
                    ) {
                        Text("Log ${sliderValue.roundToInt()} ml")
                    }
                }
            }
        }

        // --- Log Section ---
        if (uiState.todaysLog.isNotEmpty()) {
            item {
                Text(
                    "Today's Log",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(uiState.todaysLog, key = { it.id }) { entry ->
                ListItem(
                    headlineContent = { Text("${entry.amountMl} ml") },
                    trailingContent = { Text(timeFormat.format(Date(entry.timestamp))) }
                )
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