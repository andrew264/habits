package com.andrew264.habits.ui.counters.editor

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.R
import com.andrew264.habits.domain.model.Counter
import com.andrew264.habits.domain.repository.CounterRepository
import com.andrew264.habits.model.counter.AggregationType
import com.andrew264.habits.model.counter.CounterType
import com.andrew264.habits.ui.common.SnackbarMessage
import com.andrew264.habits.ui.common.color_picker.utils.toHexCode
import com.andrew264.habits.util.SnackbarCommand
import com.andrew264.habits.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CounterEditorUiState(
    val isLoading: Boolean = true,
    val isNewCounter: Boolean = true,
    val showDeleteConfirmation: Boolean = false,
    val name: String = "",
    val colorHex: String = "#FF4CAF50",
    val type: CounterType = CounterType.NUMBER,
    val aggregationType: AggregationType = AggregationType.SUM,
    val target: String = "",
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val targetError: Int? = null
)

sealed interface CounterEditorUiEvent {
    object NavigateUp : CounterEditorUiEvent
}

@HiltViewModel
class CounterEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val counterRepository: CounterRepository,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CounterEditorUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<CounterEditorUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private var currentCounterId: String? = null

    fun initialize(counterId: String?) {
        if (currentCounterId == counterId && !_uiState.value.isLoading) return
        currentCounterId = counterId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (counterId == null) {
                _uiState.update {
                    it.copy(isLoading = false, isNewCounter = true)
                }
            } else {
                counterRepository.getCounterById(counterId).first()?.let { counter ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNewCounter = false,
                            name = counter.name,
                            colorHex = counter.colorHex,
                            type = counter.type,
                            aggregationType = counter.aggregationType,
                            target = counter.target?.toString() ?: ""
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName, nameError = null) }
    }

    fun onColorChange(newColor: Color) {
        _uiState.update { it.copy(colorHex = newColor.toHexCode(includeAlpha = true)) }
    }

    fun onTypeChange(newType: CounterType) {
        _uiState.update { it.copy(type = newType) }
    }

    fun onAggregationTypeChange(newAggregationType: AggregationType) {
        _uiState.update { it.copy(aggregationType = newAggregationType) }
    }

    fun onTargetChange(newTarget: String) {
        _uiState.update { it.copy(target = newTarget, targetError = null) }
    }

    fun onSave() {
        if (validate()) {
            viewModelScope.launch {
                val state = _uiState.value
                val counter = Counter(
                    id = currentCounterId ?: UUID.randomUUID().toString(),
                    name = state.name.trim(),
                    colorHex = state.colorHex,
                    type = state.type,
                    aggregationType = state.aggregationType,
                    target = state.target.toDoubleOrNull()
                )
                counterRepository.saveCounter(counter)
                snackbarManager.showMessage(SnackbarCommand(message = SnackbarMessage.FromResource(R.string.counter_saved_message)))
                _uiEvents.emit(CounterEditorUiEvent.NavigateUp)
            }
        }
    }

    fun onDelete() {
        if (!_uiState.value.isNewCounter && currentCounterId != null) {
            viewModelScope.launch {
                counterRepository.getCounterById(currentCounterId!!).first()?.let {
                    counterRepository.deleteCounter(it)
                    snackbarManager.showMessage(SnackbarCommand(message = SnackbarMessage.FromResource(R.string.counter_deleted_message)))
                    _uiEvents.emit(CounterEditorUiEvent.NavigateUp)
                }
            }
        }
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun onShowDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onDismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    private fun validate(): Boolean {
        var isValid = true
        if (_uiState.value.name.isBlank()) {
            _uiState.update { it.copy(nameError = R.string.counter_editor_name_error_blank) }
            isValid = false
        }
        if (_uiState.value.target.isNotBlank() && _uiState.value.target.toDoubleOrNull() == null) {
            _uiState.update { it.copy(targetError = R.string.counter_editor_target_error_invalid) }
            isValid = false
        }
        return isValid
    }
}