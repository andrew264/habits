package com.andrew264.habits.domain.usecase

import com.andrew264.habits.domain.model.UsageStatistics
import com.andrew264.habits.domain.model.UsageTimeBin
import com.andrew264.habits.domain.repository.AppUsageRepository
import com.andrew264.habits.domain.repository.ScreenHistoryRepository
import com.andrew264.habits.ui.usage.UsageTimeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class GetUsageStatisticsUseCase @Inject constructor(
    private val screenHistoryRepository: ScreenHistoryRepository,
    private val appUsageRepository: AppUsageRepository
) {
    fun execute(range: UsageTimeRange): Flow<UsageStatistics> {
        val now = System.currentTimeMillis()
        val (startTime, binDuration, binCount) = when (range) {
            UsageTimeRange.DAY -> {
                val start = now - TimeUnit.DAYS.toMillis(1)
                Triple(start, TimeUnit.HOURS.toMillis(1), 24)
            }

            UsageTimeRange.WEEK -> {
                val start = LocalDate.now(ZoneId.systemDefault()).minusDays(6)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                Triple(start, TimeUnit.DAYS.toMillis(1), 7)
            }
        }
        val endTime = startTime + (binDuration * binCount)

        return combine(
            screenHistoryRepository.getScreenEventsInRange(startTime, endTime),
            appUsageRepository.getUsageEventsInRange(startTime, endTime)
        ) { screenEvents, appUsageEvents ->
            val screenOnPeriods = mutableListOf<Pair<Long, Long>>()
            var lastScreenOnTime: Long? = null
            for (event in screenEvents) {
                if (event.eventType == "SCREEN_ON") {
                    if (lastScreenOnTime == null) lastScreenOnTime = event.timestamp
                } else if (event.eventType == "SCREEN_OFF") {
                    if (lastScreenOnTime != null) {
                        screenOnPeriods.add(Pair(lastScreenOnTime, event.timestamp))
                        lastScreenOnTime = null
                    }
                }
            }
            lastScreenOnTime?.let { screenOnPeriods.add(Pair(it, now)) }

            val totalScreenOnTime = screenOnPeriods.sumOf { (start, end) -> max(0, end - start) }
            val pickupCount = screenEvents.count { it.eventType == "SCREEN_ON" }

            val appUsagePerBin = Array(binCount) { mutableMapOf<String, Long>() }
            val screenOnPerBin = LongArray(binCount)

            for (period in screenOnPeriods) {
                val periodStart = max(period.first, startTime)
                val periodEnd = min(period.second, endTime)

                val startBinIndex = ((periodStart - startTime) / binDuration).toInt().coerceIn(0, binCount - 1)
                val endBinIndex = ((periodEnd - 1 - startTime) / binDuration).toInt().coerceIn(0, binCount - 1)

                for (i in startBinIndex..endBinIndex) {
                    val binStart = startTime + i * binDuration
                    val binEnd = binStart + binDuration
                    val overlapStart = max(periodStart, binStart)
                    val overlapEnd = min(periodEnd, binEnd)
                    if (overlapEnd > overlapStart) {
                        screenOnPerBin[i] += (overlapEnd - overlapStart)
                    }
                }

                val relevantAppEvents = appUsageEvents.filter {
                    it.startTimestamp < periodEnd && (it.endTimestamp ?: periodEnd) > periodStart
                }

                for (appEvent in relevantAppEvents) {
                    val eventStart = max(appEvent.startTimestamp, periodStart)
                    val eventEnd = min(appEvent.endTimestamp ?: periodEnd, periodEnd)
                    if (eventEnd <= eventStart) continue

                    val startBin = ((eventStart - startTime) / binDuration).toInt().coerceIn(0, binCount - 1)
                    val endBin = ((eventEnd - 1 - startTime) / binDuration).toInt().coerceIn(0, binCount - 1)

                    for (i in startBin..endBin) {
                        val binStart = startTime + i * binDuration
                        val binEnd = binStart + binDuration
                        val overlapStart = max(eventStart, binStart)
                        val overlapEnd = min(eventEnd, binEnd)
                        if (overlapEnd > overlapStart) {
                            val durationInBin = overlapEnd - overlapStart
                            val currentMap = appUsagePerBin[i]
                            currentMap[appEvent.packageName] = (currentMap[appEvent.packageName] ?: 0L) + durationInBin
                        }
                    }
                }
            }

            val bins = (0 until binCount).map { i ->
                UsageTimeBin(
                    startTime = startTime + i * binDuration,
                    endTime = startTime + (i + 1) * binDuration,
                    totalScreenOnTime = screenOnPerBin[i],
                    appUsage = appUsagePerBin[i]
                )
            }

            val totalUsagePerApp = appUsagePerBin.flatMap { it.entries }
                .groupBy({ it.key }, { it.value })
                .mapValues { it.value.sum() }

            UsageStatistics(
                timeBins = bins,
                totalScreenOnTime = totalScreenOnTime,
                pickupCount = pickupCount,
                totalUsagePerApp = totalUsagePerApp
            )
        }
    }
}