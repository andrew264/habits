package com.andrew264.habits.ui.schedule.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.usecase.DeleteScheduleUseCase
import com.andrew264.habits.model.schedule.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SchedulesUiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null
    ) : SchedulesUiEvent
}

data class SchedulesUiState(
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val deleteScheduleUseCase: DeleteScheduleUseCase
) : ViewModel() {

    val uiState: StateFlow<SchedulesUiState> = scheduleRepository.getAllSchedules()
        .map { schedules -> SchedulesUiState(schedules = schedules, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SchedulesUiState()
        )

    private val _uiEvents = MutableSharedFlow<SchedulesUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private var lastDeletedSchedule: Schedule? = null

    fun onDeleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            when (val result = deleteScheduleUseCase.execute(schedule)) {
                is DeleteScheduleUseCase.Result.Success -> {
                    lastDeletedSchedule = schedule
                    _uiEvents.emit(
                        SchedulesUiEvent.ShowSnackbar(
                            message = "'${schedule.name}' deleted",
                            actionLabel = "Undo"
                        )
                    )
                }

                is DeleteScheduleUseCase.Result.Failure -> {
                    _uiEvents.emit(
                        SchedulesUiEvent.ShowSnackbar(message = result.message)
                    )
                }
            }
        }
    }

    fun onUndoDelete() {
        viewModelScope.launch {
            lastDeletedSchedule?.let {
                scheduleRepository.saveSchedule(it)
                lastDeletedSchedule = null
            }
        }
    }
}