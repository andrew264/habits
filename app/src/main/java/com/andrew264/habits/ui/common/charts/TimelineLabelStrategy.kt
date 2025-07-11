package com.andrew264.habits.ui.common.charts

import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

enum class TimelineLabelStrategy(
    val timeIncrement: Long,
    val formatterPattern: String,
    val chronoUnit: ChronoUnit
) {
    TWELVE_HOURS(TimeUnit.HOURS.toMillis(2), "ha", ChronoUnit.HOURS),
    DAY(TimeUnit.HOURS.toMillis(4), "ha", ChronoUnit.HOURS)
}