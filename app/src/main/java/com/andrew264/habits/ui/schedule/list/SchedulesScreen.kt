package com.andrew264.habits.ui.schedule.list

import android.view.HapticFeedbackConstants
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val schedules by viewModel.schedules.collectAsState()
    val view = LocalView.current

    LaunchedEffect(key1 = viewModel.uiEvents, snackbarHostState) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is SchedulesUiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        viewModel.onUndoDelete()
                    }
                }
            }
        }
    }

    if (schedules.isEmpty()) {
        EmptyState()
    } else {
        ScheduleList(
            schedules = schedules,
            onDelete = { viewModel.onDeleteSchedule(it) },
            onEdit = { scheduleId ->
                navController.navigate("schedule_editor?scheduleId=$scheduleId")
            }
        )
    }
}