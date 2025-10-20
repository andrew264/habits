package com.andrew264.habits.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.R
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.usecase.CheckScheduleInUseUseCase
import com.andrew264.habits.domain.usecase.DeleteScheduleUseCase
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.SnackbarMessage
import com.andrew264.habits.util.SnackbarCommand
import com.andrew264.habits.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchedulesUiState(
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = true,
    val schedulePendingDeletion: Schedule? = null
)

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    scheduleRepository: ScheduleRepository,
    private val deleteScheduleUseCase: DeleteScheduleUseCase,
    private val checkScheduleInUseUseCase: CheckScheduleInUseUseCase,
    private val snackbarManager: SnackbarManager
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

    suspend fun onDeleteSchedule(schedule: Schedule): Boolean {
        when (val checkResult = checkScheduleInUseUseCase.execute(schedule.id)) {
            is CheckScheduleInUseUseCase.Result.IsDefault -> {
                snackbarManager.showMessage(
                    SnackbarCommand(
                        message = SnackbarMessage.FromResource(R.string.schedules_view_model_default_schedule_delete_error)
                    )
                )
                return false
            }

            is CheckScheduleInUseUseCase.Result.InUse -> {
                snackbarManager.showMessage(
                    SnackbarCommand(
                        message = SnackbarMessage.FromString(checkResult.usageMessage)
                    )
                )
                return false
            }

            is CheckScheduleInUseUseCase.Result.NotInUse -> {
                _schedulePendingDeletion.value = schedule
                snackbarManager.showMessage(
                    SnackbarCommand(
                        message = SnackbarMessage.FromResource(
                            R.string.schedules_view_model_schedule_deleted,
                            formatArgs = listOf(schedule.name)
                        ),
                        actionLabel = SnackbarMessage.FromResource(R.string.schedules_view_model_undo),
                        onAction = { onUndoDelete() },
                        onDismiss = { onDeletionConfirmed() }
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