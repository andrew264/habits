package com.andrew264.habits.ui.schedule.list

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.ScheduleEditor
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SchedulesScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SchedulesViewModel = hiltViewModel(),
    onNavigate: (AppRoute) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is SchedulesUiEvent.ShowSnackbar -> {
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

    Scaffold(
        floatingActionButton = {
            SmallExtendedFloatingActionButton(
                onClick = {
                    onNavigate(ScheduleEditor(scheduleId = null))
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "New Schedule") },
                text = { Text("New Schedule") }
            )
        }
    ) { innerPadding ->
        val listToShow = uiState.schedules
        val pendingDeletionId = uiState.schedulePendingDeletion?.id
        val contentModifier = Modifier.padding(innerPadding)

        when {
            uiState.isLoading -> {
                Box(
                    modifier = contentModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            listToShow.isEmpty() -> {
                EmptyState(modifier = contentModifier)
            }

            else -> {
                ScheduleList(
                    modifier = contentModifier,
                    schedules = listToShow,
                    pendingDeletionId = pendingDeletionId,
                    onDelete = viewModel::onDeleteSchedule,
                    onEdit = { scheduleId ->
                        onNavigate(ScheduleEditor(scheduleId = scheduleId))
                    }
                )
            }
        }
    }
}