package com.andrew264.habits.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.R
import com.andrew264.habits.domain.usecase.DeletableDataType
import com.andrew264.habits.domain.usecase.DeleteDataUseCase
import com.andrew264.habits.domain.usecase.TimeRangeOption
import com.andrew264.habits.ui.common.SnackbarMessage
import com.andrew264.habits.util.SnackbarCommand
import com.andrew264.habits.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataManagementUiState(
    val selectedTimeRange: TimeRangeOption = TimeRangeOption.LAST_HOUR,
    val selectedDataTypes: Set<DeletableDataType> = setOf(
        DeletableDataType.SLEEP,
        DeletableDataType.WATER,
        DeletableDataType.USAGE
    ),
    val isDeleting: Boolean = false,
    val showConfirmationDialog: Boolean = false,
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val deleteDataUseCase: DeleteDataUseCase,
    private val snackbarManager: SnackbarManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState = _uiState.asStateFlow()

    fun selectTimeRange(timeRange: TimeRangeOption) {
        _uiState.update { it.copy(selectedTimeRange = timeRange) }
    }

    fun toggleDataType(dataType: DeletableDataType) {
        _uiState.update {
            val newSelection = if (dataType in it.selectedDataTypes) {
                it.selectedDataTypes - dataType
            } else {
                it.selectedDataTypes + dataType
            }
            it.copy(selectedDataTypes = newSelection)
        }
    }

    fun onDeleteClicked() {
        _uiState.update { it.copy(showConfirmationDialog = true) }
    }

    fun onDeleteConfirmed() {
        _uiState.update { it.copy(isDeleting = true, showConfirmationDialog = false) }
        viewModelScope.launch {
            try {
                deleteDataUseCase.execute(
                    dataTypes = uiState.value.selectedDataTypes,
                    timeRange = uiState.value.selectedTimeRange
                )
                snackbarManager.showMessage(SnackbarCommand(message = SnackbarMessage.FromResource(R.string.data_management_data_deleted_successfully)))
            } catch (_: Exception) {
                snackbarManager.showMessage(SnackbarCommand(message = SnackbarMessage.FromResource(R.string.data_management_error_deleting_data)))
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun onDismissConfirmation() {
        _uiState.update { it.copy(showConfirmationDialog = false) }
    }
}