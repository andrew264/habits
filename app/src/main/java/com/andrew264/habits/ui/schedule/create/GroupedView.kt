package com.andrew264.habits.ui.schedule.create

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupedView(
    schedule: Schedule,
    modifier: Modifier = Modifier,
    onUpdateGroupName: (groupId: String, newName: String) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onToggleDayInGroup: (groupId: String, day: DayOfWeek) -> Unit,
    onAddTimeRangeToGroup: (groupId: String, timeRange: TimeRange) -> Unit,
    onUpdateTimeRangeInGroup: (groupId: String, updatedTimeRange: TimeRange) -> Unit,
    onDeleteTimeRangeFromGroup: (groupId: String, timeRange: TimeRange) -> Unit,
) {
    val view = LocalView.current

    if (schedule.groups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(Dimens.PaddingLarge),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No Groups Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Tap the 'New Group' button below to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
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
                        // Header Section
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

                            IconButton(
                                onClick = {
                                    onDeleteGroup(group.id)
                                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                },
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Group",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Day Selector
                        DaySelector(
                            selectedDays = group.days,
                            onDayClick = { day -> onToggleDayInGroup(group.id, day) }
                        )

                        // Time Ranges Section
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

                        // Add Time Button
                        FilledTonalButton(
                            onClick = {
                                onAddTimeRangeToGroup(group.id, TimeRange(fromMinuteOfDay = 540, toMinuteOfDay = 600))
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            },
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
                            TimeRange(fromMinuteOfDay = 480, toMinuteOfDay = 540), // 8:00 - 9:00
                            TimeRange(fromMinuteOfDay = 600, toMinuteOfDay = 660)  // 10:00 - 11:00
                        )
                    ),
                    ScheduleGroup(
                        id = "group2",
                        name = "Weekends",
                        days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        timeRanges = listOf(
                            TimeRange(fromMinuteOfDay = 720, toMinuteOfDay = 780) // 12:00 - 13:00
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