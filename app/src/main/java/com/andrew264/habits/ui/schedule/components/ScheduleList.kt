package com.andrew264.habits.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.theme.Dimens

@Composable
internal fun ScheduleList(
    modifier: Modifier = Modifier,
    schedules: List<Schedule>,
    listState: LazyListState = rememberLazyListState(),
    pendingDeletionId: String?,
    onDelete: suspend (Schedule) -> Boolean,
    onEdit: (scheduleId: String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
        contentPadding = PaddingValues(top = Dimens.PaddingLarge, bottom = 88.dp)
    ) {
        items(schedules, key = { it.id }) { schedule ->
            ScheduleListItem(
                schedule = schedule,
                isPendingDeletion = schedule.id == pendingDeletionId,
                onDelete = { onDelete(schedule) },
                onEdit = { onEdit(schedule.id) }
            )
        }
    }
}