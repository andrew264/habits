package com.andrew264.habits.ui.schedule

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.navigation.sharedAxisXEnter
import com.andrew264.habits.ui.navigation.sharedAxisXExit
import com.andrew264.habits.ui.schedule.components.SchedulesListPane
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SchedulesListDetailScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SchedulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is SchedulesUiEvent.ShowSnackbar -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onUndoDelete()
                        } else if (event.actionLabel != null) {
                            viewModel.onDeletionConfirmed()
                        }
                    }
                }
            }
        }
    }

    SchedulesListDetailScreenContent(
        uiState = uiState,
        onDeleteSchedule = viewModel::onDeleteSchedule,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SchedulesListDetailScreenContent(
    uiState: SchedulesUiState,
    onDeleteSchedule: suspend (Schedule) -> Boolean,
    snackbarHostState: SnackbarHostState
) {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<ScheduleSelection>()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val expandedFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    val selection = scaffoldNavigator.currentDestination?.contentKey

    Scaffold(
        floatingActionButton = {
            if (selection == null) {
                val fabInteractionSource = remember { MutableInteractionSource() }
                val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                HapticInteractionEffect(fabInteractionSource)
                SmallExtendedFloatingActionButton(
                    text = { Text(text = "New Schedule") },
                    icon = { Icon(Icons.Filled.Add, "New Schedule") },
                    onClick = {
                        scope.launch {
                            scaffoldNavigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = ScheduleSelection(scheduleId = UUID.randomUUID().toString())
                            )
                        }
                    },
                    modifier = Modifier.padding(bottom = navBarPadding + Dimens.PaddingMedium),
                    expanded = expandedFab,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    interactionSource = fabInteractionSource,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        NavigableListDetailPaneScaffold(
            modifier = Modifier.padding(paddingValues),
            navigator = scaffoldNavigator,
            listPane = {
                AnimatedPane(
                    enterTransition = sharedAxisXEnter(forward = false),
                    exitTransition = sharedAxisXExit(forward = true)
                ) {
                    SchedulesListPane(
                        uiState = uiState,
                        listState = listState,
                        onDelete = onDeleteSchedule,
                        onEdit = { scheduleId ->
                            scope.launch {
                                scaffoldNavigator.navigateTo(
                                    pane = ListDetailPaneScaffoldRole.Detail,
                                    contentKey = ScheduleSelection(scheduleId)
                                )
                            }
                        }
                    )
                }
            },
            detailPane = {
                AnimatedPane(
                    enterTransition = sharedAxisXEnter(forward = true),
                    exitTransition = sharedAxisXExit(forward = false)
                ) {
                    if (selection != null) {
                        ScheduleEditorScreen(
                            scheduleId = selection.scheduleId,
                            snackbarHostState = snackbarHostState,
                            onNavigateUp = { scope.launch { scaffoldNavigator.navigateBack() } }
                        )
                    } else {
                        DetailPanePlaceholder()
                    }
                }
            }
        )
    }
}

@Composable
private fun DetailPanePlaceholder() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(Dimens.PaddingLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Select a schedule to edit, or create a new one.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}