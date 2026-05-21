package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_targets",
    indices = [
        Index(value = ["dateEpochMillisUtcStart"], unique = true),
    ],
)
data class DailyTargetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateEpochMillisUtcStart: Long,
    val targetProteinsGrams: Double,
    val targetFatsGrams: Double,
    val targetCarbsGrams: Double,
    val targetCaloriesKcal: Double,
)
