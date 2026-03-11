package com.andrew264.habits.data.entity.counter

import androidx.room.*

@Entity(
    tableName = "counter_logs",
    foreignKeys = [
        ForeignKey(
            entity = CounterEntity::class,
            parentColumns = ["counter_id"],
            childColumns = ["counter_id_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["counter_id_fk"]),
        Index(value = ["timestamp"])
    ]
)
data class CounterLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "counter_id_fk")
    val counterId: String,
    val timestamp: Long,
    val value: Double
)