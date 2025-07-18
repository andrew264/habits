package com.andrew264.habits.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.editor.ScheduleEditor
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.usecase.SaveScheduleUseCase
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.TimeRange
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
    data class ShowSnackbar(val message: String) : ScheduleUiEvent
    object NavigateUp : ScheduleUiEvent
}

data class ScheduleEditorUiState(
    val schedule: Schedule? = null,
    val viewMode: ScheduleViewMode = ScheduleViewMode.GROUPED,
    val isNewSchedule: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val saveScheduleUseCase: SaveScheduleUseCase,
    private val scheduleEditor: ScheduleEditor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleEditorUiState())
    val uiState: StateFlow<ScheduleEditorUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ScheduleUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private var currentScheduleId: String? = null

    val perDayRepresentation: StateFlow<Map<DayOfWeek, List<TimeRange>>> = uiState
        .map { it.schedule }
        .filterNotNull()
        .map { currentSchedule ->
            DayOfWeek.entries.associateWith { day ->
                currentSchedule.groups
                    .filter { group -> day in group.days }
                    .flatMap { group -> group.timeRanges }
                    .sortedBy { it.fromMinuteOfDay }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun initialize(scheduleId: String?) {
        // Avoid re-initializing if the ID is the same and we are not in a loading state.
        if (scheduleId == currentScheduleId && !_uiState.value.isLoading) return
        currentScheduleId = scheduleId

        viewModelScope.launch {
            _uiState.value = ScheduleEditorUiState(isLoading = true) // Reset state
            if (scheduleId == null) {
                // This case is now a fallback, the FAB should always provide an ID.
                _uiState.value = ScheduleEditorUiState(
                    schedule = createNewSchedule(),
                    isNewSchedule = true,
                    isLoading = false
                )
            } else {
                // Fetch the schedule once to determine if it's new or existing.
                val existingSchedule = scheduleRepository.getSchedule(scheduleId).first()
                if (existingSchedule != null) {
                    // It's an existing schedule, load it for editing.
                    _uiState.value = ScheduleEditorUiState(
                        schedule = existingSchedule,
                        isNewSchedule = false,
                        isLoading = false
                    )
                } else {
                    // It's a new schedule, create it with the provided ID.
                    _uiState.value = ScheduleEditorUiState(
                        schedule = createNewSchedule(scheduleId),
                        isNewSchedule = true,
                        isLoading = false
                    )
                }
            }
        }
    }


    private fun createNewSchedule(id: String = UUID.randomUUID().toString()): Schedule {
        return Schedule(id = id, name = "", groups = emptyList())
    }

    fun setViewMode(mode: ScheduleViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun saveSchedule() {
        val currentSchedule = _uiState.value.schedule ?: return
        viewModelScope.launch {
            when (val result = saveScheduleUseCase.execute(currentSchedule)) {
                is SaveScheduleUseCase.Result.Success -> {
                    _uiEvents.emit(ScheduleUiEvent.ShowSnackbar("Schedule saved!"))
                    _uiEvents.emit(ScheduleUiEvent.NavigateUp)
                }

                is SaveScheduleUseCase.Result.Failure -> {
                    _uiEvents.emit(ScheduleUiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    private fun updateSchedule(transform: (Schedule) -> Schedule) {
        _uiState.update { state ->
            state.schedule?.let {
                state.copy(schedule = transform(it))
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
        _uiState.value.schedule?.let { currentSchedule ->
            val result = scheduleEditor.updateTimeRangeInDay(currentSchedule, day, updatedTimeRange)
            _uiState.update { it.copy(schedule = result.schedule) }
            result.userMessage?.let { message ->
                viewModelScope.launch { _uiEvents.emit(ScheduleUiEvent.ShowSnackbar(message)) }
            }
        }
    }

    fun deleteTimeRangeFromDay(
        day: DayOfWeek,
        timeRange: TimeRange
    ) {
        _uiState.value.schedule?.let { currentSchedule ->
            val result = scheduleEditor.deleteTimeRangeFromDay(currentSchedule, day, timeRange)
            _uiState.update { it.copy(schedule = result.schedule) }
            result.userMessage?.let { message ->
                viewModelScope.launch { _uiEvents.emit(ScheduleUiEvent.ShowSnackbar(message)) }
            }
        }
    }
}