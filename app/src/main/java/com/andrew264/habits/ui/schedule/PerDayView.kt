package com.andrew264.habits.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.TimeRange
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PerDayView(
    perDayRepresentation: Map<DayOfWeek, List<TimeRange>>,
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    var expandedDays by rememberSaveable { mutableStateOf(emptySet<DayOfWeek>()) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(DayOfWeek.entries.toList(), key = { it.name }) { day ->
            val timeRanges = perDayRepresentation[day] ?: emptyList()
            val isExpanded = day in expandedDays

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Column {
                    // Day Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedDays = if (isExpanded) {
                                    expandedDays - day
                                } else {
                                    expandedDays + day
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = day.name.take(3),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = day.name.lowercase().replaceFirstChar {
                                    it.titlecase(Locale.getDefault())
                                },
                                fontWeight = FontWeight.Bold,
                            )

                            Text(
                                text = when (timeRanges.size) {
                                    0 -> "No time ranges"
                                    1 -> "1 time range"
                                    else -> "${timeRanges.size} time ranges"
                                },
                            )
                        }

                        val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrow_rotation")
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.rotate(rotation)
                        )
                    }

                    // Collapsible content
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 20.dp),
                        ) {
                            // Time Ranges
                            if (timeRanges.isNotEmpty()) {
                                Column {
                                    timeRanges.forEach { timeRange ->
                                        TimeRangeRow(
                                            timeRange = timeRange,
                                            onDelete = { viewModel.deleteTimeRangeFromDay(day, timeRange) },
                                            onUpdate = { newTimeRange ->
                                                viewModel.updateTimeRangeInDay(day, timeRange, newTimeRange)
                                            }
                                        )
                                    }
                                }
                            } else {
                                // Empty State
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "No time ranges set",
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Add Time Button
                            FilledTonalButton(
                                onClick = { viewModel.addTimeRangeToDay(day, TimeRange(540, 600)) },
                                modifier = Modifier.align(Alignment.End),
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Add Time Range",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}