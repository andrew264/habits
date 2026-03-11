package com.andrew264.habits.ui.counters.detail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.R
import com.andrew264.habits.domain.model.CounterLog
import com.andrew264.habits.domain.repository.CounterRepository
import com.andrew264.habits.domain.usecase.counter.CounterDetailsModel
import com.andrew264.habits.domain.usecase.counter.GetCounterDetailsUseCase
import com.andrew264.habits.ui.common.SnackbarMessage
import com.andrew264.habits.util.SnackbarCommand
import com.andrew264.habits.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChartTimeRange(@param:StringRes val label: Int) {
    WEEK(R.string.time_range_7_days),
    MONTH(R.string.time_range_30_days)
}

data class CounterDetailUiState(
    val isLoading: Boolean = true,
    val details: CounterDetailsModel? = null,
    val selectedRange: ChartTimeRange = ChartTimeRange.WEEK,
    val newLogValue: String = "",
    val showDurationPicker: Boolean = false
)

private data class LocalUiState(
    val newLogValue: String = "",
    val showDurationPicker: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CounterDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCounterDetailsUseCase: GetCounterDetailsUseCase,
    private val counterRepository: CounterRepository,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    private val _counterId = MutableStateFlow<String?>(null)
    private val _selectedRange = MutableStateFlow(ChartTimeRange.WEEK)
    private val _localUiState = MutableStateFlow(LocalUiState())

    val uiState: StateFlow<CounterDetailUiState> = combine(
        _counterId.filterNotNull().flatMapLatest { id ->
            _selectedRange.flatMapLatest { range ->
                getCounterDetailsUseCase.execute(id, range).map { it to range }
            }
        },
        _localUiState
    ) { (details, range), local ->
        CounterDetailUiState(
            isLoading = false,
            details = details,
            selectedRange = range,
            newLogValue = local.newLogValue,
            showDurationPicker = local.showDurationPicker
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CounterDetailUiState()
    )

    fun initialize(id: String) {
        if (_counterId.value == id) return
        _counterId.value = id
    }

    fun setTimeRange(range: ChartTimeRange) {
        _selectedRange.value = range
    }

    fun onNewLogValueChange(value: String) {
        _localUiState.update { it.copy(newLogValue = value) }
    }

    fun addLog(value: Double) {
        val id = _counterId.value ?: return
        viewModelScope.launch {
            val log = CounterLog(
                counterId = id,
                timestamp = System.currentTimeMillis(),
                value = value
            )
            counterRepository.addLog(log)
            _localUiState.update { it.copy(newLogValue = "", showDurationPicker = false) }
        }
    }

    fun deleteLog(log: CounterLog) {
        viewModelScope.launch {
            counterRepository.deleteLog(log)
            snackbarManager.showMessage(
                SnackbarCommand(
                    message = SnackbarMessage.FromResource(R.string.counter_detail_log_deleted),
                    actionLabel = SnackbarMessage.FromResource(R.string.counter_detail_undo),
                    onAction = { undoDelete(log) }
                )
            )
        }
    }

    private fun undoDelete(log: CounterLog) {
        viewModelScope.launch {
            counterRepository.addLog(log)
        }
    }

    fun onShowDurationPicker() {
        _localUiState.update { it.copy(showDurationPicker = true) }
    }

    fun onDismissDurationPicker() {
        _localUiState.update { it.copy(showDurationPicker = false) }
    }
}