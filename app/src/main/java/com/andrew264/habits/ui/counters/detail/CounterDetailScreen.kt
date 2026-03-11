package com.andrew264.habits.ui.counters.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.andrew264.habits.ui.common.charts.BarChart
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.components.FilterButtonGroup
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.common.duration_picker.DurationPickerDialog
import com.andrew264.habits.ui.common.list_items.ListSectionHeader
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.navigation.AppRoute
import com.andrew264.habits.ui.navigation.CounterEditor
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        onSetTimeRange = viewModel::setTimeRange,
        onNewLogValueChange = viewModel::onNewLogValueChange,
        onAddLog = {
            val value = uiState.newLogValue.toDoubleOrNull()
            if (value != null) viewModel.addLog(value)
        },
        onAddDuration = { viewModel.addLog(it.toDouble()) },
        onDeleteLog = { viewModel.deleteLog(it) },
        onShowDurationPicker = viewModel::onShowDurationPicker,
        onDismissDurationPicker = viewModel::onDismissDurationPicker
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterDetailScreen(
    uiState: CounterDetailUiState,
    onNavigateUp: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onSetTimeRange: (ChartTimeRange) -> Unit,
    onNewLogValueChange: (String) -> Unit,
    onAddLog: () -> Unit,
    onAddDuration: (Int) -> Unit,
    onDeleteLog: (com.andrew264.habits.domain.model.CounterLog) -> Unit,
    onShowDurationPicker: () -> Unit,
    onDismissDurationPicker: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (uiState.showDurationPicker) {
        DurationPickerDialog(
            title = stringResource(R.string.counter_detail_add_entry),
            description = "",
            initialTotalMinutes = 0,
            onDismissRequest = onDismissDurationPicker,
            onConfirm = onAddDuration
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
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.counter_detail_edit_counter))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        if (uiState.isLoading) {
            ContainedLoadingIndicator(Modifier.padding(paddingValues))
        } else if (uiState.details == null) {
            Text(stringResource(R.string.schedule_selector_none))
        } else {
            val details = uiState.details
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(all = Dimens.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                item { AddEntryCard(uiState, onNewLogValueChange, onAddLog, onShowDurationPicker) }

                if (details.recentLogs.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.History,
                            title = stringResource(R.string.counter_detail_no_history_title),
                            description = stringResource(R.string.counter_detail_no_history_description)
                        )
                    }
                } else {
                    item { HistoryChartCard(uiState, onSetTimeRange) }
                    item { ListSectionHeader(stringResource(R.string.counter_detail_recent_history_title)) }
                    items(details.recentLogs, key = { it.id }) { log ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        SwipeToDismissBox(
                            state = dismissState,
                            onDismiss = { direction ->
                                if (direction == SwipeToDismissBoxValue.EndToStart) {
                                    onDeleteLog(log)
                                }
                            },
                            backgroundContent = {
                                val color = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = Dimens.PaddingLarge),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.schedule_list_item_delete))
                                }
                            }) {
                            val date = Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault())
                            ListItem(
                                headlineContent = { Text(FormatUtils.formatCounterValue(log.value, details.counter.type)) },
                                supportingContent = { Text(date.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))) }
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEntryCard(
    uiState: CounterDetailUiState,
    onNewLogValueChange: (String) -> Unit,
    onAddLog: () -> Unit,
    onShowDurationPicker: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            Text(stringResource(R.string.counter_detail_add_entry), style = MaterialTheme.typography.titleLarge)

            if (uiState.details?.counter?.type == CounterType.DURATION) {
                Button(onClick = onShowDurationPicker, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.counter_detail_add_entry))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    OutlinedTextField(
                        value = uiState.newLogValue,
                        onValueChange = onNewLogValueChange,
                        label = { Text(stringResource(R.string.counter_detail_new_value_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    IconButton(onClick = onAddLog, enabled = uiState.newLogValue.isNotBlank()) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.counter_detail_add_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChartCard(
    uiState: CounterDetailUiState,
    onSetTimeRange: (ChartTimeRange) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.counter_detail_history_chart_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Dimens.PaddingMedium))
            FilterButtonGroup(
                options = ChartTimeRange.entries,
                selectedOption = uiState.selectedRange,
                onOptionSelected = onSetTimeRange,
                label = { Text(stringResource(it.label)) }
            )
            Spacer(Modifier.height(Dimens.PaddingLarge))
            BarChart(
                entries = uiState.details?.chartEntries ?: emptyList(),
                barColor = uiState.details?.counter?.colorHex?.toColorOrNull() ?: MaterialTheme.colorScheme.primary,
                yAxisLabelFormatter = { FormatUtils.formatCounterValue(it.toDouble(), uiState.details!!.counter.type) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Preview
@Composable
private fun CounterDetailScreenPreview() {
    HabitsTheme {
        val counter = com.andrew264.habits.domain.model.Counter("1", "Pushups", CounterType.NUMBER, AggregationType.MAX, 50.0, "#F44336")
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
                details = com.andrew264.habits.domain.usecase.counter.CounterDetailsModel(counter, logs, chartEntries)
            ),
            onNavigateUp = {},
            onNavigateToEdit = {},
            onSetTimeRange = {},
            onNewLogValueChange = {},
            onAddLog = {},
            onDeleteLog = {},
            onAddDuration = {},
            onShowDurationPicker = {},
            onDismissDurationPicker = {}
        )
    }
}