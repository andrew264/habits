package com.andrew264.habits.ui.schedule.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupedView(
    schedule: Schedule,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onUpdateGroupName: (groupId: String, newName: String) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onToggleDayInGroup: (groupId: String, day: DayOfWeek) -> Unit,
    onAddTimeRangeToGroup: (groupId: String, timeRange: TimeRange) -> Unit,
    onUpdateTimeRangeInGroup: (groupId: String, updatedTimeRange: TimeRange) -> Unit,
    onDeleteTimeRangeFromGroup: (groupId: String, timeRange: TimeRange) -> Unit,
) {
    val view = LocalView.current

    if (schedule.groups.isEmpty()) {
        EmptyState(
            modifier = modifier,
            icon = Icons.Default.Add,
            title = "No Groups Yet",
            description = "Groups allow you to apply the same time ranges to multiple days. Tap 'New Group' in the editor to add one."
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.padding(bottom = 72.dp),
            contentPadding = PaddingValues(start = Dimens.PaddingLarge, end = Dimens.PaddingLarge, bottom = Dimens.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(schedule.groups, key = { it.id }) { group ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                        ) {
                            OutlinedTextField(
                                value = group.name,
                                onValueChange = { newName -> onUpdateGroupName(group.id, newName) },
                                label = { Text("Group Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            val deleteInteractionSource = remember { MutableInteractionSource() }
                            HapticInteractionEffect(deleteInteractionSource)
                            IconButton(
                                onClick = {
                                    onDeleteGroup(group.id)
                                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                },
                                interactionSource = deleteInteractionSource,
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Group",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }


                        DaySelector(
                            selectedDays = group.days,
                            onDayClick = { day -> onToggleDayInGroup(group.id, day) }
                        )


                        if (group.timeRanges.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(Dimens.PaddingSmall),
                                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                            ) {
                                Text(
                                    text = "Time Ranges",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                group.timeRanges.forEach { timeRange ->
                                    key(timeRange.id) {
                                        TimeRangeRow(
                                            timeRange = timeRange,
                                            onDelete = {
                                                onDeleteTimeRangeFromGroup(group.id, timeRange)
                                            },
                                            onUpdate = { newTimeRange ->
                                                onUpdateTimeRangeInGroup(group.id, newTimeRange)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        val addTimeRangeInteractionSource = remember { MutableInteractionSource() }
                        HapticInteractionEffect(addTimeRangeInteractionSource)
                        FilledTonalButton(
                            onClick = {
                                onAddTimeRangeToGroup(group.id, TimeRange(fromMinuteOfDay = 540, toMinuteOfDay = 600))
                            },
                            interactionSource = addTimeRangeInteractionSource,
                            modifier = Modifier.align(Alignment.End),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.PaddingLarge)
                            )
                            Spacer(Modifier.width(Dimens.PaddingSmall))
                            Text("Add Time Range")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun GroupedViewPreview() {
    HabitsTheme {
        GroupedView(
            schedule = Schedule(
                id = "1",
                name = "Morning Routine",
                groups = listOf(
                    ScheduleGroup(
                        id = "group1",
                        name = "Weekdays",
                        days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                        timeRanges = listOf(
                            TimeRange(fromMinuteOfDay = 480, toMinuteOfDay = 540),
                            TimeRange(fromMinuteOfDay = 600, toMinuteOfDay = 660)
                        )
                    ),
                    ScheduleGroup(
                        id = "group2",
                        name = "Weekends",
                        days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        timeRanges = listOf(
                            TimeRange(fromMinuteOfDay = 720, toMinuteOfDay = 780)
                        )
                    )
                )
            ),
            onUpdateGroupName = { _, _ -> },
            onDeleteGroup = { _ -> },
            onToggleDayInGroup = { _, _ -> },
            onAddTimeRangeToGroup = { _, _ -> },
            onUpdateTimeRangeInGroup = { _, _ -> },
            onDeleteTimeRangeFromGroup = { _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun GroupedViewEmptyPreview() {
    HabitsTheme {
        GroupedView(
            schedule = Schedule(id = "2", name = "Empty Schedule", groups = emptyList()),
            onUpdateGroupName = { _, _ -> },
            onDeleteGroup = { _ -> },
            onToggleDayInGroup = { _, _ -> },
            onAddTimeRangeToGroup = { _, _ -> },
            onUpdateTimeRangeInGroup = { _, _ -> },
            onDeleteTimeRangeFromGroup = { _, _ -> }
        )
    }
}