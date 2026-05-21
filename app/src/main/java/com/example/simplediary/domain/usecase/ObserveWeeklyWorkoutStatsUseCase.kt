package com.example.simplediary.domain.usecase

import com.example.simplediary.domain.model.WeeklyWorkoutStats
import com.example.simplediary.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow

class ObserveWeeklyWorkoutStatsUseCase(
    private val repository: JournalRepository,
) {
    operator fun invoke(weekStartEpochMillisUtc: Long): Flow<WeeklyWorkoutStats> {
        return repository.observeWeeklyWorkoutStats(weekStartEpochMillisUtc)
    }
}
