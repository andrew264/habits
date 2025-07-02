package com.andrew264.habits.ui.schedule.create

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleEditorScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val perDayRepresentation by viewModel.perDayRepresentation.collectAsState()
    val view = LocalView.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (uiState.isLoading) {
            LoadingState()
        } else {
            // Main content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Schedule Name Editor
                OutlinedTextField(
                    value = uiState.schedule?.name.orEmpty(),
                    onValueChange = { newName -> viewModel.updateScheduleName(newName) },
                    label = { Text("Schedule Name") },
                    placeholder = { Text("Enter schedule name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // View Mode Toggles
                val options = ScheduleViewMode.entries
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    options.forEachIndexed { index, mode ->
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
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        ScheduleViewMode.PER_DAY -> {
                            PerDayView(
                                perDayRepresentation = perDayRepresentation,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
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