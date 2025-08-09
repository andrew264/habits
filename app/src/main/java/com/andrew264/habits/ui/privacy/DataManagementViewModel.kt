package com.andrew264.habits.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.usecase.DeletableDataType
import com.andrew264.habits.domain.usecase.DeleteDataUseCase
import com.andrew264.habits.domain.usecase.TimeRangeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

sealed interface DataManagementEvent {
    data class ShowSnackbar(val message: String) : DataManagementEvent
}

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val deleteDataUseCase: DeleteDataUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DataManagementEvent>()
    val events = _events.asSharedFlow()

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
                _events.emit(DataManagementEvent.ShowSnackbar("Data deleted successfully."))
            } catch (_: Exception) {
                _events.emit(DataManagementEvent.ShowSnackbar("Error deleting data."))
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun onDismissConfirmation() {
        _uiState.update { it.copy(showConfirmationDialog = false) }
    }
}