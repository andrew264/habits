package com.andrew264.habits.domain.usecase

import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.domain.analyzer.ScheduleAnalyzer
import com.andrew264.habits.domain.model.TimelineSegment
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.bedtime.BedtimeChartRange
import com.andrew264.habits.ui.bedtime.ScheduleInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Gathers and processes all data required for the Bedtime screen.
 * This includes fetching schedules, user presence history, and combining them
 * into UI-ready models like [ScheduleInfo] and [TimelineSegment].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetBedtimeScreenDataUseCase @Inject constructor(
    settingsRepository: SettingsRepository,
    scheduleRepository: ScheduleRepository,
    private val userPresenceHistoryRepository: UserPresenceHistoryRepository
) {
    val allSchedules: Flow<List<Schedule>> = scheduleRepository.getAllSchedules()
        .map { dbSchedules ->
            listOf(DefaultSchedules.defaultSleepSchedule) + dbSchedules
        }

    val selectedSchedule: Flow<Schedule> = settingsRepository.settingsFlow
        .map { it.selectedScheduleId }
        .flatMapLatest { id ->
            allSchedules.map { schedules ->
                schedules.find { it.id == id } ?: DefaultSchedules.defaultSleepSchedule
            }
        }

    val scheduleInfo: Flow<ScheduleInfo> = selectedSchedule.map { schedule ->
        val analyzer = ScheduleAnalyzer(schedule.groups)
        ScheduleInfo(
            summary = analyzer.createSummary(),
            coverage = analyzer.calculateCoverage()
        )
    }

    fun getTimelineSegments(range: BedtimeChartRange): Flow<List<TimelineSegment>> {
        val now = System.currentTimeMillis()
        val viewEnd: Long = now
        val viewStart: Long = now - range.durationMillis

        val eventsInRangeFlow = userPresenceHistoryRepository.getPresenceHistoryInRangeFlow(viewStart, viewEnd)

        return flow {
            val precedingEvent = userPresenceHistoryRepository.getLatestEventBefore(viewStart)
            eventsInRangeFlow.collect { eventsInRange ->
                emit(processEventsToSegments(eventsInRange, precedingEvent, viewStart, viewEnd))
            }
        }
    }

    private fun processEventsToSegments(
        events: List<UserPresenceEvent>,
        precedingEvent: UserPresenceEvent?,
        viewStartTimeMillis: Long,
        viewEndTimeMillis: Long,
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
        var lastKnownState = precedingEvent?.let { UserPresenceState.valueOf(it.state) } ?: UserPresenceState.UNKNOWN

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
            val eventState = UserPresenceState.valueOf(event.state)
            lastKnownState = eventState
            currentTime = event.timestamp.coerceAtMost(viewEndTimeMillis)
        }

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
}