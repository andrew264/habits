package com.andrew264.habits.ui.counters.editor

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.model.counter.AggregationType
import com.andrew264.habits.model.counter.CounterType
import com.andrew264.habits.ui.common.color_picker.ColorPickerDialog
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.common.list_items.ListItemPosition
import com.andrew264.habits.ui.common.list_items.ListSectionHeader
import com.andrew264.habits.ui.common.list_items.NavigationListItem
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CounterEditorScreen(
    counterId: String?,
    onNavigateUp: () -> Unit,
    viewModel: CounterEditorViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(counterId) {
        viewModel.initialize(counterId)
    }
    LaunchedEffect(viewModel.uiEvents) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is CounterEditorUiEvent.NavigateUp -> onNavigateUp()
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteConfirmation,
            title = { Text(stringResource(R.string.counter_editor_delete_confirmation_title)) },
            text = { Text(stringResource(R.string.counter_editor_delete_confirmation_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        viewModel.onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.counter_editor_delete_confirmation_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    viewModel.onDismissDeleteConfirmation()
                }) {
                    Text(stringResource(R.string.data_management_cancel))
                }
            }
        )
    }

    CounterEditorScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onNameChange = viewModel::onNameChange,
        onColorChange = viewModel::onColorChange,
        onTypeChange = viewModel::onTypeChange,
        onAggregationTypeChange = viewModel::onAggregationTypeChange,
        onTargetChange = viewModel::onTargetChange,
        onSave = viewModel::onSave,
        onDelete = viewModel::onShowDeleteConfirmation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterEditorScreen(
    uiState: CounterEditorUiState,
    onNavigateUp: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorChange: (androidx.compose.ui.graphics.Color) -> Unit,
    onTypeChange: (CounterType) -> Unit,
    onAggregationTypeChange: (AggregationType) -> Unit,
    onTargetChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = uiState.colorHex.toColorOrNull() ?: MaterialTheme.colorScheme.primary,
            onDismissRequest = { showColorPicker = false },
            onConfirmation = {
                onColorChange(it)
                showColorPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = if (uiState.isNewCounter) stringResource(R.string.counter_editor_new_counter_title) else stringResource(R.string.counter_editor_edit_counter_title),
                onNavigateUp = onNavigateUp,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val saveInteractionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(saveInteractionSource)
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.counter_editor_save)) },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                onClick = onSave,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                interactionSource = saveInteractionSource
            )
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
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.counter_editor_name_label)) },
                    placeholder = { Text(stringResource(R.string.counter_editor_name_placeholder)) },
                    isError = uiState.nameError != null,
                    supportingText = { uiState.nameError?.let { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )

                NavigationListItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(uiState.colorHex.toColorOrNull() ?: MaterialTheme.colorScheme.primary)
                        )
                    },
                    title = stringResource(R.string.counter_editor_display_color),
                    onClick = { showColorPicker = true },
                    position = ListItemPosition.SEPARATE
                )

                OptionGroup(
                    title = stringResource(R.string.counter_editor_section_type),
                    options = CounterType.entries,
                    selectedOption = uiState.type,
                    onOptionSelected = onTypeChange,
                    optionLabel = { it.toDisplayString() }
                )

                OptionGroup(
                    title = stringResource(R.string.counter_editor_section_aggregation),
                    options = AggregationType.entries,
                    selectedOption = uiState.aggregationType,
                    onOptionSelected = onAggregationTypeChange,
                    optionLabel = { it.toDisplayString() }
                )

                Column {
                    ListSectionHeader(stringResource(R.string.counter_editor_section_target))
                    OutlinedTextField(
                        value = uiState.target,
                        onValueChange = onTargetChange,
                        label = { Text(stringResource(R.string.counter_editor_target_label)) },
                        isError = uiState.targetError != null,
                        supportingText = {
                            if (uiState.targetError != null) {
                                Text(stringResource(uiState.targetError))
                            } else {
                                Text(
                                    when (uiState.type) {
                                        CounterType.NUMBER -> stringResource(R.string.counter_editor_target_description_number)
                                        CounterType.DECIMAL -> stringResource(R.string.counter_editor_target_description_decimal)
                                        CounterType.DURATION -> stringResource(R.string.counter_editor_target_description_duration)
                                    }
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!uiState.isNewCounter) {
                    val deleteInteractionSource = remember { MutableInteractionSource() }
                    HapticInteractionEffect(deleteInteractionSource)
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        interactionSource = deleteInteractionSource,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(Dimens.PaddingSmall))
                        Text(stringResource(R.string.counter_editor_delete_counter))
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionGroup(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: @Composable (T) -> String
) {
    val view = LocalView.current
    Column {
        ListSectionHeader(title)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                SegmentedButton(
                    selected = option == selectedOption,
                    onClick = {
                        onOptionSelected(option)
                        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                    },
                    shape = SegmentedButtonDefaults.itemShape(options.indexOf(option), options.size)
                ) {
                    Text(optionLabel(option))
                }
            }
        }
    }
}

@Composable
fun CounterType.toDisplayString(): String = when (this) {
    CounterType.NUMBER -> stringResource(R.string.counter_type_number)
    CounterType.DECIMAL -> stringResource(R.string.counter_type_decimal)
    CounterType.DURATION -> stringResource(R.string.counter_type_duration)
}

@Composable
fun AggregationType.toDisplayString(): String = when (this) {
    AggregationType.SUM -> stringResource(R.string.aggregation_type_sum)
    AggregationType.MAX -> stringResource(R.string.aggregation_type_max)
    AggregationType.MIN -> stringResource(R.string.aggregation_type_min)
    AggregationType.AVERAGE -> stringResource(R.string.aggregation_type_average)
}

@Preview(showBackground = true)
@Composable
private fun CounterEditorScreenNewPreview() {
    HabitsTheme {
        CounterEditorScreen(
            uiState = CounterEditorUiState(isLoading = false, isNewCounter = true),
            onNavigateUp = {}, onNameChange = {}, onColorChange = {}, onTypeChange = {}, onAggregationTypeChange = {}, onTargetChange = {}, onSave = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CounterEditorScreenEditPreview() {
    HabitsTheme {
        CounterEditorScreen(
            uiState = CounterEditorUiState(
                isLoading = false,
                isNewCounter = false,
                name = "Pushups",
                target = "50"
            ),
            onNavigateUp = {}, onNameChange = {}, onColorChange = {}, onTypeChange = {}, onAggregationTypeChange = {}, onTargetChange = {}, onSave = {}, onDelete = {}
        )
    }
}