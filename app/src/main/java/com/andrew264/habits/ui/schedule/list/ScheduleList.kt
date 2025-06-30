package com.andrew264.habits.ui.schedule.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.Schedule

@Composable
internal fun ScheduleList(
    schedules: List<Schedule>,
    onDelete: (Schedule) -> Unit,
    onEdit: (scheduleId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        items(schedules, key = { it.id }) { schedule ->
            ScheduleListItem(
                schedule = schedule,
                onDelete = { onDelete(schedule) },
                onEdit = { onEdit(schedule.id) }
            )
        }
    }
}