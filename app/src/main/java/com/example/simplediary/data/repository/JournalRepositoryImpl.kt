package com.example.simplediary.data.repository

import com.example.simplediary.domain.model.DailySummary
import com.example.simplediary.domain.model.FeedFilter
import com.example.simplediary.domain.model.WeeklyWorkoutStats
import com.example.simplediary.domain.repository.FeedRow
import com.example.simplediary.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class JournalRepositoryImpl : JournalRepository {
    override fun observeFeed(filter: FeedFilter): Flow<List<FeedRow>> = flowOf(emptyList())

    override fun observeDailySummary(dayEpochMillisUtcStart: Long): Flow<DailySummary> {
        return flowOf(
            DailySummary(
                dateEpochMillisUtcStart = dayEpochMillisUtcStart,
                actualProteinsGrams = 0.0,
                targetProteinsGrams = 0.0,
                actualFatsGrams = 0.0,
                targetFatsGrams = 0.0,
                actualCarbsGrams = 0.0,
                targetCarbsGrams = 0.0,
                actualCaloriesKcal = 0.0,
                targetCaloriesKcal = 0.0,
            )
        )
    }

    override fun observeWeeklyWorkoutStats(weekStartEpochMillisUtc: Long): Flow<WeeklyWorkoutStats> {
        return flowOf(
            WeeklyWorkoutStats(
                weekStartEpochMillisUtc = weekStartEpochMillisUtc,
                totalWorkouts = 0,
                totalDurationMinutes = 0,
                totalCaloriesBurned = 0,
            )
        )
    }
}
