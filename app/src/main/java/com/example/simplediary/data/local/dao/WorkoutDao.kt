package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.simplediary.data.local.entity.WorkoutEntity
import com.example.simplediary.data.local.model.WeeklyWorkoutStatsDbRow
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
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:typeId IS NULL OR typeId = :typeId)
        ORDER BY dateEpochMillisUtcStart DESC
        """
    )
    fun observeWorkouts(
        fromEpochMillisInclusive: Long? = null,
        toEpochMillisInclusive: Long? = null,
        categoryId: Long? = null,
        typeId: Long? = null,
    ): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts ORDER BY dateEpochMillisUtcStart DESC")
    suspend fun getAllWorkouts(): List<WorkoutEntity>

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
