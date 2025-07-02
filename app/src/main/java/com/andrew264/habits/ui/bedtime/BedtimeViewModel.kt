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

data class BedtimeUiState(
    val timelineSegments: List<TimelineSegment> = emptyList(),
    val selectedTimelineRange: TimelineRange = TimelineRange.DAY,
    val allSchedules: List<Schedule> = listOf(DefaultSchedules.defaultSleepSchedule),
    val selectedSchedule: Schedule = DefaultSchedules.defaultSleepSchedule,
    val scheduleInfo: ScheduleInfo? = null,
    val viewStartTimeMillis: Long = 0,
    val viewEndTimeMillis: Long = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BedtimeViewModel @Inject constructor(
    private val getBedtimeScreenDataUseCase: GetBedtimeScreenDataUseCase,
    private val setSleepScheduleUseCase: SetSleepScheduleUseCase
) : ViewModel() {

    private val _selectedTimelineRange = MutableStateFlow(TimelineRange.DAY)
    private val _uiState = MutableStateFlow(BedtimeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedTimelineRange.flatMapLatest { range ->
                // Every time range changes, we get a new flow of timeline segments
                getBedtimeScreenDataUseCase.getTimelineSegments(range)
                    .combine(getBedtimeScreenDataUseCase.allSchedules) { segments, allSchedules ->
                        Pair(segments, allSchedules)
                    }
                    .combine(getBedtimeScreenDataUseCase.selectedSchedule) { (segments, allSchedules), selected ->
                        Triple(segments, allSchedules, selected)
                    }
                    .combine(getBedtimeScreenDataUseCase.scheduleInfo) { (segments, allSchedules, selected), info ->
                        val endTime = System.currentTimeMillis()
                        _uiState.update {
                            it.copy(
                                selectedTimelineRange = range,
                                timelineSegments = segments,
                                allSchedules = allSchedules,
                                selectedSchedule = selected,
                                scheduleInfo = info,
                                viewEndTimeMillis = endTime,
                                viewStartTimeMillis = endTime - range.durationMillis,
                                isLoading = false
                            )
                        }
                    }
            }.collect()
        }
    }

    fun setTimelineRange(range: TimelineRange) {
        _selectedTimelineRange.value = range
    }

    fun selectSchedule(scheduleId: String) {
        viewModelScope.launch {
            setSleepScheduleUseCase.execute(scheduleId)
        }
    }
}