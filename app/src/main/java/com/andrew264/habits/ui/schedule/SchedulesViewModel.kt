package com.andrew264.habits.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.usecase.CheckScheduleInUseUseCase
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
    val isLoading: Boolean = true,
    val schedulePendingDeletion: Schedule? = null
)

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    scheduleRepository: ScheduleRepository,
    private val deleteScheduleUseCase: DeleteScheduleUseCase,
    private val checkScheduleInUseUseCase: CheckScheduleInUseUseCase
) : ViewModel() {

    private val _schedulePendingDeletion = MutableStateFlow<Schedule?>(null)

    val uiState: StateFlow<SchedulesUiState> =
        combine(scheduleRepository.getAllSchedules(), _schedulePendingDeletion) { schedules, pendingDeletion ->
            SchedulesUiState(
                schedules = schedules,
                isLoading = false,
                schedulePendingDeletion = pendingDeletion
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SchedulesUiState()
        )

    private val _uiEvents = MutableSharedFlow<SchedulesUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    suspend fun onDeleteSchedule(schedule: Schedule): Boolean {
        when (val checkResult = checkScheduleInUseUseCase.execute(schedule.id)) {
            is CheckScheduleInUseUseCase.Result.IsDefault -> {
                _uiEvents.emit(SchedulesUiEvent.ShowSnackbar("The default schedule cannot be deleted."))
                return false
            }

            is CheckScheduleInUseUseCase.Result.InUse -> {
                _uiEvents.emit(SchedulesUiEvent.ShowSnackbar(checkResult.usageMessage))
                return false
            }

            is CheckScheduleInUseUseCase.Result.NotInUse -> {
                _schedulePendingDeletion.value = schedule
                _uiEvents.emit(
                    SchedulesUiEvent.ShowSnackbar(
                        message = "'${schedule.name}' deleted",
                        actionLabel = "Undo"
                    )
                )
                return true
            }
        }
    }

    fun onUndoDelete() {
        _schedulePendingDeletion.value = null
    }

    fun onDeletionConfirmed() {
        viewModelScope.launch {
            _schedulePendingDeletion.value?.let {
                deleteScheduleUseCase.execute(it)
                _schedulePendingDeletion.value = null
            }
        }
    }
}