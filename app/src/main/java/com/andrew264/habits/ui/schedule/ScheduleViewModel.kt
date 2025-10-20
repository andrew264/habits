package com.andrew264.habits.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.R
import com.andrew264.habits.domain.analyzer.ScheduleAnalyzer
import com.andrew264.habits.domain.analyzer.ScheduleCoverage
import com.andrew264.habits.domain.editor.ScheduleEditor
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.usecase.SaveScheduleUseCase
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.TimeRange
import com.andrew264.habits.ui.common.SnackbarMessage
import com.andrew264.habits.util.SnackbarCommand
import com.andrew264.habits.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ScheduleViewMode {
    GROUPED,
    PER_DAY
}

sealed interface ScheduleUiEvent {
    object NavigateUp : ScheduleUiEvent
}

data class ScheduleEditorUiState(
    val schedule: Schedule? = null,
    val viewMode: ScheduleViewMode = ScheduleViewMode.GROUPED,
    val isNewSchedule: Boolean = true,
    val isLoading: Boolean = true,
    val scheduleCoverage: ScheduleCoverage? = null,
    val perDayRepresentation: Map<DayOfWeek, List<TimeRange>> = emptyMap()
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val saveScheduleUseCase: SaveScheduleUseCase,
    private val scheduleEditor: ScheduleEditor,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    private val _internalState = MutableStateFlow(ScheduleEditorUiState())

    val uiState: StateFlow<ScheduleEditorUiState> = _internalState.map { state ->
        val perDayRep = state.schedule?.let { schedule ->
            DayOfWeek.entries.associateWith { day ->
                schedule.groups
                    .filter { group -> day in group.days }
                    .flatMap { group -> group.timeRanges }
                    .sortedBy { it.fromMinuteOfDay }
            }
        } ?: emptyMap()
        state.copy(perDayRepresentation = perDayRep)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleEditorUiState()
    )

    private val _uiEvents = MutableSharedFlow<ScheduleUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private var currentScheduleId: String? = null

    fun initialize(scheduleId: String?) {
        // Avoid re-initializing if the ID is the same and we are not in a loading state.
        if (scheduleId == currentScheduleId && !_internalState.value.isLoading) return
        currentScheduleId = scheduleId

        viewModelScope.launch {
            _internalState.value = ScheduleEditorUiState(isLoading = true) // Reset state
            if (scheduleId == null) {
                // This case is now a fallback, the FAB should always provide an ID.
                val newSchedule = createNewSchedule()
                _internalState.value = ScheduleEditorUiState(
                    schedule = newSchedule,
                    isNewSchedule = true,
                    isLoading = false,
                    scheduleCoverage = ScheduleAnalyzer(newSchedule.groups).calculateCoverage()
                )
            } else {
                // Fetch the schedule once to determine if it's new or existing.
                val existingSchedule = scheduleRepository.getSchedule(scheduleId).first()
                if (existingSchedule != null) {
                    // It's an existing schedule, load it for editing.
                    _internalState.value = ScheduleEditorUiState(
                        schedule = existingSchedule,
                        isNewSchedule = false,
                        isLoading = false,
                        scheduleCoverage = ScheduleAnalyzer(existingSchedule.groups).calculateCoverage()
                    )
                } else {
                    // It's a new schedule, create it with the provided ID.
                    val newSchedule = createNewSchedule(scheduleId)
                    _internalState.value = ScheduleEditorUiState(
                        schedule = newSchedule,
                        isNewSchedule = true,
                        isLoading = false,
                        scheduleCoverage = ScheduleAnalyzer(newSchedule.groups).calculateCoverage()
                    )
                }
            }
        }
    }


    private fun createNewSchedule(id: String = UUID.randomUUID().toString()): Schedule {
        return Schedule(id = id, name = "", groups = emptyList())
    }

    fun setViewMode(mode: ScheduleViewMode) {
        _internalState.update { it.copy(viewMode = mode) }
    }

    fun saveSchedule() {
        val currentSchedule = _internalState.value.schedule ?: return
        viewModelScope.launch {
            when (val result = saveScheduleUseCase.execute(currentSchedule)) {
                is SaveScheduleUseCase.Result.Success -> {
                    snackbarManager.showMessage(SnackbarCommand(message = SnackbarMessage.FromResource(R.string.schedule_view_model_schedule_saved)))
                    _uiEvents.emit(ScheduleUiEvent.NavigateUp)
                }

                is SaveScheduleUseCase.Result.Failure -> {
                    snackbarManager.showMessage(SnackbarCommand(message = SnackbarMessage.FromString(result.message)))
                }
            }
        }
    }

    private fun updateSchedule(transform: (Schedule) -> Schedule) {
        _internalState.update { state ->
            state.schedule?.let {
                val newSchedule = transform(it)
                val analyzer = ScheduleAnalyzer(newSchedule.groups)
                state.copy(
                    schedule = newSchedule,
                    scheduleCoverage = analyzer.calculateCoverage()
                )
            } ?: state
        }
    }

    fun updateScheduleName(name: String) {
        updateSchedule { scheduleEditor.updateScheduleName(it, name) }
    }

    fun addGroup() {
        updateSchedule { scheduleEditor.addGroup(it) }
    }

    fun deleteGroup(groupId: String) {
        updateSchedule { scheduleEditor.deleteGroup(it, groupId) }
    }

    fun updateGroupName(
        groupId: String,
        newName: String
    ) {
        updateSchedule { scheduleEditor.updateGroupName(it, groupId, newName) }
    }

    fun toggleDayInGroup(
        groupId: String,
        day: DayOfWeek
    ) {
        updateSchedule { scheduleEditor.toggleDayInGroup(it, groupId, day) }
    }

    fun addTimeRangeToGroup(
        groupId: String,
        timeRange: TimeRange
    ) {
        updateSchedule { scheduleEditor.addTimeRangeToGroup(it, groupId, timeRange) }
    }

    fun updateTimeRangeInGroup(
        groupId: String,
        updatedTimeRange: TimeRange
    ) {
        updateSchedule { scheduleEditor.updateTimeRangeInGroup(it, groupId, updatedTimeRange) }
    }

    fun deleteTimeRangeFromGroup(
        groupId: String,
        timeRange: TimeRange
    ) {
        updateSchedule { scheduleEditor.deleteTimeRangeFromGroup(it, groupId, timeRange) }
    }

    fun addTimeRangeToDay(
        day: DayOfWeek,
        timeRange: TimeRange
    ) {
        updateSchedule { scheduleEditor.addTimeRangeToDay(it, day, timeRange) }
    }

    fun updateTimeRangeInDay(
        day: DayOfWeek,
        updatedTimeRange: TimeRange
    ) {
        _internalState.value.schedule?.let { currentSchedule ->
            val result = scheduleEditor.updateTimeRangeInDay(currentSchedule, day, updatedTimeRange)
            val analyzer = ScheduleAnalyzer(result.schedule.groups)
            _internalState.update {
                it.copy(
                    schedule = result.schedule,
                    scheduleCoverage = analyzer.calculateCoverage()
                )
            }
            result.userMessage?.let { message ->
                viewModelScope.launch { snackbarManager.showMessage(SnackbarCommand(message = SnackbarMessage.FromString(message))) }
            }
        }
    }

    fun deleteTimeRangeFromDay(
        day: DayOfWeek,
        timeRange: TimeRange
    ) {
        _internalState.value.schedule?.let { currentSchedule ->
            val result = scheduleEditor.deleteTimeRangeFromDay(currentSchedule, day, timeRange)
            val analyzer = ScheduleAnalyzer(result.schedule.groups)
            _internalState.update {
                it.copy(
                    schedule = result.schedule,
                    scheduleCoverage = analyzer.calculateCoverage()
                )
            }
            result.userMessage?.let { message ->
                viewModelScope.launch { snackbarManager.showMessage(SnackbarCommand(message = SnackbarMessage.FromString(message))) }
            }
        }
    }
}