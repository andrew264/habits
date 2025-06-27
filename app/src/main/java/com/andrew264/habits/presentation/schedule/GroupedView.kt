package com.andrew264.habits.presentation.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.TimeRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupedView(
    schedule: Schedule,
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(schedule.groups, key = { it.id }) { group ->

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = group.name,
                            onValueChange = { newName -> viewModel.updateGroupName(group.id, newName) },
                            label = { Text("Group Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = { viewModel.deleteGroup(group.id) },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Group",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${group.days.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = "Days",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${group.timeRanges.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = "Times",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    // Day Selector
                    DaySelector(
                        selectedDays = group.days,
                        onDayClick = { day -> viewModel.toggleDayInGroup(group.id, day) }
                    )

                    // Time Ranges Section
                    if (group.timeRanges.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Time Ranges",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            group.timeRanges.forEach { timeRange ->
                                TimeRangeRow(
                                    timeRange = timeRange,
                                    onDelete = { viewModel.deleteTimeRangeFromGroup(group.id, timeRange) },
                                    onUpdate = { newTimeRange ->
                                        viewModel.updateTimeRangeInGroup(
                                            group.id,
                                            timeRange,
                                            newTimeRange
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Add Time Button
                    FilledTonalButton(
                        onClick = { viewModel.addTimeRangeToGroup(group.id, TimeRange(540, 600)) },
                        modifier = Modifier.align(Alignment.End),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add Time Range")
                    }
                }
            }
        }

        // Add Group Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { viewModel.addGroup() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentPadding = PaddingValues(16.dp),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Create New Group",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}