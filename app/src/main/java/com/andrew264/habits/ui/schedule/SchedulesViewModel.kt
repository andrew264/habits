package com.andrew264.habits.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
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
    private val scheduleRepository: ScheduleRepository
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
            lastDeletedSchedule = schedule
            scheduleRepository.deleteSchedule(schedule)
            _uiEvents.emit(
                SchedulesUiEvent.ShowSnackbar(
                    message = "${schedule.name} deleted",
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