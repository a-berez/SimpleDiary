package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.simplediary.data.local.model.FeedDbRow
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query(
        """
        SELECT * FROM (
            SELECT
                m.id AS entryId,
                'MEAL' AS entryType,
                m.timestampEpochMillis AS timestampEpochMillis,
                m.text AS title,
                NULL AS subtitle,
                m.photoPath AS photoPath,
                NULL AS workoutType
            FROM meals m

            UNION ALL

            SELECT
                w.id AS entryId,
                'WORKOUT' AS entryType,
                w.dateEpochMillisUtcStart AS timestampEpochMillis,
                w.type AS title,
                ('Duration: ' || w.durationMinutes || ' min, Calories: ' || w.caloriesBurned || ' kcal') AS subtitle,
                NULL AS photoPath,
                w.type AS workoutType
            FROM workouts w

            UNION ALL

            SELECT
                s.id AS entryId,
                'STATE_NOTE' AS entryType,
                s.timestampEpochMillis AS timestampEpochMillis,
                s.text AS title,
                NULL AS subtitle,
                s.photoPath AS photoPath,
                NULL AS workoutType
            FROM state_notes s
        )
        WHERE (:fromEpochMillisInclusive IS NULL OR timestampEpochMillis >= :fromEpochMillisInclusive)
          AND (:toEpochMillisInclusive IS NULL OR timestampEpochMillis <= :toEpochMillisInclusive)
          AND (
              (entryType = 'MEAL' AND :includeMeals = 1) OR
              (entryType = 'WORKOUT' AND :includeWorkouts = 1) OR
              (entryType = 'STATE_NOTE' AND :includeStateNotes = 1)
          )
          AND (entryType != 'WORKOUT' OR :workoutType IS NULL OR workoutType = :workoutType)
        ORDER BY timestampEpochMillis DESC
        """
    )
    fun observeFeed(
        includeMeals: Boolean,
        includeWorkouts: Boolean,
        includeStateNotes: Boolean,
        workoutType: String? = null,
        fromEpochMillisInclusive: Long? = null,
        toEpochMillisInclusive: Long? = null,
    ): Flow<List<FeedDbRow>>
}
