package com.andrew264.habits.ui.schedule.create

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleEditorScreen(
    viewModel: ScheduleViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val perDayRepresentation by viewModel.perDayRepresentation.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is ScheduleUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }

                is ScheduleUiEvent.NavigateUp -> {
                    onNavigateUp()
                }
            }
        }
    }

    ScheduleEditorScreenContent(
        uiState = uiState,
        perDayRepresentation = perDayRepresentation,
        onUpdateScheduleName = viewModel::updateScheduleName,
        onSaveSchedule = viewModel::saveSchedule,
        onSetViewMode = viewModel::setViewMode,
        onAddGroup = viewModel::addGroup,
        onDeleteGroup = viewModel::deleteGroup,
        onUpdateGroupName = viewModel::updateGroupName,
        onToggleDayInGroup = viewModel::toggleDayInGroup,
        onAddTimeRangeToGroup = viewModel::addTimeRangeToGroup,
        onUpdateTimeRangeInGroup = viewModel::updateTimeRangeInGroup,
        onDeleteTimeRangeFromGroup = viewModel::deleteTimeRangeFromGroup,
        onAddTimeRangeToDay = viewModel::addTimeRangeToDay,
        onUpdateTimeRangeInDay = viewModel::updateTimeRangeInDay,
        onDeleteTimeRangeFromDay = viewModel::deleteTimeRangeFromDay
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditorScreenContent(
    uiState: ScheduleEditorUiState,
    perDayRepresentation: Map<DayOfWeek, List<TimeRange>>,
    onUpdateScheduleName: (String) -> Unit,
    onSaveSchedule: () -> Unit,
    onSetViewMode: (ScheduleViewMode) -> Unit,
    onAddGroup: () -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onUpdateGroupName: (groupId: String, newName: String) -> Unit,
    onToggleDayInGroup: (groupId: String, day: DayOfWeek) -> Unit,
    onAddTimeRangeToGroup: (groupId: String, timeRange: TimeRange) -> Unit,
    onUpdateTimeRangeInGroup: (groupId: String, updatedTimeRange: TimeRange) -> Unit,
    onDeleteTimeRangeFromGroup: (groupId: String, timeRange: TimeRange) -> Unit,
    onAddTimeRangeToDay: (day: DayOfWeek, timeRange: TimeRange) -> Unit,
    onUpdateTimeRangeInDay: (day: DayOfWeek, updatedTimeRange: TimeRange) -> Unit,
    onDeleteTimeRangeFromDay: (day: DayOfWeek, timeRange: TimeRange) -> Unit,
) {
    val view = LocalView.current

    Scaffold(
        floatingActionButton = {
            if (uiState.viewMode == ScheduleViewMode.GROUPED) {
                SmallExtendedFloatingActionButton(
                    onClick = {
                        onAddGroup()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    icon = { Icon(Icons.Default.Add, "Create New Group") },
                    text = { Text("New Group") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                LoadingState()
            } else {
                // Main content area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.PaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
                        // Schedule Name Editor
                        OutlinedTextField(
                            value = uiState.schedule?.name.orEmpty(),
                            onValueChange = onUpdateScheduleName,
                            label = { Text("Schedule Name") },
                            placeholder = { Text("Enter schedule name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        FilledTonalButton(
                            onClick = {
                                onSaveSchedule()
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = "Save Schedule",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(Dimens.PaddingSmall))
                            Text("Save")
                        }
                    }


                    // View Mode Toggles
                    val options = ScheduleViewMode.entries
                    ButtonGroup(
                        overflowIndicator = { menuState ->
                            IconButton(onClick = { menuState.show() }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        options.forEachIndexed { index, mode ->
                            customItem(
                                buttonGroupContent = {
                                    ToggleButton(
                                        checked = uiState.viewMode == mode,
                                        onCheckedChange = {
                                            onSetViewMode(mode)
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                    ) {
                                        Text(
                                            text = when (mode) {
                                                ScheduleViewMode.GROUPED -> "📋 Grouped"
                                                ScheduleViewMode.PER_DAY -> "📅 Per Day"
                                            },
                                            fontWeight = if (mode == uiState.viewMode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                menuContent = { menuState ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (mode) {
                                                    ScheduleViewMode.GROUPED -> "Grouped"
                                                    ScheduleViewMode.PER_DAY -> "Per Day"
                                                }
                                            )
                                        },
                                        onClick = {
                                            onSetViewMode(mode)
                                            menuState.dismiss()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                // Content with smooth transitions
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Crossfade(
                        targetState = uiState.viewMode,
                        label = "ViewModeCrossfade"
                    ) { mode ->
                        when (mode) {
                            ScheduleViewMode.GROUPED -> {
                                uiState.schedule?.let {
                                    GroupedView(
                                        schedule = it,
                                        modifier = Modifier.fillMaxSize(),
                                        onUpdateGroupName = onUpdateGroupName,
                                        onDeleteGroup = onDeleteGroup,
                                        onToggleDayInGroup = onToggleDayInGroup,
                                        onAddTimeRangeToGroup = onAddTimeRangeToGroup,
                                        onUpdateTimeRangeInGroup = onUpdateTimeRangeInGroup,
                                        onDeleteTimeRangeFromGroup = onDeleteTimeRangeFromGroup
                                    )
                                }
                            }

                            ScheduleViewMode.PER_DAY -> {
                                PerDayView(
                                    perDayRepresentation = perDayRepresentation,
                                    modifier = Modifier.fillMaxSize(),
                                    onAddTimeRangeToDay = onAddTimeRangeToDay,
                                    onUpdateTimeRangeInDay = onUpdateTimeRangeInDay,
                                    onDeleteTimeRangeFromDay = onDeleteTimeRangeFromDay
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Preview(name = "Editor - Grouped View", showBackground = true)
@Composable
private fun ScheduleEditorScreenGroupedPreview() {
    val schedule = Schedule(
        id = "1",
        name = "Work Schedule",
        groups = listOf(
            ScheduleGroup(
                id = "g1",
                name = "Weekdays",
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                timeRanges = listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60))
            ),
            ScheduleGroup(
                id = "g2",
                name = "Weekends",
                days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                timeRanges = listOf(TimeRange(fromMinuteOfDay = 10 * 60, toMinuteOfDay = 14 * 60))
            )
        )
    )
    HabitsTheme {
        ScheduleEditorScreenContent(
            uiState = ScheduleEditorUiState(isLoading = false, schedule = schedule, viewMode = ScheduleViewMode.GROUPED),
            perDayRepresentation = emptyMap(),
            onUpdateScheduleName = {}, onSaveSchedule = {}, onSetViewMode = {}, onAddGroup = {},
            onDeleteGroup = {}, onUpdateGroupName = { _, _ -> }, onToggleDayInGroup = { _, _ -> },
            onAddTimeRangeToGroup = { _, _ -> }, onUpdateTimeRangeInGroup = { _, _ -> },
            onDeleteTimeRangeFromGroup = { _, _ -> }, onAddTimeRangeToDay = { _, _ -> },
            onUpdateTimeRangeInDay = { _, _ -> }, onDeleteTimeRangeFromDay = { _, _ -> }
        )
    }
}

@Preview(name = "Editor - Per Day View", showBackground = true)
@Composable
private fun ScheduleEditorScreenPerDayPreview() {
    val perDayRep = mapOf(
        DayOfWeek.MONDAY to listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60)),
        DayOfWeek.TUESDAY to listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60)),
        DayOfWeek.WEDNESDAY to listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 12 * 60), TimeRange(fromMinuteOfDay = 13 * 60, toMinuteOfDay = 17 * 60)),
        DayOfWeek.THURSDAY to listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60)),
        DayOfWeek.FRIDAY to listOf(TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 16 * 60)),
        DayOfWeek.SATURDAY to emptyList(),
        DayOfWeek.SUNDAY to emptyList()
    )
    HabitsTheme {
        ScheduleEditorScreenContent(
            uiState = ScheduleEditorUiState(
                isLoading = false,
                schedule = Schedule(id = "1", name = "My Schedule", groups = emptyList()),
                viewMode = ScheduleViewMode.PER_DAY
            ),
            perDayRepresentation = perDayRep,
            onUpdateScheduleName = {}, onSaveSchedule = {}, onSetViewMode = {}, onAddGroup = {},
            onDeleteGroup = {}, onUpdateGroupName = { _, _ -> }, onToggleDayInGroup = { _, _ -> },
            onAddTimeRangeToGroup = { _, _ -> }, onUpdateTimeRangeInGroup = { _, _ -> },
            onDeleteTimeRangeFromGroup = { _, _ -> }, onAddTimeRangeToDay = { _, _ -> },
            onUpdateTimeRangeInDay = { _, _ -> }, onDeleteTimeRangeFromDay = { _, _ -> }
        )
    }
}

@Preview(name = "Editor - Empty", showBackground = true)
@Composable
private fun ScheduleEditorScreenEmptyPreview() {
    HabitsTheme {
        ScheduleEditorScreenContent(
            uiState = ScheduleEditorUiState(
                isLoading = false,
                schedule = Schedule(id = "1", name = "New Schedule", groups = emptyList()),
                viewMode = ScheduleViewMode.GROUPED
            ),
            perDayRepresentation = emptyMap(),
            onUpdateScheduleName = {}, onSaveSchedule = {}, onSetViewMode = {}, onAddGroup = {},
            onDeleteGroup = {}, onUpdateGroupName = { _, _ -> }, onToggleDayInGroup = { _, _ -> },
            onAddTimeRangeToGroup = { _, _ -> }, onUpdateTimeRangeInGroup = { _, _ -> },
            onDeleteTimeRangeFromGroup = { _, _ -> }, onAddTimeRangeToDay = { _, _ -> },
            onUpdateTimeRangeInDay = { _, _ -> }, onDeleteTimeRangeFromDay = { _, _ -> }
        )
    }
}