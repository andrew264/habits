package com.andrew264.habits.ui.water.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterTopAppBar(
    onNavigateToStats: () -> Unit,
    onWaterReminderClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Water") },
        actions = {
            val statsInteractionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(statsInteractionSource)
            IconButton(
                onClick = onNavigateToStats,
                interactionSource = statsInteractionSource,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.BarChart, contentDescription = "Hydration Statistics")
            }

            val reminderInteractionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(reminderInteractionSource)
            IconButton(
                onClick = onWaterReminderClick,
                interactionSource = reminderInteractionSource,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Alarm, contentDescription = "Reminder Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun WaterTopAppBarPreview() {
    WaterTopAppBar(
        onNavigateToStats = {},
        onWaterReminderClick = {}
    )
}