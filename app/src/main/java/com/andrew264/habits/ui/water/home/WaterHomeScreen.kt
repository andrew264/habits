package com.andrew264.habits.ui.water.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WaterHomeScreen(
    navController: NavController,
    viewModel: WaterHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.isEnabled) {
        FeatureDisabledContent(
            onEnableClicked = { navController.navigate("water_settings") }
        )
    } else {
        WaterTrackingContent(
            uiState = uiState,
            onLogWater = viewModel::logWater
        )
    }
}

@Composable
private fun WaterTrackingContent(
    modifier: Modifier = Modifier,
    uiState: WaterHomeUiState,
    onLogWater: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(250f) }
    val animatedProgress by animateFloatAsState(targetValue = uiState.progress, label = "WaterProgressAnimation")
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Progress Section ---
        item {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(200.dp),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.todaysIntakeMl} ml",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of ${uiState.dailyTargetMl} ml",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Input Section ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Log Intake: ${sliderValue.roundToInt()} ml",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 50f..1000f,
                        steps = (1000 / 50) - 2 // 50ml increments
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        Button(onClick = { sliderValue = 250f }) { Text("250ml") }
                        Button(onClick = { sliderValue = 500f }) { Text("500ml") }
                        Button(onClick = { sliderValue = 700f }) { Text("750ml") }
                    }
                    Button(
                        onClick = { onLogWater(sliderValue.roundToInt()) },
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
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Water Tracking is Disabled",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enable this feature in the settings to start tracking your daily water intake.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onEnableClicked) {
            Text("Go to Settings")
        }
    }
}