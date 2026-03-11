package com.andrew264.habits.domain.usecase.counter

import com.andrew264.habits.domain.model.Counter
import com.andrew264.habits.domain.model.CounterLog
import com.andrew264.habits.domain.repository.CounterRepository
import com.andrew264.habits.model.counter.AggregationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class CounterWithProgress(
    val counter: Counter,
    val todayValue: Double,
    val hasLogsToday: Boolean
)

class GetCountersWithTodayProgressUseCase @Inject constructor(
    private val counterRepository: CounterRepository
) {
    fun execute(): Flow<List<CounterWithProgress>> {
        val today = LocalDate.now(ZoneId.systemDefault())
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return combine(
            counterRepository.getAllCounters(),
            counterRepository.getAllLogsInRange(startOfDay, endOfDay)
        ) { counters, logs ->
            val logsByCounterId = logs.groupBy { it.counterId }

            counters.map { counter ->
                val counterLogs = logsByCounterId[counter.id] ?: emptyList()
                val aggregatedValue = calculateAggregation(counterLogs, counter.aggregationType)

                CounterWithProgress(
                    counter = counter,
                    todayValue = aggregatedValue,
                    hasLogsToday = counterLogs.isNotEmpty()
                )
            }
        }
    }

    private fun calculateAggregation(logs: List<CounterLog>, type: AggregationType): Double {
        if (logs.isEmpty()) return 0.0

        return when (type) {
            AggregationType.SUM -> logs.sumOf { it.value }
            AggregationType.MAX -> logs.maxOfOrNull { it.value } ?: 0.0
            AggregationType.MIN -> logs.minOfOrNull { it.value } ?: 0.0
            AggregationType.AVERAGE -> {
                val avg = logs.map { it.value }.average()
                if (avg.isNaN()) 0.0 else avg
            }
        }
    }
}