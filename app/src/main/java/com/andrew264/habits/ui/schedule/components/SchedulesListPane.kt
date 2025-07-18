package com.andrew264.habits.ui.schedule.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.schedule.SchedulesUiState

@Composable
fun SchedulesListPane(
    modifier: Modifier = Modifier,
    uiState: SchedulesUiState,
    listState: LazyListState,
    onDelete: suspend (Schedule) -> Boolean,
    onEdit: (scheduleId: String) -> Unit
) {
    if (uiState.isLoading) {
        ContainedLoadingIndicator()
    } else if (uiState.schedules.isEmpty() && uiState.schedulePendingDeletion == null) {
        EmptyState(
            icon = Icons.Default.Schedule,
            title = "No schedules yet",
            description = "Tap the 'New Schedule' button to create one."
        )
    } else {
        ScheduleList(
            modifier = modifier,
            schedules = uiState.schedules,
            listState = listState,
            pendingDeletionId = uiState.schedulePendingDeletion?.id,
            onDelete = onDelete,
            onEdit = onEdit
        )
    }
}