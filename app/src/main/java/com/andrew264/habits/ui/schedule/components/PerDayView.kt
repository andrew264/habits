package com.andrew264.habits.ui.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.Dimens
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PerDayView(
    perDayRepresentation: Map<DayOfWeek, List<TimeRange>>,
    modifier: Modifier = Modifier,
    onAddTimeRangeToDay: (day: DayOfWeek, timeRange: TimeRange) -> Unit,
    onUpdateTimeRangeInDay: (day: DayOfWeek, updatedTimeRange: TimeRange) -> Unit,
    onDeleteTimeRangeFromDay: (day: DayOfWeek, timeRange: TimeRange) -> Unit,
) {
    var expandedDays by rememberSaveable { mutableStateOf(emptySet<DayOfWeek>()) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(all = Dimens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        items(DayOfWeek.entries.toList(), key = { it.name }) { day ->
            val timeRanges = perDayRepresentation[day] ?: emptyList()
            val isExpanded = day in expandedDays
            val expandInteractionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(expandInteractionSource)

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Column {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = expandInteractionSource,
                                indication = LocalIndication.current,
                                onClick = {
                                    expandedDays = if (isExpanded) {
                                        expandedDays - day
                                    } else {
                                        expandedDays + day
                                    }
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = Dimens.PaddingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
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
                                    style = MaterialTheme.typography.titleMedium,
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            Text(
                                text = when (timeRanges.size) {
                                    0 -> stringResource(R.string.per_day_view_no_time_ranges)
                                    1 -> stringResource(R.string.per_day_view_one_time_range)
                                    else -> stringResource(R.string.per_day_view_time_ranges, timeRanges.size)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = stringResource(R.string.per_day_view_arrow_rotation))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) stringResource(R.string.per_day_view_collapse) else stringResource(R.string.per_day_view_expand),
                            modifier = Modifier.rotate(rotation)
                        )
                    }


                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 20.dp),
                        ) {

                            if (timeRanges.isNotEmpty()) {
                                Column {
                                    timeRanges.forEach { timeRange ->
                                        key(timeRange.id) {
                                            TimeRangeRow(
                                                timeRange = timeRange,
                                                onDelete = {
                                                    onDeleteTimeRangeFromDay(
                                                        day,
                                                        timeRange
                                                    )
                                                },
                                                onUpdate = { newTimeRange ->
                                                    onUpdateTimeRangeInDay(
                                                        day,
                                                        newTimeRange
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Dimens.PaddingExtraLarge),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.per_day_view_no_time_ranges_set),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }

                            val addInteractionSource = remember { MutableInteractionSource() }
                            HapticInteractionEffect(addInteractionSource)
                            FilledTonalButton(
                                onClick = {
                                    onAddTimeRangeToDay(day, TimeRange(fromMinuteOfDay = 540, toMinuteOfDay = 600))
                                },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ),
                                interactionSource = addInteractionSource,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimens.PaddingLarge)
                                )
                                Spacer(Modifier.width(Dimens.PaddingSmall))
                                Text(
                                    text = stringResource(R.string.per_day_view_add_time_range),
                                )
                            }
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
fun PerDayViewPreview() {
    val perDayRepresentation = mapOf(
        DayOfWeek.MONDAY to listOf(
            TimeRange(fromMinuteOfDay = 540, toMinuteOfDay = 600),
            TimeRange(fromMinuteOfDay = 720, toMinuteOfDay = 780)
        ),
        DayOfWeek.TUESDAY to emptyList(),
        DayOfWeek.WEDNESDAY to listOf(
            TimeRange(fromMinuteOfDay = 600, toMinuteOfDay = 660)
        )
    )
    PerDayView(
        perDayRepresentation = perDayRepresentation,
        onAddTimeRangeToDay = { _, _ -> },
        onUpdateTimeRangeInDay = { _, _ -> },
        onDeleteTimeRangeFromDay = { _, _ -> }
    )
}