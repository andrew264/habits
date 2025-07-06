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
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupedView(
    schedule: Schedule,
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
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
                                onValueChange = { newName -> viewModel.updateGroupName(group.id, newName) },
                                label = { Text("Group Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            IconButton(
                                onClick = {
                                    viewModel.deleteGroup(group.id)
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
                            onDayClick = { day -> viewModel.toggleDayInGroup(group.id, day) }
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
                                                viewModel.deleteTimeRangeFromGroup(group.id, timeRange)
                                            },
                                            onUpdate = { newTimeRange ->
                                                viewModel.updateTimeRangeInGroup(group.id, newTimeRange)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Add Time Button
                        FilledTonalButton(
                            onClick = {
                                viewModel.addTimeRangeToGroup(group.id, TimeRange(fromMinuteOfDay = 540, toMinuteOfDay = 600))
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