package com.andrew264.habits.ui.schedule.components

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
import com.andrew264.habits.ui.schedule.ScheduleUiEvent
import com.andrew264.habits.ui.schedule.ScheduleViewMode
import com.andrew264.habits.ui.schedule.ScheduleViewModel
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleEditorContent(
    scheduleId: String?,
    snackbarHostState: SnackbarHostState,
    onNavigateUp: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val perDayRepresentation by viewModel.perDayRepresentation.collectAsState()
    val view = LocalView.current

    val listState = rememberLazyListState()
    val expandedFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    LaunchedEffect(scheduleId) {
        viewModel.initialize(scheduleId)
    }

    LaunchedEffect(viewModel.uiEvents) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            LoadingState()
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
                            onValueChange = viewModel::updateScheduleName,
                            label = { Text("Schedule Name") },
                            placeholder = { Text("Enter schedule name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        FilledTonalButton(
                            onClick = { viewModel.saveSchedule() },
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
                                            viewModel.setViewMode(mode)
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
                                            viewModel.setViewMode(mode)
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
                                        listState = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        onUpdateGroupName = viewModel::updateGroupName,
                                        onDeleteGroup = viewModel::deleteGroup,
                                        onToggleDayInGroup = viewModel::toggleDayInGroup,
                                        onAddTimeRangeToGroup = viewModel::addTimeRangeToGroup,
                                        onUpdateTimeRangeInGroup = viewModel::updateTimeRangeInGroup,
                                        onDeleteTimeRangeFromGroup = viewModel::deleteTimeRangeFromGroup
                                    )
                                }
                            }

                            ScheduleViewMode.PER_DAY -> {
                                PerDayView(
                                    perDayRepresentation = perDayRepresentation,
                                    modifier = Modifier.fillMaxSize(),
                                    onAddTimeRangeToDay = viewModel::addTimeRangeToDay,
                                    onUpdateTimeRangeInDay = viewModel::updateTimeRangeInDay,
                                    onDeleteTimeRangeFromDay = viewModel::deleteTimeRangeFromDay
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB for adding a new group, scoped to this editor content
        if (!uiState.isLoading && uiState.viewMode == ScheduleViewMode.GROUPED) {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.addGroup()
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

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}