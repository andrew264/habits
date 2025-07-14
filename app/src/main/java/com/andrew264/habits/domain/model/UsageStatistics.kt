package com.andrew264.habits.domain.model

data class UsageStatistics(
    val timeBins: List<UsageTimeBin>,
    val totalScreenOnTime: Long,
    val pickupCount: Int,
    val totalUsagePerApp: Map<String, Long> // PackageName to total millis
)

data class UsageTimeBin(
    val startTime: Long,
    val endTime: Long,
    val totalScreenOnTime: Long,
    val appUsage: Map<String, Long> // PackageName to millis in this bin
)