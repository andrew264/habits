package com.andrew264.habits.ui.schedule

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.ui.navigation.sharedAxisXEnter
import com.andrew264.habits.ui.navigation.sharedAxisXExit
import com.andrew264.habits.ui.schedule.components.EmptyState
import com.andrew264.habits.ui.schedule.components.ScheduleEditorContent
import com.andrew264.habits.ui.schedule.components.ScheduleList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SchedulesListDetailScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SchedulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<ScheduleSelection>()
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val selection = scaffoldNavigator.currentDestination?.contentKey

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is SchedulesUiEvent.ShowSnackbar -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            viewModel.onUndoDelete()
                        } else if (event.actionLabel != null) {
                            viewModel.onDeletionConfirmed()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selection == null) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            scaffoldNavigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = ScheduleSelection(scheduleId = null)
                            )
                        }
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Schedule")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        NavigableListDetailPaneScaffold(
            modifier = Modifier.padding(paddingValues),
            navigator = scaffoldNavigator,
            listPane = {
                AnimatedPane(
                    enterTransition = sharedAxisXEnter(forward = false),
                    exitTransition = sharedAxisXExit(forward = true)
                ) {
                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.schedules.isEmpty() && uiState.schedulePendingDeletion == null) {
                        EmptyState()
                    } else {
                        ScheduleList(
                            schedules = uiState.schedules,
                            pendingDeletionId = uiState.schedulePendingDeletion?.id,
                            onDelete = viewModel::onDeleteSchedule,
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
                }
            },
            detailPane = {
                AnimatedPane(
                    enterTransition = sharedAxisXEnter(forward = false),
                    exitTransition = sharedAxisXExit(forward = true)
                ) {
                    if (selection != null) {
                        ScheduleEditorContent(
                            scheduleId = selection.scheduleId,
                            snackbarHostState = snackbarHostState,
                            onNavigateUp = { scope.launch { scaffoldNavigator.navigateBack() } }
                        )
                    } else {
                        // Placeholder when no item is selected on large screens
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Select a schedule to edit, or create a new one.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )
    }
}