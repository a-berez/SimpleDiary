package com.example.simplediary.domain.repository

import com.example.simplediary.domain.model.DailySummary
import com.example.simplediary.domain.model.EntryType
import com.example.simplediary.domain.model.FeedFilter
import com.example.simplediary.domain.model.WeeklyWorkoutStats
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun observeFeed(filter: FeedFilter): Flow<List<FeedRow>>
    fun observeDailySummary(dayEpochMillisUtcStart: Long): Flow<DailySummary>
    fun observeWeeklyWorkoutStats(weekStartEpochMillisUtc: Long): Flow<WeeklyWorkoutStats>
}

data class FeedRow(
    val id: Long,
    val type: EntryType,
    val timestampEpochMillis: Long,
    val title: String,
    val subtitle: String? = null,
    val photoPath: String? = null,
)
