package com.andrew264.habits.model

data class ManualSleepSchedule(
    val bedtimeHour: Int? = null,
    val bedtimeMinute: Int? = null,
    val wakeUpHour: Int? = null,
    val wakeUpMinute: Int? = null
) {
    val isBedtimeSet: Boolean get() = bedtimeHour != null && bedtimeMinute != null
    val isWakeUpTimeSet: Boolean get() = wakeUpHour != null && wakeUpMinute != null

    val bedtimeInMinutesTotal: Int?
        get() = if (isBedtimeSet) bedtimeHour!! * 60 + bedtimeMinute!! else null

    val wakeUpInMinutesTotal: Int?
        get() = if (isWakeUpTimeSet) wakeUpHour!! * 60 + wakeUpMinute!! else null
}
