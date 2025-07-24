package com.andrew264.habits.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavigationRail(
    topLevelBackStack: TopLevelBackStack,
    state: WideNavigationRailState,
    isCompact: Boolean,
    scope: CoroutineScope
) {
    if (isCompact) {
        ModalWideNavigationRail(
            state = state,
            hideOnCollapse = true
        ) {
            AppNavigationRailContent(
                topLevelBackStack = topLevelBackStack,
                railState = state,
                isRailExpanded = true,
                isCompact = true,
                scope = scope
            )
        }
    } else {
        WideNavigationRail(
            state = state,
            header = {
                val expanded = state.targetValue == WideNavigationRailValue.Expanded
                val interactionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(interactionSource)
                IconButton(
                    onClick = {
                        scope.launch { if (expanded) state.collapse() else state.expand() }
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier.padding(start = Dimens.PaddingExtraLarge),
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Crossfade(targetState = expanded, label = "MenuIconCrossfade") { isExpanded ->
                        if (isExpanded) {
                            Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = "Collapse rail")
                        } else {
                            Icon(Icons.Filled.Menu, contentDescription = "Expand rail")
                        }
                    }
                }
            }
        ) {
            val expanded = state.targetValue == WideNavigationRailValue.Expanded
            AppNavigationRailContent(
                topLevelBackStack = topLevelBackStack,
                railState = state,
                isRailExpanded = expanded,
                isCompact = false,
                scope = scope
            )
        }
    }
}

@Composable
private fun AppNavigationRailContent(
    topLevelBackStack: TopLevelBackStack,
    railState: WideNavigationRailState,
    isRailExpanded: Boolean,
    isCompact: Boolean,
    scope: CoroutineScope
) {
    val currentTopLevelRoute = topLevelBackStack.currentTopLevelRoute

    Column(Modifier.verticalScroll(rememberScrollState())) {
        railItems.forEach { screen ->
            val selected = currentTopLevelRoute == screen
            val interactionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(interactionSource)
            WideNavigationRailItem(
                railExpanded = isRailExpanded,
                icon = {
                    Icon(
                        if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = selected,
                onClick = {
                    if (topLevelBackStack.currentTopLevelRoute != screen) {
                        topLevelBackStack.switchTopLevel(screen)
                    }
                    if (isCompact) {
                        scope.launch { railState.collapse() }
                    }
                },
                interactionSource = interactionSource
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun AppNavigationRailPreview() {
    val topLevelBackStack = remember { TopLevelBackStack(Home) }
    val state = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    AppNavigationRail(
        topLevelBackStack = topLevelBackStack,
        state = state,
        isCompact = false,
        scope = scope
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun AppNavigationRailContentPreview() {
    val topLevelBackStack = remember { TopLevelBackStack(Home) }
    val railState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    HabitsTheme {
        Surface {
            AppNavigationRailContent(
                topLevelBackStack = topLevelBackStack,
                railState = railState,
                isRailExpanded = true,
                isCompact = false,
                scope = scope
            )
        }
    }
}