package com.andrew264.habits.domain.analyzer

import com.andrew264.habits.data.entity.WaterIntakeEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

data class DailyWaterIntake(
    val date: LocalDate,
    val totalMl: Int
)

data class HourlyWaterIntake(
    val hour: Int, // 0-23
    val totalMl: Int
)

data class WaterStatistics(
    val dailyIntakes: List<DailyWaterIntake>,
    val hourlyBreakdown: List<HourlyWaterIntake>,
    val dailyAverage: Int,
    val totalDays: Int,
    val daysGoalMet: Int,
    val successRate: Float
)

class WaterStatisticsAnalyzer @Inject constructor() {

    fun analyze(
        entries: List<WaterIntakeEntry>,
        dailyTargetMl: Int
    ): WaterStatistics {
        if (entries.isEmpty()) {
            return WaterStatistics(emptyList(), emptyList(), 0, 0, 0, 0f)
        }

        val dailyIntakes = groupEntriesByDay(entries)
        val hourlyBreakdown = groupEntriesByHour(entries)

        val totalDays = dailyIntakes.size
        val totalIntake = dailyIntakes.sumOf { it.totalMl }
        val dailyAverage = if (totalDays > 0) totalIntake / totalDays else 0
        val daysGoalMet = dailyIntakes.count { it.totalMl >= dailyTargetMl }
        val successRate = if (totalDays > 0) (daysGoalMet.toFloat() / totalDays.toFloat()) else 0f

        return WaterStatistics(
            dailyIntakes = dailyIntakes,
            hourlyBreakdown = hourlyBreakdown,
            dailyAverage = dailyAverage,
            totalDays = totalDays,
            daysGoalMet = daysGoalMet,
            successRate = successRate
        )
    }

    private fun groupEntriesByDay(entries: List<WaterIntakeEntry>): List<DailyWaterIntake> {
        return entries
            .groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .map { (date, dailyEntries) ->
                DailyWaterIntake(date, dailyEntries.sumOf { it.amountMl })
            }
            .sortedBy { it.date }
    }

    private fun groupEntriesByHour(entries: List<WaterIntakeEntry>): List<HourlyWaterIntake> {
        val hourlyMap = entries
            .groupBy {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()).hour
            }
            .mapValues { (_, hourlyEntries) ->
                hourlyEntries.sumOf { it.amountMl }
            }

        // Ensure all hours from 0-23 are present
        return (0..23).map { hour ->
            HourlyWaterIntake(hour, hourlyMap.getOrDefault(hour, 0))
        }
    }
}