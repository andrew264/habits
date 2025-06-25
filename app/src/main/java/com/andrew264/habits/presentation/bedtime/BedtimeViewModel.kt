package com.andrew264.habits.presentation.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.data.repository.SettingsRepository
import com.andrew264.habits.data.repository.UserPresenceHistoryRepository
import com.andrew264.habits.model.ManualSleepSchedule
import com.andrew264.habits.state.UserPresenceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
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
    DAY("1 Day", TimeUnit.DAYS.toMillis(1)),
    WEEK("7 Days", TimeUnit.DAYS.toMillis(7))
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BedtimeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userPresenceHistoryRepository: UserPresenceHistoryRepository
) : ViewModel() {

    private val _selectedTimelineRange = MutableStateFlow(TimelineRange.DAY)
    val selectedTimelineRange: StateFlow<TimelineRange> = _selectedTimelineRange.asStateFlow()

    private val manualScheduleState: StateFlow<ManualSleepSchedule> =
        settingsRepository.settingsFlow
            .map { persistentSettings -> persistentSettings.manualSleepSchedule }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ManualSleepSchedule()
            )

    val currentBedtimeHour: StateFlow<Int?> = manualScheduleState
        .map { it.bedtimeHour }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            manualScheduleState.value.bedtimeHour
        )

    val currentBedtimeMinute: StateFlow<Int?> = manualScheduleState
        .map { it.bedtimeMinute }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            manualScheduleState.value.bedtimeMinute
        )

    val currentWakeUpHour: StateFlow<Int?> = manualScheduleState
        .map { it.wakeUpHour }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            manualScheduleState.value.wakeUpHour
        )

    val currentWakeUpMinute: StateFlow<Int?> = manualScheduleState
        .map { it.wakeUpMinute }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            manualScheduleState.value.wakeUpMinute
        )

    // This flow will combine the selected range and historical data to produce timeline segments
    val timelineSegments: StateFlow<List<TimelineSegment>> = _selectedTimelineRange.flatMapLatest { range ->
        val now = System.currentTimeMillis()
        // Align startTime to the beginning of the current day for "1 Day" or "7 Days ago" for "7 Days"
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val endTimeMillis = calendar.timeInMillis + TimeUnit.DAYS.toMillis(1) // End of current day

        val startTimeMillis = when (range) {
            TimelineRange.DAY -> calendar.timeInMillis // Start of today
            TimelineRange.WEEK -> calendar.timeInMillis - TimeUnit.DAYS.toMillis(6) // 7 days ago (inclusive of today)
        }

        // Flow for events within the range
        val eventsInRangeFlow = userPresenceHistoryRepository.getPresenceHistoryInRangeFlow(startTimeMillis, endTimeMillis)

        // Combine with the event immediately preceding the startTime
        flow {
            val precedingEvent = userPresenceHistoryRepository.getLatestEventBefore(startTimeMillis)
            eventsInRangeFlow.collect { eventsInRange ->
                emit(processEventsToSegments(eventsInRange, precedingEvent, startTimeMillis, endTimeMillis, range))
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
        range: TimelineRange
    ): List<TimelineSegment> {
        if (events.isEmpty() && precedingEvent == null) {
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
        var currentIdx = 0

        var activeState = precedingEvent?.let { UserPresenceState.valueOf(it.state) } ?: UserPresenceState.UNKNOWN

        if (precedingEvent != null && precedingEvent.timestamp < viewStartTimeMillis) {
            // The state from precedingEvent applies from viewStartTimeMillis up to the first event in range, or end of view
        } else if (events.isNotEmpty() && events.first().timestamp > viewStartTimeMillis) {
            // First event is after view start, so UNKNOWN or preceding state until then
            activeState = precedingEvent?.let { UserPresenceState.valueOf(it.state) } ?: UserPresenceState.UNKNOWN
        }


        while (currentTime < viewEndTimeMillis && currentIdx <= events.size) {
            val nextEventTime: Long
            val nextState: UserPresenceState

            if (currentIdx < events.size) {
                val event = events[currentIdx]
                if (event.timestamp <= currentTime) {
                    activeState = UserPresenceState.valueOf(event.state)
                    val segmentStartTime = currentTime

                    var segmentEndTime = viewEndTimeMillis
                    if (currentIdx + 1 < events.size) {
                        segmentEndTime = events[currentIdx + 1].timestamp.coerceAtMost(viewEndTimeMillis)
                    }

                    if (segmentStartTime < segmentEndTime) {
                        segments.add(
                            TimelineSegment(
                                segmentStartTime,
                                segmentEndTime,
                                activeState,
                                segmentEndTime - segmentStartTime
                            )
                        )
                    }
                    currentTime = segmentEndTime
                    currentIdx++
                    continue
                } else {
                    nextEventTime = event.timestamp.coerceAtMost(viewEndTimeMillis)
                    nextState = UserPresenceState.valueOf(event.state)
                }
            } else {
                nextEventTime = viewEndTimeMillis
                nextState = activeState
            }

            if (currentTime < nextEventTime) {
                segments.add(
                    TimelineSegment(
                        currentTime,
                        nextEventTime,
                        activeState,
                        nextEventTime - currentTime
                    )
                )
            }

            currentTime = nextEventTime
            if (currentIdx < events.size) {
                activeState = nextState
            }
            currentIdx++
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

    fun setBedtime(
        hour: Int,
        minute: Int
    ) {
        viewModelScope.launch {
            settingsRepository.updateManualBedtime(hour, minute)
        }
    }

    fun clearBedtime() {
        viewModelScope.launch {
            settingsRepository.updateManualBedtime(null, null)
        }
    }

    fun setWakeUpTime(
        hour: Int,
        minute: Int
    ) {
        viewModelScope.launch {
            settingsRepository.updateManualWakeUpTime(hour, minute)
        }
    }

    fun clearWakeUpTime() {
        viewModelScope.launch {
            settingsRepository.updateManualWakeUpTime(null, null)
        }
    }
}