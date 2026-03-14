package com.andrew264.habits.ui.counters.detail

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.model.counter.AggregationType
import com.andrew264.habits.model.counter.CounterType
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.common.charts.InteractiveLineGraph
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.common.duration_picker.DurationPickerDialog
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.common.list_items.ListSectionHeader
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.counters.components.TimelineItem
import com.andrew264.habits.ui.counters.components.TimelineLogItem
import com.andrew264.habits.ui.counters.components.TimelineSkipItem
import com.andrew264.habits.ui.counters.components.rememberTimelineItems
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.CounterEditor
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
fun CounterDetailScreen(
    counterId: String,
    onNavigate: (AppRoute) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: CounterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(counterId) {
        viewModel.initialize(counterId)
    }

    CounterDetailScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onNavigateToEdit = { onNavigate(CounterEditor(counterId = counterId)) },
        onNewLogValueChange = viewModel::onNewLogValueChange,
        onAddLog = {
            val value = uiState.newLogValue.toDoubleOrNull()
            if (value != null) viewModel.addLog(value)
        },
        onAddDuration = { viewModel.addLog(it) },
        onDeleteLog = { viewModel.deleteLog(it) },
        onShowDurationPicker = viewModel::onShowDurationPicker,
        onDismissDurationPicker = viewModel::onDismissDurationPicker,
        onChartEntrySelected = viewModel::onChartEntrySelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterDetailScreen(
    uiState: CounterDetailUiState,
    onNavigateUp: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNewLogValueChange: (String) -> Unit,
    onAddLog: () -> Unit,
    onAddDuration: (Double) -> Unit,
    onDeleteLog: (com.andrew264.habits.domain.model.CounterLog) -> Unit,
    onShowDurationPicker: () -> Unit,
    onDismissDurationPicker: () -> Unit,
    onChartEntrySelected: (Int?) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (uiState.showDurationPicker) {
        DurationPickerDialog(
            title = stringResource(R.string.counter_detail_add_entry),
            description = "",
            initialHours = 0,
            initialMinutes = 0,
            initialSeconds = 0,
            showSeconds = true,
            minuteInterval = 1,
            onDismissRequest = onDismissDurationPicker,
            onConfirm = { h, m, s ->
                val totalMinutes = h * 60 + m + (s / 60.0)
                onAddDuration(totalMinutes)
                onDismissDurationPicker()
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SimpleTopAppBar(
                title = uiState.details?.counter?.name ?: "",
                onNavigateUp = onNavigateUp,
                scrollBehavior = scrollBehavior,
                actions = {
                    val editInteractionSource = remember { MutableInteractionSource() }
                    HapticInteractionEffect(editInteractionSource)
                    IconButton(onClick = onNavigateToEdit, interactionSource = editInteractionSource) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.counter_detail_edit_counter)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.details != null && !uiState.isLoading) {
                BottomAddEntryBar(
                    uiState = uiState,
                    onNewLogValueChange = onNewLogValueChange,
                    onAddLog = onAddLog,
                    onShowDurationPicker = onShowDurationPicker
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        if (uiState.isLoading) {
            ContainedLoadingIndicator(Modifier.padding(paddingValues))
        } else if (uiState.details == null) {
            Box(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.schedule_selector_none))
            }
        } else {
            val details = uiState.details
            val timelineItems = rememberTimelineItems(uiState.displayedLogs)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                if (details.allLogs.isNotEmpty()) {
                    item {
                        Column {
                            InteractiveLineGraph(
                                entries = details.chartEntries,
                                lineColor = details.counter.colorHex.toColorOrNull()
                                    ?: MaterialTheme.colorScheme.primary,
                                selectedIndex = uiState.selectedChartIndex,
                                onSelectionChanged = onChartEntrySelected,
                                yAxisLabelFormatter = {
                                    FormatUtils.formatCounterValue(
                                        it.toDouble(),
                                        details.counter.type
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                        }
                    }

                    item {
                        Box(modifier = Modifier.padding(horizontal = Dimens.PaddingLarge)) {
                            ListSectionHeader(
                                title = if (uiState.selectedDateLabel.isNotEmpty())
                                    "Logs for ${uiState.selectedDateLabel}"
                                else
                                    stringResource(R.string.counter_detail_recent_history_title)
                            )
                        }
                    }

                    if (uiState.displayedLogs.isEmpty()) {
                        item {
                            Text(
                                text = "No logs recorded on this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Dimens.PaddingExtraLarge)
                            )
                        }
                    } else {
                        items(timelineItems, key = { it.key }) { item ->
                            when (item) {
                                is TimelineItem.LogEntry -> {
                                    TimelineLogItem(
                                        entry = item,
                                        counterType = details.counter.type,
                                        color = details.counter.colorHex.toColorOrNull() ?: MaterialTheme.colorScheme.primary,
                                        onDelete = { onDeleteLog(item.log) }
                                    )
                                }

                                is TimelineItem.TimeSkip -> {
                                    TimelineSkipItem(color = details.counter.colorHex.toColorOrNull() ?: MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        EmptyState(
                            modifier = Modifier.padding(horizontal = Dimens.PaddingLarge, vertical = 64.dp),
                            icon = Icons.Outlined.History,
                            title = stringResource(R.string.counter_detail_no_history_title),
                            description = stringResource(R.string.counter_detail_no_history_description)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomAddEntryBar(
    uiState: CounterDetailUiState,
    onNewLogValueChange: (String) -> Unit,
    onAddLog: () -> Unit,
    onShowDurationPicker: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            if (uiState.details?.counter?.type == CounterType.DURATION) {
                val addInteractionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(addInteractionSource)
                Button(
                    onClick = onShowDurationPicker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    interactionSource = addInteractionSource
                ) {
                    Text(stringResource(R.string.counter_detail_add_entry))
                }
            } else {
                OutlinedTextField(
                    value = uiState.newLogValue,
                    onValueChange = onNewLogValueChange,
                    placeholder = { Text(stringResource(R.string.counter_detail_new_value_label)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                val addInteractionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(addInteractionSource)
                Button(
                    onClick = { onAddLog() },
                    enabled = uiState.newLogValue.isNotBlank(),
                    modifier = Modifier.height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    interactionSource = addInteractionSource
                ) {
                    Text(stringResource(R.string.counter_detail_add_button))
                }
            }
        }
    }
}

@Preview
@Composable
private fun CounterDetailScreenPreview() {
    HabitsTheme {
        val counter = com.andrew264.habits.domain.model.Counter(
            "1",
            "Pushups",
            CounterType.NUMBER,
            AggregationType.MAX,
            50.0,
            "#F44336"
        )
        val logs = listOf(
            com.andrew264.habits.domain.model.CounterLog(1, "1", System.currentTimeMillis() - 86400000, 45.0),
            com.andrew264.habits.domain.model.CounterLog(2, "1", System.currentTimeMillis(), 48.0)
        )
        val chartEntries = listOf(
            BarChartEntry(40f, "Mon"),
            BarChartEntry(42f, "Tue"),
            BarChartEntry(45f, "Wed")
        )

        CounterDetailScreen(
            uiState = CounterDetailUiState(
                isLoading = false,
                details = com.andrew264.habits.domain.usecase.counter.CounterDetailsModel(
                    counter,
                    allLogs = logs,
                    chartEntries
                ),
                displayedLogs = logs,
                selectedDateLabel = "Mar 14"
            ),
            onNavigateUp = {},
            onNavigateToEdit = {},
            onNewLogValueChange = {},
            onAddLog = {},
            onDeleteLog = {},
            onAddDuration = {},
            onShowDurationPicker = {},
            onDismissDurationPicker = {},
            onChartEntrySelected = {}
        )
    }
}