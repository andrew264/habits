package com.andrew264.habits.domain.scheduler

interface SessionAlarmScheduler {
    fun schedule(packageName: String, limitMinutes: Int)
    fun cancel()
}