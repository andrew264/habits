package com.andrew264.habits.ui.schedule.create

import androidx.lifecycle.SavedStateHandle
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

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val saveScheduleUseCase: SaveScheduleUseCase,
    private val scheduleEditor: ScheduleEditor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scheduleId: String? = savedStateHandle["scheduleId"]
    val isNewSchedule: Boolean = scheduleId == null

    private val _schedule = MutableStateFlow<Schedule?>(null)
    val schedule: StateFlow<Schedule?> = _schedule.asStateFlow()

    private val _viewMode = MutableStateFlow(ScheduleViewMode.GROUPED)
    val viewMode: StateFlow<ScheduleViewMode> = _viewMode.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ScheduleUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    val perDayRepresentation: StateFlow<Map<DayOfWeek, List<TimeRange>>> = schedule
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

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            if (scheduleId == null) {
                _schedule.value = createNewSchedule()
            } else {
                scheduleRepository.getSchedule(scheduleId).collect {
                    // Only set the value if it's the first time, to avoid overwriting user edits.
                    if (_schedule.value == null) {
                        _schedule.value = it ?: createNewSchedule(scheduleId)
                    }
                }
            }
        }
    }

    private fun createNewSchedule(id: String = UUID.randomUUID().toString()): Schedule {
        return Schedule(id = id, name = "", groups = emptyList())
    }

    // --- General Actions ---

    fun setViewMode(mode: ScheduleViewMode) {
        _viewMode.value = mode
    }

    fun saveSchedule() {
        val currentSchedule = _schedule.value ?: return
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

    // --- State Modification Actions ---
    fun updateScheduleName(name: String) {
        _schedule.update { it?.let { schedule -> scheduleEditor.updateScheduleName(schedule, name) } }
    }

    fun addGroup() {
        _schedule.update { it?.let { schedule -> scheduleEditor.addGroup(schedule) } }
    }

    fun deleteGroup(groupId: String) {
        _schedule.update { it?.let { schedule -> scheduleEditor.deleteGroup(schedule, groupId) } }
    }

    fun updateGroupName(
        groupId: String,
        newName: String
    ) {
        _schedule.update { it?.let { schedule -> scheduleEditor.updateGroupName(schedule, groupId, newName) } }
    }

    fun toggleDayInGroup(
        groupId: String,
        day: DayOfWeek
    ) {
        _schedule.update { it?.let { schedule -> scheduleEditor.toggleDayInGroup(schedule, groupId, day) } }
    }

    fun addTimeRangeToGroup(
        groupId: String,
        timeRange: TimeRange
    ) {
        _schedule.update { it?.let { schedule -> scheduleEditor.addTimeRangeToGroup(schedule, groupId, timeRange) } }
    }

    fun updateTimeRangeInGroup(
        groupId: String,
        old: TimeRange,
        new: TimeRange
    ) {
        _schedule.update { it?.let { schedule -> scheduleEditor.updateTimeRangeInGroup(schedule, groupId, old, new) } }
    }

    fun deleteTimeRangeFromGroup(
        groupId: String,
        timeRange: TimeRange
    ) {
        _schedule.update { it?.let { schedule -> scheduleEditor.deleteTimeRangeFromGroup(schedule, groupId, timeRange) } }
    }

    fun addTimeRangeToDay(
        day: DayOfWeek,
        timeRange: TimeRange
    ) {
        _schedule.update { it?.let { schedule -> scheduleEditor.addTimeRangeToDay(schedule, day, timeRange) } }
    }

    fun updateTimeRangeInDay(
        day: DayOfWeek,
        old: TimeRange,
        new: TimeRange
    ) {
        _schedule.value?.let { currentSchedule ->
            val result = scheduleEditor.updateTimeRangeInDay(currentSchedule, day, old, new)
            _schedule.value = result.schedule
            result.userMessage?.let { message ->
                viewModelScope.launch { _uiEvents.emit(ScheduleUiEvent.ShowSnackbar(message)) }
            }
        }
    }

    fun deleteTimeRangeFromDay(
        day: DayOfWeek,
        timeRange: TimeRange
    ) {
        _schedule.value?.let { currentSchedule ->
            val result = scheduleEditor.deleteTimeRangeFromDay(currentSchedule, day, timeRange)
            _schedule.value = result.schedule
            result.userMessage?.let { message ->
                viewModelScope.launch { _uiEvents.emit(ScheduleUiEvent.ShowSnackbar(message)) }
            }
        }
    }
}