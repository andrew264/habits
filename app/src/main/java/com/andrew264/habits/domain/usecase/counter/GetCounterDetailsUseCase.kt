package com.andrew264.habits.domain.usecase.counter

import com.andrew264.habits.domain.model.Counter
import com.andrew264.habits.domain.model.CounterLog
import com.andrew264.habits.domain.repository.CounterRepository
import com.andrew264.habits.model.counter.AggregationType
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.common.utils.FormatUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class CounterDetailsModel(
    val counter: Counter,
    val allLogs: List<CounterLog>,
    val chartEntries: List<BarChartEntry>
)

class GetCounterDetailsUseCase @Inject constructor(
    private val counterRepository: CounterRepository
) {
    fun execute(counterId: String): Flow<CounterDetailsModel?> {
        val days = 30

        return combine(
            counterRepository.getCounterById(counterId),
            counterRepository.getLogsForCounter(counterId)
        ) { counter, allLogs ->

            if (counter == null) return@combine null
            val now = System.currentTimeMillis()
            val startTime = now - TimeUnit.DAYS.toMillis(days.toLong())
            val logsInRange = allLogs.filter { it.timestamp >= startTime }
            val groupedByDay = logsInRange.groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            val chartEntries = (0 until days).map { i ->
                val date = LocalDate.now(ZoneId.systemDefault()).minusDays((days - 1 - i).toLong())
                val logsForDay = groupedByDay[date] ?: emptyList()
                val aggregatedValue = calculateAggregation(logsForDay, counter.aggregationType)
                val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val label = FormatUtils.formatChartDayLabel(timestamp)
                BarChartEntry(value = aggregatedValue.toFloat(), label = label, timestamp = timestamp)

            }

            CounterDetailsModel(
                counter = counter,
                allLogs = allLogs,
                chartEntries = chartEntries
            )
        }
    }

    private fun calculateAggregation(logs: List<CounterLog>, type: AggregationType): Double {
        if (logs.isEmpty()) return 0.0
        return when (type) {
            AggregationType.SUM -> logs.sumOf { it.value }
            AggregationType.MAX -> logs.maxOfOrNull { it.value } ?: 0.0
            AggregationType.MIN -> logs.minOfOrNull { it.value } ?: 0.0
            AggregationType.AVERAGE -> logs.map { it.value }.average().let { if (it.isNaN()) 0.0 else it }
        }
    }
}