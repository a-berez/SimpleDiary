package com.example.simplediary.domain.usecase

import com.example.simplediary.domain.model.DailySummary
import com.example.simplediary.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow

class ObserveDailySummaryUseCase(
    private val repository: JournalRepository,
) {
    operator fun invoke(dayEpochMillisUtcStart: Long): Flow<DailySummary> {
        return repository.observeDailySummary(dayEpochMillisUtcStart)
    }
}
