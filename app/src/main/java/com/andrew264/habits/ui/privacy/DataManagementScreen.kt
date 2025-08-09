package com.andrew264.habits.ui.privacy

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.domain.usecase.DeletableDataType
import com.andrew264.habits.domain.usecase.TimeRangeOption
import com.andrew264.habits.ui.privacy.components.DataTypeItem
import com.andrew264.habits.ui.privacy.components.DeleteConfirmationDialog
import com.andrew264.habits.ui.privacy.components.TimeRangeRow
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DataManagementScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: DataManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DataManagementEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    if (uiState.showConfirmationDialog) {
        DeleteConfirmationDialog(
            timeRange = uiState.selectedTimeRange,
            onDismiss = viewModel::onDismissConfirmation,
            onConfirm = viewModel::onDeleteConfirmed
        )
    }

    DataManagementScreenContent(
        uiState = uiState,
        onSelectTimeRange = viewModel::selectTimeRange,
        onToggleDataType = viewModel::toggleDataType,
        onDeleteClicked = viewModel::onDeleteClicked
    )
}

@Composable
private fun DataManagementScreenContent(
    uiState: DataManagementUiState,
    onSelectTimeRange: (TimeRangeOption) -> Unit,
    onToggleDataType: (DeletableDataType) -> Unit,
    onDeleteClicked: () -> Unit,
) {
    val view = LocalView.current

    Scaffold(
        bottomBar = {
            Surface {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onDeleteClicked()
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        },
                        enabled = uiState.selectedDataTypes.isNotEmpty() && !uiState.isDeleting,
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Delete Data")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = Dimens.PaddingLarge)
        ) {
            item {
                TimeRangeRow(
                    selected = uiState.selectedTimeRange,
                    onSelected = onSelectTimeRange
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingLarge), color = Color(0x00000000))
            }
            item {
                DataTypeItem(
                    title = "Sleep History",
                    description = "Data from Sleep API and bedtime schedules.",
                    icon = Icons.Outlined.Bedtime,
                    checked = DeletableDataType.SLEEP in uiState.selectedDataTypes,
                    onToggle = { onToggleDataType(DeletableDataType.SLEEP) }
                )
            }
            item {
                DataTypeItem(
                    title = "Water Intake History",
                    description = "All logged water entries.",
                    icon = Icons.Outlined.WaterDrop,
                    checked = DeletableDataType.WATER in uiState.selectedDataTypes,
                    onToggle = { onToggleDataType(DeletableDataType.WATER) }
                )
            }
            item {
                DataTypeItem(
                    title = "App & Screen Usage History",
                    description = "Screen on/off times and foreground app data.",
                    icon = Icons.Outlined.Timeline,
                    checked = DeletableDataType.USAGE in uiState.selectedDataTypes,
                    onToggle = { onToggleDataType(DeletableDataType.USAGE) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataManagementScreenContentPreview() {
    HabitsTheme {
        DataManagementScreenContent(
            uiState = DataManagementUiState(
                selectedTimeRange = TimeRangeOption.LAST_7_DAYS,
                selectedDataTypes = setOf(
                    DeletableDataType.SLEEP,
                    DeletableDataType.USAGE
                ),
                isDeleting = false,
                showConfirmationDialog = false
            ),
            onSelectTimeRange = {},
            onToggleDataType = {},
            onDeleteClicked = {}
        )
    }
}

@Preview(showBackground = true, name = "Deleting State")
@Composable
private fun DataManagementScreenContentDeletingPreview() {
    HabitsTheme {
        DataManagementScreenContent(
            uiState = DataManagementUiState(
                isDeleting = true
            ),
            onSelectTimeRange = {},
            onToggleDataType = {},
            onDeleteClicked = {}
        )
    }
}