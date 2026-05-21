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
                (
                    SELECT
                        ('Items: ' || COUNT(*) ||
                        ' • K:' || CAST(ROUND(COALESCE(SUM(nr.caloriesKcal), 0.0), 0) AS INTEGER) ||
                        ' P:' || CAST(ROUND(COALESCE(SUM(nr.proteinsGrams), 0.0), 0) AS INTEGER) ||
                        ' F:' || CAST(ROUND(COALESCE(SUM(nr.fatsGrams), 0.0), 0) AS INTEGER) ||
                        ' C:' || CAST(ROUND(COALESCE(SUM(nr.carbsGrams), 0.0), 0) AS INTEGER))
                    FROM nutrition_rows nr
                    WHERE nr.mealId = m.id
                ) AS subtitle,
                (
                    SELECT
                        COALESCE(
                            GROUP_CONCAT(
                                COALESCE(NULLIF(TRIM(nr.itemName), ''), 'Item') ||
                                ' - K:' || CAST(ROUND(COALESCE(nr.caloriesKcal, 0.0), 0) AS INTEGER) ||
                                ' P:' || CAST(ROUND(COALESCE(nr.proteinsGrams, 0.0), 0) AS INTEGER) ||
                                ' F:' || CAST(ROUND(COALESCE(nr.fatsGrams, 0.0), 0) AS INTEGER) ||
                                ' C:' || CAST(ROUND(COALESCE(nr.carbsGrams, 0.0), 0) AS INTEGER),
                                CHAR(10)
                            ),
                            'No nutrition rows'
                        ) ||
                        CHAR(10) || CHAR(10) ||
                        'Total - K:' || CAST(ROUND(COALESCE(SUM(nr.caloriesKcal), 0.0), 0) AS INTEGER) ||
                        ' P:' || CAST(ROUND(COALESCE(SUM(nr.proteinsGrams), 0.0), 0) AS INTEGER) ||
                        ' F:' || CAST(ROUND(COALESCE(SUM(nr.fatsGrams), 0.0), 0) AS INTEGER) ||
                        ' C:' || CAST(ROUND(COALESCE(SUM(nr.carbsGrams), 0.0), 0) AS INTEGER)
                    FROM nutrition_rows nr
                    WHERE nr.mealId = m.id
                ) AS expandedDetails,
                m.photoPath AS photoPath
            FROM meals m

            UNION ALL

            SELECT
                w.id AS entryId,
                'WORKOUT' AS entryType,
                w.dateEpochMillisUtcStart AS timestampEpochMillis,
                COALESCE(wt.name, wc.name, 'Workout') AS title,
                ('Duration: ' || w.durationMinutes || ' min') AS subtitle,
                ('Category: ' || COALESCE(wc.name, '-') || CHAR(10) ||
                 'Type: ' || COALESCE(wt.name, '-') || CHAR(10) ||
                 'Duration: ' || w.durationMinutes || ' min' || CHAR(10) ||
                 'Calories burned: ' || CAST(ROUND(COALESCE(w.caloriesBurned, 0.0), 0) AS INTEGER) || CHAR(10) ||
                 'Note: ' || COALESCE(NULLIF(TRIM(w.note), ''), '-')) AS expandedDetails,
                NULL AS photoPath
            FROM workouts w
            LEFT JOIN workout_categories wc ON wc.id = w.categoryId
            LEFT JOIN workout_types wt ON wt.id = w.typeId

            UNION ALL

            SELECT
                s.id AS entryId,
                'STATE_NOTE' AS entryType,
                s.timestampEpochMillis AS timestampEpochMillis,
                s.text AS title,
                NULL AS subtitle,
                s.text AS expandedDetails,
                s.photoPath AS photoPath
            FROM state_notes s
        )
        WHERE (:fromEpochMillisInclusive IS NULL OR timestampEpochMillis >= :fromEpochMillisInclusive)
          AND (:toEpochMillisInclusive IS NULL OR timestampEpochMillis <= :toEpochMillisInclusive)
          AND (
              (entryType = 'MEAL' AND :includeMeals = 1) OR
              (entryType = 'WORKOUT' AND :includeWorkouts = 1) OR
              (entryType = 'STATE_NOTE' AND :includeStateNotes = 1)
          )
        ORDER BY timestampEpochMillis DESC
        """
    )
    fun observeFeed(
        includeMeals: Boolean,
        includeWorkouts: Boolean,
        includeStateNotes: Boolean,
        fromEpochMillisInclusive: Long? = null,
        toEpochMillisInclusive: Long? = null,
    ): Flow<List<FeedDbRow>>
}
