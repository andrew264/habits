package com.andrew264.habits.ui.schedule.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.repository.ScheduleRepository
import com.andrew264.habits.repository.SettingsRepository
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

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val schedules = scheduleRepository.getAllSchedules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiEvents = MutableSharedFlow<SchedulesUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private var lastDeletedSchedule: Schedule? = null

    fun onDeleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            // Prevent deletion if the schedule is assigned
            val currentSettings = settingsRepository.settingsFlow.first()
            val isSleepSchedule = currentSettings.selectedScheduleId == schedule.id
            val isWaterSchedule = currentSettings.waterReminderScheduleId == schedule.id

            if (schedule.id == DefaultSchedules.DEFAULT_SLEEP_SCHEDULE_ID) {
                _uiEvents.emit(
                    SchedulesUiEvent.ShowSnackbar(message = "The default schedule cannot be deleted.")
                )
                return@launch
            }

            if (isSleepSchedule || isWaterSchedule) {
                val usage = when {
                    isSleepSchedule && isWaterSchedule -> "sleep tracking and water reminders"
                    isSleepSchedule -> "sleep tracking"
                    else -> "water reminders"
                }
                _uiEvents.emit(
                    SchedulesUiEvent.ShowSnackbar(message = "Cannot delete schedule. It's assigned to $usage.")
                )
                return@launch
            }

            // Proceed with deletion
            lastDeletedSchedule = schedule
            scheduleRepository.deleteSchedule(schedule)
            _uiEvents.emit(
                SchedulesUiEvent.ShowSnackbar(
                    message = "'${schedule.name}' deleted",
                    actionLabel = "Undo"
                )
            )
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