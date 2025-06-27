package com.andrew264.habits.presentation.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.TimeRange
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PerDayView(
    perDayRepresentation: Map<DayOfWeek, List<TimeRange>>,
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(DayOfWeek.entries.toList(), key = { it.name }) { day ->
            val timeRanges = perDayRepresentation[day] ?: emptyList()
            val dayColor = getDayColor(day)

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Day Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                        // Time Range Count Badge
                        if (timeRanges.isNotEmpty()) {
                            Badge(
                                containerColor = dayColor,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = timeRanges.size.toString(),
                                )
                            }
                        }
                    }

                    // Time Ranges
                    if (timeRanges.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                Text(
                                    text = "Add a time range to get started",
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
                            text = "Add Time",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getDayColor(day: DayOfWeek): Color {
    return when (day) {
        DayOfWeek.MONDAY -> Color(0xFF6366F1)    // Indigo
        DayOfWeek.TUESDAY -> Color(0xFF8B5CF6)   // Violet
        DayOfWeek.WEDNESDAY -> Color(0xFFA855F7) // Purple
        DayOfWeek.THURSDAY -> Color(0xFFEC4899)  // Pink
        DayOfWeek.FRIDAY -> Color(0xFFF97316)    // Orange
        DayOfWeek.SATURDAY -> Color(0xFF10B981)  // Emerald
        DayOfWeek.SUNDAY -> Color(0xFF3B82F6)    // Blue
    }
}