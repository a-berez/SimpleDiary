package com.example.simplediary.data.local.model

data class DailyWorkoutStatsDbRow(
    val dayStartEpochMillisUtc: Long,
    val totalWorkouts: Int,
    val totalDurationMinutes: Int,
    val totalCaloriesBurned: Int,
    val totalCardioDistanceKm: Double,
)
