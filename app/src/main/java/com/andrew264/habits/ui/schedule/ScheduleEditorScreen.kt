package com.andrew264.habits.ui.schedule

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.schedule.components.GroupedView
import com.andrew264.habits.ui.schedule.components.PerDayView
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

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
        onNavigateUp = onNavigateUp,
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
    onNavigateUp: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewSchedule) "New Schedule" else "Edit Schedule") },
                subtitle = {
                    uiState.scheduleCoverage?.let {
                        Text(
                            "${String.format(Locale.getDefault(), "%.1f", it.totalHours)} hours/week",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                navigationIcon = {
                    val interactionSource = remember { MutableInteractionSource() }
                    HapticInteractionEffect(interactionSource)
                    IconButton(onClick = onNavigateUp, interactionSource = interactionSource) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    val interactionSource = remember { MutableInteractionSource() }
                    HapticInteractionEffect(interactionSource)
                    IconButton(
                        onClick = onSaveSchedule, interactionSource = interactionSource,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier
                            .width(64.dp)
                            .height(48.dp)
                            .padding(end = Dimens.PaddingSmall),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Icon(Icons.Default.Done, "Save Schedule")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.viewMode == ScheduleViewMode.GROUPED) {
                val fabInteractionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(fabInteractionSource)
                SmallExtendedFloatingActionButton(
                    text = { Text("New Group") },
                    icon = { Icon(Icons.Default.Add, "Create New Group") },
                    onClick = onAddGroup,
                    expanded = expandedFab,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    interactionSource = fabInteractionSource
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        if (uiState.isLoading) {
            ContainedLoadingIndicator(Modifier.padding(paddingValues))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.PaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    OutlinedTextField(
                        value = uiState.schedule?.name.orEmpty(),
                        onValueChange = onUpdateScheduleName,
                        label = { Text("Schedule Name") },
                        placeholder = { Text("Enter schedule name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

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
                                            if (uiState.viewMode != mode) {
                                                onSetViewMode(mode)
                                                view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                                            }
                                        },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                        colors = ToggleButtonDefaults.toggleButtonColors(containerColor = MaterialTheme.colorScheme.surface)
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
    }
}