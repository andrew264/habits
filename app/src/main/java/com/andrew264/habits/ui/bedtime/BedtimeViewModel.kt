package com.andrew264.habits.ui.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.analyzer.ScheduleCoverage
import com.andrew264.habits.domain.model.TimelineSegment
import com.andrew264.habits.domain.usecase.GetBedtimeScreenDataUseCase
import com.andrew264.habits.domain.usecase.SetSleepScheduleUseCase
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class TimelineRange(
    val label: String,
    val durationMillis: Long
) {
    TWELVE_HOURS("12 Hr", TimeUnit.HOURS.toMillis(12)),
    DAY("1 Day", TimeUnit.DAYS.toMillis(1)),
    WEEK("7 Days", TimeUnit.DAYS.toMillis(7))
}

data class ScheduleInfo(
    val summary: String,
    val coverage: ScheduleCoverage
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BedtimeViewModel @Inject constructor(
    private val getBedtimeScreenDataUseCase: GetBedtimeScreenDataUseCase,
    private val setSleepScheduleUseCase: SetSleepScheduleUseCase
) : ViewModel() {

    private val _selectedTimelineRange = MutableStateFlow(TimelineRange.DAY)
    val selectedTimelineRange: StateFlow<TimelineRange> = _selectedTimelineRange.asStateFlow()

    private val _viewEndTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val viewEndTimeMillis: StateFlow<Long> = _viewEndTimeMillis

    val viewStartTimeMillis: StateFlow<Long> = _selectedTimelineRange.map { range ->
        _viewEndTimeMillis.value - range.durationMillis
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _viewEndTimeMillis.value - _selectedTimelineRange.value.durationMillis
    )

    val allSchedules: StateFlow<List<Schedule>> = getBedtimeScreenDataUseCase.allSchedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(DefaultSchedules.defaultSleepSchedule)
        )

    val selectedSchedule: StateFlow<Schedule> = getBedtimeScreenDataUseCase.selectedSchedule
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DefaultSchedules.defaultSleepSchedule
        )

    val scheduleInfo: StateFlow<ScheduleInfo?> = getBedtimeScreenDataUseCase.scheduleInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val timelineSegments: StateFlow<List<TimelineSegment>> = _selectedTimelineRange
        .onEach {
            _viewEndTimeMillis.value = System.currentTimeMillis()
        }
        .flatMapLatest { range ->
            getBedtimeScreenDataUseCase.getTimelineSegments(range)
        }
        .map { domainSegments ->
            domainSegments.map { it }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setTimelineRange(range: TimelineRange) {
        _selectedTimelineRange.value = range
    }

    fun selectSchedule(scheduleId: String) {
        viewModelScope.launch {
            setSleepScheduleUseCase.execute(scheduleId)
        }
    }
}