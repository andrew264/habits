package com.andrew264.habits.ui.counters.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.R
import com.andrew264.habits.domain.model.CounterLog
import com.andrew264.habits.domain.repository.CounterRepository
import com.andrew264.habits.domain.usecase.counter.CounterDetailsModel
import com.andrew264.habits.domain.usecase.counter.GetCounterDetailsUseCase
import com.andrew264.habits.ui.common.SnackbarMessage
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.util.SnackbarCommand
import com.andrew264.habits.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class CounterDetailUiState(
    val isLoading: Boolean = true,
    val details: CounterDetailsModel? = null,
    val newLogValue: String = "",
    val showDurationPicker: Boolean = false,
    val selectedChartIndex: Int? = null,
    val displayedLogs: List<CounterLog> = emptyList(),
    val selectedDateLabel: String = ""
)

private data class LocalUiState(
    val newLogValue: String = "",
    val showDurationPicker: Boolean = false,
    val selectedChartIndex: Int? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CounterDetailViewModel @Inject constructor(
    private val getCounterDetailsUseCase: GetCounterDetailsUseCase,
    private val counterRepository: CounterRepository,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    private val _counterId = MutableStateFlow<String?>(null)
    private val _localUiState = MutableStateFlow(LocalUiState())

    val uiState: StateFlow<CounterDetailUiState> = combine(
        _counterId.filterNotNull().flatMapLatest { id ->
            getCounterDetailsUseCase.execute(id)
        },
        _localUiState
    ) { details, local ->

        val chartEntries = details?.chartEntries ?: emptyList()
        val indexToUse = local.selectedChartIndex ?: (chartEntries.size - 1).takeIf { it >= 0 }
        val selectedEntry = indexToUse?.let { chartEntries.getOrNull(it) }

        val displayedLogs = if (selectedEntry != null && selectedEntry.timestamp != null && details != null) {
            val selectedLocalDate = Instant.ofEpochMilli(selectedEntry.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            details.allLogs.filter {
                Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() == selectedLocalDate
            }.sortedByDescending { it.timestamp }
        } else {
            emptyList()
        }

        CounterDetailUiState(
            isLoading = false,
            details = details,
            newLogValue = local.newLogValue,
            showDurationPicker = local.showDurationPicker,
            selectedChartIndex = indexToUse,
            displayedLogs = displayedLogs,
            selectedDateLabel = selectedEntry?.timestamp?.let { FormatUtils.formatShortDateLocaleAware(it) } ?: ""
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

    fun onChartEntrySelected(index: Int?) {
        _localUiState.update { it.copy(selectedChartIndex = index) }
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