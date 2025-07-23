package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.model.AppSegment
import com.andrew264.habits.domain.model.ScreenOnPeriod
import com.andrew264.habits.domain.model.UsageTimelineModel
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.repository.ScreenHistoryRepository
import com.andrew264.habits.domain.repository.WhitelistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetUsageTimelineUseCase @Inject constructor(
    private val screenHistoryRepository: ScreenHistoryRepository,
    private val appUsageRepository: AppUsageRepository,
    private val whitelistRepository: WhitelistRepository,
) {
    fun execute(
        startTime: Long,
        endTime: Long
    ): Flow<UsageTimelineModel> {
        return combine(
            screenHistoryRepository.getScreenEventsInRange(startTime, endTime),
            appUsageRepository.getUsageEventsInRange(startTime, endTime),
            whitelistRepository.getWhitelistedApps()
        ) { screenEvents, appUsageEvents, whitelistedApps ->
            val whitelistedAppsMap = whitelistedApps.associate { it.packageName to it.colorHex }

            val screenOnPeriods = mutableListOf<Pair<Long, Long>>()
            var lastScreenOnTime: Long? = null
            val pickupCount = screenEvents.count { it.eventType == "SCREEN_ON" }

            for (event in screenEvents) {
                if (event.eventType == "SCREEN_ON") {
                    if (lastScreenOnTime == null) {
                        lastScreenOnTime = event.timestamp
                    }
                } else if (event.eventType == "SCREEN_OFF") {
                    if (lastScreenOnTime != null) {
                        screenOnPeriods.add(Pair(lastScreenOnTime, event.timestamp))
                        lastScreenOnTime = null
                    }
                }
            }

            lastScreenOnTime?.let { screenOnPeriods.add(Pair(it, endTime)) }

            val totalScreenOnTime = screenOnPeriods.sumOf { it.second - it.first }


            val finalPeriods = screenOnPeriods.map { (periodStart, periodEnd) ->
                val segments = appUsageEvents
                    .filter { it.startTimestamp < periodEnd && (it.endTimestamp ?: Long.MAX_VALUE) > periodStart }
                    .map { usageEvent ->
                        AppSegment(
                            packageName = usageEvent.packageName,
                            startTimestamp = maxOf(usageEvent.startTimestamp, periodStart),
                            endTimestamp = minOf(usageEvent.endTimestamp ?: periodEnd, periodEnd),
                            color = whitelistedAppsMap[usageEvent.packageName]
                        )
                    }
                ScreenOnPeriod(periodStart, periodEnd, segments)
            }

            UsageTimelineModel(
                screenOnPeriods = finalPeriods,
                viewStart = startTime,
                viewEnd = endTime,
                pickupCount = pickupCount,
                totalScreenOnTime = totalScreenOnTime
            )
        }
    }
}