package com.example.simplediary.data.repository

import com.example.simplediary.data.local.dao.DailyTargetDao
import com.example.simplediary.data.local.dao.FeedDao
import com.example.simplediary.data.local.dao.MealDao
import com.example.simplediary.data.local.dao.WorkoutDao
import com.example.simplediary.domain.model.DailySummary
import com.example.simplediary.domain.model.EntryType
import com.example.simplediary.domain.model.FeedFilter
import com.example.simplediary.domain.model.WeeklyWorkoutStats
import com.example.simplediary.domain.repository.FeedRow
import com.example.simplediary.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private const val ONE_DAY_MILLIS: Long = 24L * 60L * 60L * 1000L
private const val ONE_WEEK_MILLIS: Long = 7L * ONE_DAY_MILLIS

class JournalRepositoryImpl(
    private val feedDao: FeedDao,
    private val mealDao: MealDao,
    private val dailyTargetDao: DailyTargetDao,
    private val workoutDao: WorkoutDao,
) : JournalRepository {
    override fun observeFeed(filter: FeedFilter): Flow<List<FeedRow>> {
        val includeMeals = filter.entryTypes.isEmpty() || filter.entryTypes.contains(EntryType.MEAL)
        val includeWorkouts = filter.entryTypes.isEmpty() || filter.entryTypes.contains(EntryType.WORKOUT)
        val includeStateNotes = filter.entryTypes.isEmpty() || filter.entryTypes.contains(EntryType.STATE_NOTE)

        return feedDao.observeFeed(
            includeMeals = includeMeals,
            includeWorkouts = includeWorkouts,
            includeStateNotes = includeStateNotes,
            fromEpochMillisInclusive = filter.fromEpochMillisInclusive,
            toEpochMillisInclusive = filter.toEpochMillisInclusive,
        ).map { rows ->
            rows.mapNotNull { row ->
                val type = runCatching { EntryType.valueOf(row.entryType) }.getOrNull() ?: return@mapNotNull null
                FeedRow(
                    id = row.entryId,
                    type = type,
                    timestampEpochMillis = row.timestampEpochMillis,
                    title = row.title,
                    subtitle = row.subtitle,
                    expandedDetails = row.expandedDetails,
                    photoPath = row.photoPath,
                )
            }
        }
    }

    override fun observeDailySummary(dayEpochMillisUtcStart: Long): Flow<DailySummary> {
        val dayEndEpochMillisUtcExclusive = dayEpochMillisUtcStart + ONE_DAY_MILLIS
        return combine(
            mealDao.observeDailyMacroTotals(
                dayStartEpochMillisUtc = dayEpochMillisUtcStart,
                dayEndEpochMillisUtcExclusive = dayEndEpochMillisUtcExclusive,
            ),
            dailyTargetDao.observeTargetEffectiveForDay(dayEpochMillisUtcStart),
        ) { actual, target ->
            DailySummary(
                dateEpochMillisUtcStart = dayEpochMillisUtcStart,
                actualProteinsGrams = actual.actualProteinsGrams,
                targetProteinsGrams = target?.targetProteinsGrams ?: 0.0,
                actualFatsGrams = actual.actualFatsGrams,
                targetFatsGrams = target?.targetFatsGrams ?: 0.0,
                actualCarbsGrams = actual.actualCarbsGrams,
                targetCarbsGrams = target?.targetCarbsGrams ?: 0.0,
                actualCaloriesKcal = actual.actualCaloriesKcal,
                targetCaloriesKcal = target?.targetCaloriesKcal ?: 0.0,
            )
        }
    }

    override fun observeWeeklyWorkoutStats(weekStartEpochMillisUtc: Long): Flow<WeeklyWorkoutStats> {
        val weekEndEpochMillisUtcExclusive = weekStartEpochMillisUtc + ONE_WEEK_MILLIS
        return workoutDao.observeWeeklyStats(
            weekStartEpochMillisUtc = weekStartEpochMillisUtc,
            weekEndEpochMillisUtcExclusive = weekEndEpochMillisUtcExclusive,
        ).map { weekly ->
            WeeklyWorkoutStats(
                weekStartEpochMillisUtc = weekStartEpochMillisUtc,
                totalWorkouts = weekly.totalWorkouts,
                totalDurationMinutes = weekly.totalDurationMinutes,
                totalCaloriesBurned = weekly.totalCaloriesBurned,
            )
        }
    }
}
