package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.simplediary.domain.model.WorkoutType

@Entity(
    tableName = "workouts",
    indices = [
        Index(value = ["dateEpochMillisUtcStart"]),
        Index(value = ["type"]),
    ],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateEpochMillisUtcStart: Long,
    val type: WorkoutType,
    val durationMinutes: Int,
    val caloriesBurned: Int,
)
