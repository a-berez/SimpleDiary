package com.example.simplediary.domain.model

data class DailySummary(
    val dateEpochMillisUtcStart: Long,
    val actualProteinsGrams: Double,
    val targetProteinsGrams: Double,
    val actualFatsGrams: Double,
    val targetFatsGrams: Double,
    val actualCarbsGrams: Double,
    val targetCarbsGrams: Double,
    val actualCaloriesKcal: Double,
    val targetCaloriesKcal: Double,
)
