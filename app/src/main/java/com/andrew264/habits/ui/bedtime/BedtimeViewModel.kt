package com.andrew264.habits.ui.bedtime

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.R
import com.andrew264.habits.domain.analyzer.ScheduleCoverage
import com.andrew264.habits.domain.model.TimelineSegment
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.usecase.GetBedtimeScreenDataUseCase
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class BedtimeChartRange(
    @StringRes val label: Int,
    val durationMillis: Long,
    val isLinear: Boolean
) {
    TWELVE_HOURS(R.string.bedtime_chart_range_12_hr, TimeUnit.HOURS.toMillis(12), true),
    DAY(R.string.bedtime_chart_range_1_day, TimeUnit.DAYS.toMillis(1), true),
    WEEK(R.string.bedtime_chart_range_7_days, TimeUnit.DAYS.toMillis(7), false),
    MONTH(R.string.bedtime_chart_range_30_days, TimeUnit.DAYS.toMillis(30), false)
}

data class ScheduleInfo(
    val summary: String,
    val coverage: ScheduleCoverage
)

data class BedtimeUiState(
    val timelineSegments: List<TimelineSegment> = emptyList(),
    val selectedTimelineRange: BedtimeChartRange = BedtimeChartRange.DAY,
    val allSchedules: List<Schedule> = listOf(DefaultSchedules.defaultSleepSchedule),
    val selectedSchedule: Schedule = DefaultSchedules.defaultSleepSchedule,
    val scheduleInfo: ScheduleInfo? = null,
    val viewStartTimeMillis: Long = 0,
    val viewEndTimeMillis: Long = 0,
    val isLoading: Boolean = true,
    val isBedtimeTrackingEnabled: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BedtimeViewModel @Inject constructor(
    private val getBedtimeScreenDataUseCase: GetBedtimeScreenDataUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTimelineRange = MutableStateFlow(BedtimeChartRange.DAY)
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<BedtimeUiState> = settingsRepository.settingsFlow
        .flatMapLatest { settings ->
            if (!settings.isBedtimeTrackingEnabled) {
                flowOf(BedtimeUiState(isLoading = false, isBedtimeTrackingEnabled = false))
            } else {
                combine(_selectedTimelineRange, refreshTrigger) { range, _ -> range }
                    .flatMapLatest { range ->
                        getBedtimeScreenDataUseCase.getTimelineSegments(range)
                            .combine(getBedtimeScreenDataUseCase.allSchedules) { segments, allSchedules ->
                                Pair(segments, allSchedules)
                            }
                            .combine(getBedtimeScreenDataUseCase.selectedSchedule) { (segments, allSchedules), selected ->
                                Triple(segments, allSchedules, selected)
                            }
                            .combine(getBedtimeScreenDataUseCase.scheduleInfo) { (segments, allSchedules, selected), info ->
                                val endTime = System.currentTimeMillis()
                                BedtimeUiState(
                                    isBedtimeTrackingEnabled = true,
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
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BedtimeUiState()
        )


    fun setTimelineRange(range: BedtimeChartRange) {
        _selectedTimelineRange.value = range
    }

    fun refresh() {
        refreshTrigger.value++
    }
}