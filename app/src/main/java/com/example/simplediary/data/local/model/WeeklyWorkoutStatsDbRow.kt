package com.example.simplediary.data.local.model

data class WeeklyWorkoutStatsDbRow(
    val weekStartEpochMillisUtc: Long,
    val totalWorkouts: Int,
    val totalDurationMinutes: Int,
    val totalCaloriesBurned: Int,
    val totalCardioDistanceKm: Double,
)
