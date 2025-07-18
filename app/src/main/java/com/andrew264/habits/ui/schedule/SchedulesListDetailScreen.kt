package com.andrew264.habits.ui.schedule

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.ui.navigation.sharedAxisXEnter
import com.andrew264.habits.ui.navigation.sharedAxisXExit
import com.andrew264.habits.ui.schedule.components.EmptyState
import com.andrew264.habits.ui.schedule.components.ScheduleEditorContent
import com.andrew264.habits.ui.schedule.components.ScheduleList
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.launch
import java.util.UUID


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

    val listState = rememberLazyListState()
    val expandedFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    val selection = scaffoldNavigator.currentDestination?.contentKey

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { event ->
            // TODO: snackbar shows up twice (sometimes ?), what is up with another showSnackbar in ScheduleEditorContent ??
            when (event) {
                is SchedulesUiEvent.ShowSnackbar -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short
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
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            scaffoldNavigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = ScheduleSelection(scheduleId = UUID.randomUUID().toString())
                            )
                        }
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    expanded = expandedFab,
                    icon = { Icon(Icons.Filled.Add, "New Schedule") },
                    text = { Text(text = "New Schedule") },
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
                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.schedules.isEmpty() && uiState.schedulePendingDeletion == null) {
                        EmptyState()
                    } else {
                        ScheduleList(
                            schedules = uiState.schedules,
                            listState = listState,
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
                // TODO: in here when in a foldable, when the selection is not null and back button/gesture is done, make the selection null first?
                AnimatedPane(
                    enterTransition = sharedAxisXEnter(forward = true),
                    exitTransition = sharedAxisXExit(forward = false)
                ) {
                    if (selection != null) {
                        ScheduleEditorContent(
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