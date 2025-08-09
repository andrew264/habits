package com.andrew264.habits.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.navigation.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainTopAppBar(
    topLevelBackStack: TopLevelBackStack,
    onWaterReminderClick: () -> Unit
) {
    val currentRoute = topLevelBackStack.backStack.lastOrNull() ?: return

    val topLevelScreen = railItems.find { it == currentRoute }
    val isTopLevelScreen = topLevelScreen != null

    val title: String = topLevelScreen?.title ?: when (currentRoute) {
        is Whitelist -> "Manage Whitelist"
        is WaterStats -> "Hydration Statistics"
        is Privacy -> "Delete Data"
        else -> ""
    }

    val navigationIcon: @Composable () -> Unit = if (!isTopLevelScreen) {
        {
            val interactionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(interactionSource)
            IconButton(
                onClick = { topLevelBackStack.removeLast() },
                interactionSource = interactionSource
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    } else {
        {}
    }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = navigationIcon,
        actions = {
            if (currentRoute is Water) {
                val statsInteractionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(statsInteractionSource)
                IconButton(
                    onClick = { topLevelBackStack.add(WaterStats) },
                    interactionSource = statsInteractionSource
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = "Hydration Statistics")
                }

                val reminderInteractionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(reminderInteractionSource)
                IconButton(
                    onClick = onWaterReminderClick,
                    interactionSource = reminderInteractionSource
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = "Reminder Settings")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun MainTopAppBarPreview() {
    val topLevelBackStack = TopLevelBackStack(Home)
    topLevelBackStack.add(Water)
    MainTopAppBar(
        topLevelBackStack = topLevelBackStack,
        onWaterReminderClick = {}
    )
}