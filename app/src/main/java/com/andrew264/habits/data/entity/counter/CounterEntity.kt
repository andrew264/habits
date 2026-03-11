package com.andrew264.habits.data.entity.counter

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrew264.habits.model.counter.AggregationType
import com.andrew264.habits.model.counter.CounterType

@Entity(tableName = "counters")
data class CounterEntity(
    @PrimaryKey
    @ColumnInfo(name = "counter_id")
    val id: String,
    val name: String,
    val type: CounterType,
    @ColumnInfo(name = "aggregation_type")
    val aggregationType: AggregationType,
    val target: Double?,
    @ColumnInfo(name = "color_hex")
    val colorHex: String
)