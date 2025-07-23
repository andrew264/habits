package com.andrew264.habits.ui.schedule

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.schedule.components.GroupedView
import com.andrew264.habits.ui.schedule.components.PerDayView
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ScheduleEditorScreen(
    scheduleId: String?,
    snackbarHostState: SnackbarHostState,
    onNavigateUp: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val perDayRepresentation by viewModel.perDayRepresentation.collectAsState()

    LaunchedEffect(scheduleId) {
        viewModel.initialize(scheduleId)
    }

    LaunchedEffect(viewModel.uiEvents) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is ScheduleUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message, duration = SnackbarDuration.Short)
                }

                is ScheduleUiEvent.NavigateUp -> {
                    onNavigateUp()
                }
            }
        }
    }

    ScheduleEditorScreen(
        uiState = uiState,
        perDayRepresentation = perDayRepresentation,
        onSaveSchedule = viewModel::saveSchedule,
        onSetViewMode = viewModel::setViewMode,
        onUpdateScheduleName = viewModel::updateScheduleName,
        onAddGroup = viewModel::addGroup,
        onUpdateGroupName = viewModel::updateGroupName,
        onDeleteGroup = viewModel::deleteGroup,
        onToggleDayInGroup = viewModel::toggleDayInGroup,
        onAddTimeRangeToGroup = viewModel::addTimeRangeToGroup,
        onUpdateTimeRangeInGroup = viewModel::updateTimeRangeInGroup,
        onDeleteTimeRangeFromGroup = viewModel::deleteTimeRangeFromGroup,
        onAddTimeRangeToDay = viewModel::addTimeRangeToDay,
        onUpdateTimeRangeInDay = viewModel::updateTimeRangeInDay,
        onDeleteTimeRangeFromDay = viewModel::deleteTimeRangeFromDay,
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScheduleEditorScreen(
    uiState: ScheduleEditorUiState,
    perDayRepresentation: Map<DayOfWeek, List<TimeRange>>,
    onSaveSchedule: () -> Unit,
    onSetViewMode: (ScheduleViewMode) -> Unit,
    onUpdateScheduleName: (String) -> Unit,
    onAddGroup: () -> Unit,
    onUpdateGroupName: (groupId: String, newName: String) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onToggleDayInGroup: (groupId: String, day: DayOfWeek) -> Unit,
    onAddTimeRangeToGroup: (groupId: String, timeRange: TimeRange) -> Unit,
    onUpdateTimeRangeInGroup: (groupId: String, updatedTimeRange: TimeRange) -> Unit,
    onDeleteTimeRangeFromGroup: (groupId: String, timeRange: TimeRange) -> Unit,
    onAddTimeRangeToDay: (day: DayOfWeek, timeRange: TimeRange) -> Unit,
    onUpdateTimeRangeInDay: (day: DayOfWeek, updatedTimeRange: TimeRange) -> Unit,
    onDeleteTimeRangeFromDay: (day: DayOfWeek, timeRange: TimeRange) -> Unit,
) {
    val view = LocalView.current

    val listState = rememberLazyListState()
    val expandedFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            ContainedLoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Main content area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.PaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                    ) {
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
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }


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
                                        listState = listState,
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


        if (!uiState.isLoading && uiState.viewMode == ScheduleViewMode.GROUPED) {
            ExtendedFloatingActionButton(
                onClick = {
                    onAddGroup()
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                },
                expanded = expandedFab,
                icon = { Icon(Icons.Default.Add, "Create New Group") },
                text = { Text("New Group") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.PaddingLarge)
            )
        }
    }
}