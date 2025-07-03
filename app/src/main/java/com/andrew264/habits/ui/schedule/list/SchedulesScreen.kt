package com.andrew264.habits.ui.schedule.list

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SchedulesScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: SchedulesViewModel = hiltViewModel()
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

    val listToShow = uiState.schedules
    val pendingDeletionId = uiState.schedulePendingDeletion?.id

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        listToShow.isEmpty() -> {
            EmptyState()
        }

        else -> {
            ScheduleList(
                schedules = listToShow,
                pendingDeletionId = pendingDeletionId,
                onDelete = { schedule ->
                    if (uiState.schedulePendingDeletion == null) {
                        viewModel.onDeleteSchedule(schedule)
                    }
                },
                onEdit = { scheduleId ->
                    navController.navigate("schedule_editor?scheduleId=$scheduleId")
                }
            )
        }
    }
}