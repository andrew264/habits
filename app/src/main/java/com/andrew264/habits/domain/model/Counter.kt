package com.andrew264.habits.domain.model

import com.andrew264.habits.model.counter.AggregationType
import com.andrew264.habits.model.counter.CounterType

data class Counter(
    val id: String,
    val name: String,
    val type: CounterType,
    val aggregationType: AggregationType,
    val target: Double? = null,
    val colorHex: String
)