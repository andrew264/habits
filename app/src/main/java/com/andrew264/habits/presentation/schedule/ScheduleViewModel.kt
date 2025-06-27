package com.andrew264.habits.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.data.repository.ScheduleRepository
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
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
    private val scheduleRepository: ScheduleRepository
    // private val savedStateHandle: SavedStateHandle // To get scheduleId from navigation
) : ViewModel() {

    private val _schedule = MutableStateFlow<Schedule?>(null)
    val schedule: StateFlow<Schedule?> = _schedule.asStateFlow()

    private val _viewMode = MutableStateFlow(ScheduleViewMode.GROUPED)
    val viewMode: StateFlow<ScheduleViewMode> = _viewMode.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ScheduleUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    val perDayRepresentation: StateFlow<Map<DayOfWeek, List<TimeRange>>> = schedule
        .map { currentSchedule ->
            if (currentSchedule == null) return@map emptyMap()

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

    fun loadSchedule(id: String?) {
        viewModelScope.launch {
            if (id == null) {
                _schedule.value = createNewSchedule()
            } else {
                scheduleRepository.getSchedule(id).collect {
                    _schedule.value = it ?: createNewSchedule(id)
                }
            }
        }
    }

    private fun createNewSchedule(id: String = UUID.randomUUID().toString()): Schedule {
        return Schedule(id = id, name = "New Schedule", groups = emptyList())
    }

    // --- General Actions ---

    fun setViewMode(mode: ScheduleViewMode) {
        _viewMode.value = mode
    }

    fun updateScheduleName(name: String) {
        _schedule.update { it?.copy(name = name) }
    }

    fun saveSchedule() {
        val currentSchedule = _schedule.value ?: return
        viewModelScope.launch {
            scheduleRepository.saveSchedule(currentSchedule)
            _uiEvents.emit(ScheduleUiEvent.ShowSnackbar("Schedule saved!"))
            _uiEvents.emit(ScheduleUiEvent.NavigateUp)
        }
    }

    // --- Group Management ---

    fun addGroup() {
        val newGroup = ScheduleGroup(
            id = UUID.randomUUID().toString(),
            name = "New Group",
            days = emptySet(),
            timeRanges = emptyList()
        )
        _schedule.update { it?.copy(groups = it.groups + newGroup) }
    }

    fun deleteGroup(groupId: String) {
        _schedule.update { currentSchedule ->
            currentSchedule?.copy(
                groups = currentSchedule.groups.filterNot { it.id == groupId }
            )
        }
    }

    fun updateGroupName(
        groupId: String,
        newName: String
    ) {
        _schedule.update { currentSchedule ->
            currentSchedule?.copy(
                groups = currentSchedule.groups.map { group ->
                    if (group.id == groupId) group.copy(name = newName) else group
                }
            )
        }
    }

    fun toggleDayInGroup(
        groupId: String,
        day: DayOfWeek
    ) {
        _schedule.update { currentSchedule ->
            currentSchedule?.copy(
                groups = currentSchedule.groups.map { group ->
                    if (group.id == groupId) {
                        val newDays = if (day in group.days) {
                            group.days - day
                        } else {
                            group.days + day
                        }
                        group.copy(days = newDays)
                    } else {
                        group
                    }
                }
            )
        }
    }

    fun addTimeRangeToGroup(
        groupId: String,
        timeRange: TimeRange
    ) {
        _schedule.update { currentSchedule ->
            currentSchedule?.copy(
                groups = currentSchedule.groups.map { group ->
                    if (group.id == groupId) {
                        group.copy(timeRanges = group.timeRanges + timeRange)
                    } else {
                        group
                    }
                }
            )
        }
    }

    fun updateTimeRangeInGroup(
        groupId: String,
        old: TimeRange,
        new: TimeRange
    ) {
        _schedule.update { currentSchedule ->
            currentSchedule?.copy(
                groups = currentSchedule.groups.map { group ->
                    if (group.id == groupId) {
                        group.copy(
                            timeRanges = group.timeRanges.map { if (it == old) new else it }
                                .sortedBy { it.fromMinuteOfDay }
                        )
                    } else {
                        group
                    }
                }
            )
        }
    }

    fun deleteTimeRangeFromGroup(
        groupId: String,
        timeRange: TimeRange
    ) {
        _schedule.update { currentSchedule ->
            currentSchedule?.copy(
                groups = currentSchedule.groups.map { group ->
                    if (group.id == groupId) {
                        group.copy(timeRanges = group.timeRanges - timeRange)
                    } else {
                        group
                    }
                }
            )
        }
    }

    // --- Per-Day View Logic ---

    fun addTimeRangeToDay(
        day: DayOfWeek,
        timeRange: TimeRange
    ) {
        _schedule.update { currentSchedule ->
            currentSchedule ?: return@update null

            // Try to find an existing group that *only* contains this day
            val singleDayGroup = currentSchedule.groups.find { it.days == setOf(day) }

            if (singleDayGroup != null) {
                // Add to existing single-day group
                currentSchedule.copy(
                    groups = currentSchedule.groups.map { group ->
                        if (group.id == singleDayGroup.id) {
                            group.copy(
                                timeRanges = (group.timeRanges + timeRange).sortedBy { it.fromMinuteOfDay })
                        } else {
                            group
                        }
                    }
                )
            } else {
                // Create a new group for this day
                val newGroup = ScheduleGroup(
                    id = UUID.randomUUID().toString(),
                    name = day.name.lowercase().replaceFirstChar { it.titlecase() },
                    days = setOf(day),
                    timeRanges = listOf(timeRange)
                )
                currentSchedule.copy(groups = currentSchedule.groups + newGroup)
            }
        }
    }

    fun updateTimeRangeInDay(
        day: DayOfWeek,
        old: TimeRange,
        new: TimeRange
    ) {
        _schedule.update { currentSchedule ->
            currentSchedule ?: return@update null

            val sourceGroup = currentSchedule.groups.find { day in it.days && old in it.timeRanges }
            sourceGroup ?: return@update currentSchedule

            // Case 1: Simple update, no un-grouping needed.
            if (sourceGroup.days.size == 1) {
                return@update currentSchedule.copy(
                    groups = currentSchedule.groups.map { group ->
                        if (group.id == sourceGroup.id) {
                            group.copy(
                                timeRanges = group.timeRanges.map { if (it == old) new else it }
                                    .sortedBy { it.fromMinuteOfDay })
                        } else {
                            group
                        }
                    }
                )
            }

            // Case 2: Un-grouping is required.
            viewModelScope.launch {
                _uiEvents.emit(
                    ScheduleUiEvent.ShowSnackbar(
                        "${
                            day.name.lowercase().replaceFirstChar { it.titlecase() }
                        } schedule is now separate from '${sourceGroup.name}' group."
                    )
                )
            }

            // New group for the separated day with the updated time range.
            val newGroupForDay = ScheduleGroup(
                id = UUID.randomUUID().toString(),
                name = day.name.lowercase().replaceFirstChar { it.titlecase() },
                days = setOf(day),
                timeRanges = sourceGroup.timeRanges.map { if (it == old) new else it }
                    .sortedBy { it.fromMinuteOfDay }
            )

            // Original group no longer contains the day.
            val updatedOriginalGroup = sourceGroup.copy(days = sourceGroup.days - day)

            val newGroupsList = currentSchedule.groups
                .filterNot { it.id == sourceGroup.id } + updatedOriginalGroup + newGroupForDay

            currentSchedule.copy(groups = newGroupsList.filterNot { it.days.isEmpty() })
        }
    }

    /**
     * The core "un-grouping" logic lives here.
     */
    fun deleteTimeRangeFromDay(
        day: DayOfWeek,
        timeRange: TimeRange
    ) {
        _schedule.update { currentSchedule ->
            currentSchedule ?: return@update null

            // Find the group that contains this day AND this time range
            val sourceGroup = currentSchedule.groups.find { day in it.days && timeRange in it.timeRanges }
            sourceGroup ?: return@update currentSchedule // Should not happen if UI is correct

            // Case 1: The group only affects this day. We can safely modify it.
            if (sourceGroup.days.size == 1) {
                return@update currentSchedule.copy(
                    groups = currentSchedule.groups.map { group ->
                        if (group.id == sourceGroup.id) {
                            group.copy(timeRanges = group.timeRanges - timeRange)
                        } else {
                            group
                        }
                    }
                )
            }

            // Case 2: The group affects multiple days. We must un-group.
            viewModelScope.launch {
                _uiEvents.emit(
                    ScheduleUiEvent.ShowSnackbar(
                        "${
                            day.name.lowercase().replaceFirstChar { it.titlecase() }
                        } schedule is now separate from '${sourceGroup.name}' group."
                    )
                )
            }

            // Create a new group for the modified day
            val newGroupForDay = ScheduleGroup(
                id = UUID.randomUUID().toString(),
                name = day.name.lowercase().replaceFirstChar { it.titlecase() },
                days = setOf(day),
                timeRanges = sourceGroup.timeRanges - timeRange // All original times except the deleted one
            )

            // Update the original group to no longer include the day
            val updatedOriginalGroup = sourceGroup.copy(days = sourceGroup.days - day)

            val newGroupsList = currentSchedule.groups
                .filterNot { it.id == sourceGroup.id } + updatedOriginalGroup + newGroupForDay

            currentSchedule.copy(groups = newGroupsList)
        }
    }
}