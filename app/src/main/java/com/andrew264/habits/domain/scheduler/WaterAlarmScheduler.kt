package com.andrew264.habits.domain.scheduler

interface WaterAlarmScheduler {
    fun scheduleNextReminder(intervalMinutes: Long)
    fun handleSnooze(snoozeMinutes: Long)
    fun cancelReminders()
}