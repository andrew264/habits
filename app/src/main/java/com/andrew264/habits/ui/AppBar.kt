package com.andrew264.habits.ui

import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.navigation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainTopAppBar(
    topLevelBackStack: TopLevelBackStack,
    railState: WideNavigationRailState,
    isCompact: Boolean,
    scope: CoroutineScope,
    onWaterReminderClick: () -> Unit
) {
    val currentRoute = topLevelBackStack.backStack.lastOrNull() ?: return
    val view = LocalView.current

    val topLevelScreen = railItems.find { it == currentRoute }
    val isTopLevelScreen = topLevelScreen != null

    val title: String = topLevelScreen?.title ?: when (currentRoute) {
        is Whitelist -> "Manage Whitelist"
        is WaterStats -> "Hydration Statistics"
        else -> ""
    }

    val navigationIcon: @Composable () -> Unit = if (!isTopLevelScreen) {
        {
            IconButton(onClick = {
                topLevelBackStack.removeLast()
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    } else if (isCompact) {
        {
            IconButton(
                onClick = {
                    scope.launch { railState.expand() }
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Open Navigation")
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
                IconButton(onClick = {
                    topLevelBackStack.add(WaterStats)
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }) {
                    Icon(Icons.Default.BarChart, contentDescription = "Hydration Statistics")
                }
                IconButton(onClick = {
                    onWaterReminderClick()
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }) {
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
    val railState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    MainTopAppBar(
        topLevelBackStack = topLevelBackStack,
        railState = railState,
        isCompact = true,
        scope = scope,
        onWaterReminderClick = {}
    )
}