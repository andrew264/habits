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
    TWELVE_HOURS("12 Hr", TimeUnit.HOURS.toMillis(12)),
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
        val viewStartTimeMillis: Long
        val viewEndTimeMillis: Long

        when (range) {
            TimelineRange.TWELVE_HOURS -> {
                viewEndTimeMillis = now
                viewStartTimeMillis = now - range.durationMillis
            }

            TimelineRange.DAY -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = now }
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                viewStartTimeMillis = calendar.timeInMillis // Start of today
                viewEndTimeMillis = viewStartTimeMillis + range.durationMillis // End of today
            }

            TimelineRange.WEEK -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = now }
                // Align end time to the end of the current day for a consistent 7-day block
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                viewEndTimeMillis = calendar.timeInMillis // End of today
                viewStartTimeMillis = viewEndTimeMillis - range.durationMillis + 1 // Start of 7 days ago (inclusive)
            }
        }

        // Flow for events within the range
        val eventsInRangeFlow = userPresenceHistoryRepository.getPresenceHistoryInRangeFlow(viewStartTimeMillis, viewEndTimeMillis)

        // Combine with the event immediately preceding the startTime
        flow {
            val precedingEvent = userPresenceHistoryRepository.getLatestEventBefore(viewStartTimeMillis)
            eventsInRangeFlow.collect { eventsInRange ->
                emit(processEventsToSegments(eventsInRange, precedingEvent, viewStartTimeMillis, viewEndTimeMillis, range))
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