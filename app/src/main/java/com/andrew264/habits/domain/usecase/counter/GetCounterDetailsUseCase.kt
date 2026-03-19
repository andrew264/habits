package com.andrew264.habits.domain.usecase.counter

import com.andrew264.habits.domain.model.Counter
import com.andrew264.habits.domain.model.CounterLog
import com.andrew264.habits.domain.repository.CounterRepository
import com.andrew264.habits.model.counter.AggregationType
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.common.utils.FormatUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun execute(counterId: String, daysLoadedFlow: Flow<Int>): Flow<CounterDetailsModel?> {
        return combine(
            counterRepository.getCounterById(counterId),
            daysLoadedFlow
        ) { counter, days -> counter to days }
            .flatMapLatest { (counter, days) ->
                if (counter == null) return@flatMapLatest flowOf(null)

                val now = System.currentTimeMillis()
                val startTime = now - TimeUnit.DAYS.toMillis(days.toLong())

                counterRepository.getLogsForCounterInRange(counter.id, startTime, now).map { logsInRange ->
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
                        allLogs = logsInRange,
                        chartEntries = chartEntries
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
            AggregationType.AVERAGE -> logs.map { it.value }.average().let { if (it.isNaN()) 0.0 else it }
        }
    }
}