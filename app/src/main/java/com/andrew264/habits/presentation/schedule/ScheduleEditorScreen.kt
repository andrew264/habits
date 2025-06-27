package com.andrew264.habits.presentation.schedule

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleEditorScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    // A real implementation would get the scheduleId from nav args
    LaunchedEffect(key1 = true) {
        viewModel.loadSchedule(null) // Load a new schedule for now
    }

    val schedule by viewModel.schedule.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val perDayRepresentation by viewModel.perDayRepresentation.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel.uiEvents) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is ScheduleUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }

                is ScheduleUiEvent.NavigateUp -> onNavigateUp()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        schedule?.let {
                            OutlinedTextField(
                                value = it.name,
                                onValueChange = { newName -> viewModel.updateScheduleName(newName) },
                                placeholder = {
                                    Text("Enter schedule name")
                                },
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateUp,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.saveSchedule() },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = "Save Schedule",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Save",
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // View Mode Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "View Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )

                    val options = ScheduleViewMode.entries
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            options.forEachIndexed { index, mode ->
                                ToggleButton(
                                    checked = viewMode == mode,
                                    onCheckedChange = { viewModel.setViewMode(mode) },
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
                                        fontWeight = if (mode == viewMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Content with smooth transitions
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Crossfade(
                    targetState = viewMode,
                    label = "ViewModeCrossfade"
                ) { mode ->
                    when (mode) {
                        ScheduleViewMode.GROUPED -> {
                            schedule?.let {
                                GroupedView(
                                    schedule = it,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } ?: LoadingState()
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LoadingIndicator()
                Text(
                    text = "Loading schedule...",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}