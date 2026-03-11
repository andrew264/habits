package com.andrew264.habits.domain.model

data class CounterLog(
    val id: Long = 0,
    val counterId: String,
    val timestamp: Long,
    val value: Double
)