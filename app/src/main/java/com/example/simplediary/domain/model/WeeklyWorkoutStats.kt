package com.example.simplediary.domain.model

data class WeeklyWorkoutStats(
    val weekStartEpochMillisUtc: Long,
    val totalWorkouts: Int,
    val totalDurationMinutes: Int,
    val totalCaloriesBurned: Int,
)
