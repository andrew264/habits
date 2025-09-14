package com.andrew264.habits.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.navigation.sharedAxisXEnter
import com.andrew264.habits.ui.navigation.sharedAxisXExit
import com.andrew264.habits.ui.schedule.components.SchedulesListPane
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SchedulesListDetailScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateUp: () -> Unit,
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

    SchedulesListDetailScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onDeleteSchedule = viewModel::onDeleteSchedule,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SchedulesListDetailScreen(
    uiState: SchedulesUiState,
    onNavigateUp: () -> Unit,
    onDeleteSchedule: suspend (Schedule) -> Boolean,
    snackbarHostState: SnackbarHostState
) {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<ScheduleSelection>(
        adaptStrategies =
            ListDetailPaneScaffoldDefaults.adaptStrategies(
                extraPaneAdaptStrategy =
                    AdaptStrategy.Reflow(reflowUnder = ListDetailPaneScaffoldRole.Detail)
            )
    )
    val scope = rememberCoroutineScope()
    val selection = scaffoldNavigator.currentDestination?.contentKey

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        listPane = {
            AnimatedPane(
                enterTransition = sharedAxisXEnter(forward = false),
                exitTransition = sharedAxisXExit(forward = true)
            ) {
                SchedulesListPane(
                    uiState = uiState,
                    onDelete = onDeleteSchedule,
                    onEdit = { scheduleId ->
                        scope.launch {
                            scaffoldNavigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = ScheduleSelection(scheduleId)
                            )
                        }
                    },
                    onNavigateUp = onNavigateUp,
                    onNewSchedule = {
                        scope.launch {
                            scaffoldNavigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = ScheduleSelection(scheduleId = UUID.randomUUID().toString())
                            )
                        }
                    },
                    isDetailPaneVisible = selection != null,
                    snackbarHostState = snackbarHostState
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