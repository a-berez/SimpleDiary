package com.example.simplediary.domain.model

data class WeeklyFoodSummary(
    val weekStartEpochMillisUtc: Long,
    val actualProteinsGrams: Double,
    val actualFatsGrams: Double,
    val actualCarbsGrams: Double,
    val actualCaloriesKcal: Double,
)
