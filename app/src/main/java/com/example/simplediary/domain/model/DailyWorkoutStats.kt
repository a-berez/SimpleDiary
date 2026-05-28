package com.example.simplediary.domain.model

data class DailyWorkoutStats(
    val dayStartEpochMillisUtc: Long,
    val totalWorkouts: Int,
    val totalDurationMinutes: Int,
    val totalCaloriesBurned: Int,
    val totalCardioDistanceKm: Double,
)
