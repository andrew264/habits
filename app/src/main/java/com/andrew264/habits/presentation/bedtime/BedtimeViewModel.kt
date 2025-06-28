package com.andrew264.habits.presentation.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.data.repository.ScheduleRepository
import com.andrew264.habits.data.repository.SettingsRepository
import com.andrew264.habits.data.repository.UserPresenceHistoryRepository
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.state.UserPresenceState
import com.andrew264.habits.util.ScheduleAnalyzer
import com.andrew264.habits.util.ScheduleCoverage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Represents a processed segment for the timeline UI
data class TimelineSegment(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val state: UserPresenceState,
    val durationMillis: Long
)

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
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val userPresenceHistoryRepository: UserPresenceHistoryRepository
) : ViewModel() {

    private val _selectedTimelineRange = MutableStateFlow(TimelineRange.DAY)
    val selectedTimelineRange: StateFlow<TimelineRange> = _selectedTimelineRange.asStateFlow()

    private val _viewStartTimeMillis = MutableStateFlow(0L)
    val viewStartTimeMillis: StateFlow<Long> = _viewStartTimeMillis.asStateFlow()

    private val _viewEndTimeMillis = MutableStateFlow(0L)
    val viewEndTimeMillis: StateFlow<Long> = _viewEndTimeMillis.asStateFlow()

    val allSchedules: StateFlow<List<Schedule>> = scheduleRepository.getAllSchedules()
        .map { dbSchedules ->
            // Add the default schedule to the top of the list
            listOf(DefaultSchedules.defaultSleepSchedule) + dbSchedules
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(DefaultSchedules.defaultSleepSchedule)
        )

    val selectedScheduleId: StateFlow<String?> = settingsRepository.settingsFlow
        .map { it.selectedScheduleId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val selectedSchedule: StateFlow<Schedule> = combine(
        allSchedules,
        selectedScheduleId
    ) { schedules, id ->
        schedules.find { it.id == id } ?: DefaultSchedules.defaultSleepSchedule
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DefaultSchedules.defaultSleepSchedule
    )

    val scheduleInfo: StateFlow<ScheduleInfo?> = selectedSchedule.map { schedule ->
        val analyzer = ScheduleAnalyzer(schedule.groups)
        ScheduleInfo(
            summary = analyzer.createSummary(),
            coverage = analyzer.calculateCoverage()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // This flow will combine the selected range and historical data to produce timeline segments
    val timelineSegments: StateFlow<List<TimelineSegment>> = _selectedTimelineRange.flatMapLatest { range ->
        val now = System.currentTimeMillis()
        val viewEnd: Long = now
        val viewStart: Long = now - range.durationMillis

        _viewStartTimeMillis.value = viewStart
        _viewEndTimeMillis.value = viewEnd

        // Flow for events within the range
        val eventsInRangeFlow = userPresenceHistoryRepository.getPresenceHistoryInRangeFlow(viewStart, viewEnd)

        // Combine with the event immediately preceding the startTime
        flow {
            val precedingEvent = userPresenceHistoryRepository.getLatestEventBefore(viewStart)
            eventsInRangeFlow.collect { eventsInRange ->
                emit(processEventsToSegments(eventsInRange, precedingEvent, viewStart, viewEnd))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun processEventsToSegments(
        events: List<UserPresenceEvent>,
        precedingEvent: UserPresenceEvent?,
        viewStartTimeMillis: Long,
        viewEndTimeMillis: Long,
    ): List<TimelineSegment> {
        if (events.isEmpty() && precedingEvent == null) {
            // If no events and no preceding event, the entire view is UNKNOWN
            return listOf(
                TimelineSegment(
                    viewStartTimeMillis,
                    viewEndTimeMillis,
                    UserPresenceState.UNKNOWN,
                    viewEndTimeMillis - viewStartTimeMillis
                )
            )
        }

        val segments = mutableListOf<TimelineSegment>()
        var currentTime = viewStartTimeMillis
        var lastKnownState = precedingEvent?.let { UserPresenceState.valueOf(it.state) } ?: UserPresenceState.UNKNOWN

        // Handle the period from viewStartTimeMillis to the first event
        val firstEventTimestamp = events.firstOrNull()?.timestamp ?: viewEndTimeMillis
        if (currentTime < firstEventTimestamp) {
            val segmentEndTime = firstEventTimestamp.coerceAtMost(viewEndTimeMillis)
            if (currentTime < segmentEndTime) {
                segments.add(
                    TimelineSegment(
                        currentTime,
                        segmentEndTime,
                        lastKnownState,
                        segmentEndTime - currentTime
                    )
                )
            }
            currentTime = segmentEndTime
        }


        events.forEach { event ->
            // If there's a gap between the current time and this event's timestamp, fill it with lastKnownState
            if (currentTime < event.timestamp && currentTime < viewEndTimeMillis) {
                val segmentEndTime = event.timestamp.coerceAtMost(viewEndTimeMillis)
                segments.add(
                    TimelineSegment(
                        currentTime,
                        segmentEndTime,
                        lastKnownState,
                        segmentEndTime - currentTime
                    )
                )
            }
            // Process the event itself
            val eventState = UserPresenceState.valueOf(event.state)
            lastKnownState = eventState // Update lastKnownState
            currentTime = event.timestamp.coerceAtMost(viewEndTimeMillis) // Move currentTime to this event's time (or viewEndTimeMillis if event is past it)
        }

        // After all events, if currentTime is still before viewEndTimeMillis, fill the remainder with the lastKnownState
        if (currentTime < viewEndTimeMillis) {
            segments.add(
                TimelineSegment(
                    currentTime,
                    viewEndTimeMillis,
                    lastKnownState,
                    viewEndTimeMillis - currentTime
                )
            )
        }

        return consolidateSegments(segments.filter { it.durationMillis > 0 })
    }


    private fun consolidateSegments(segments: List<TimelineSegment>): List<TimelineSegment> {
        if (segments.isEmpty()) return emptyList()
        val consolidated = mutableListOf<TimelineSegment>()
        var currentSegment = segments.first()

        for (i in 1 until segments.size) {
            val next = segments[i]
            if (next.state == currentSegment.state && next.startTimeMillis == currentSegment.endTimeMillis) {
                currentSegment = currentSegment.copy(
                    endTimeMillis = next.endTimeMillis,
                    durationMillis = currentSegment.durationMillis + next.durationMillis
                )
            } else {
                consolidated.add(currentSegment)
                currentSegment = next
            }
        }
        consolidated.add(currentSegment)
        return consolidated
    }


    fun setTimelineRange(range: TimelineRange) {
        _selectedTimelineRange.value = range
    }

    fun selectSchedule(scheduleId: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedScheduleId(scheduleId)
        }
    }
}