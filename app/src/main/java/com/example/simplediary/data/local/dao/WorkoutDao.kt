package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.simplediary.data.local.entity.WorkoutEntity
import com.example.simplediary.data.local.model.WeeklyWorkoutStatsDbRow
import com.example.simplediary.domain.model.WorkoutType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: Long)

    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkoutById(workoutId: Long): WorkoutEntity?

    @Query(
        """
        SELECT * FROM workouts
        WHERE (:fromEpochMillisInclusive IS NULL OR dateEpochMillisUtcStart >= :fromEpochMillisInclusive)
          AND (:toEpochMillisInclusive IS NULL OR dateEpochMillisUtcStart <= :toEpochMillisInclusive)
          AND (:workoutType IS NULL OR type = :workoutType)
        ORDER BY dateEpochMillisUtcStart DESC
        """
    )
    fun observeWorkouts(
        fromEpochMillisInclusive: Long? = null,
        toEpochMillisInclusive: Long? = null,
        workoutType: WorkoutType? = null,
    ): Flow<List<WorkoutEntity>>

    @Query(
        """
        SELECT
            :weekStartEpochMillisUtc AS weekStartEpochMillisUtc,
            COUNT(*) AS totalWorkouts,
            COALESCE(SUM(durationMinutes), 0) AS totalDurationMinutes,
            COALESCE(SUM(caloriesBurned), 0) AS totalCaloriesBurned
        FROM workouts
        WHERE dateEpochMillisUtcStart >= :weekStartEpochMillisUtc
          AND dateEpochMillisUtcStart < :weekEndEpochMillisUtcExclusive
        """
    )
    fun observeWeeklyStats(
        weekStartEpochMillisUtc: Long,
        weekEndEpochMillisUtcExclusive: Long,
    ): Flow<WeeklyWorkoutStatsDbRow>
}
