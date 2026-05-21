package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.simplediary.data.local.entity.MealEntity
import com.example.simplediary.data.local.model.DailyMacroTotalsDbRow
import com.example.simplediary.data.local.model.MealWithNutritionRows
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)

    @Query("SELECT * FROM meals WHERE id = :mealId LIMIT 1")
    suspend fun getMealById(mealId: Long): MealEntity?

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId LIMIT 1")
    fun observeMealWithNutritionRows(mealId: Long): Flow<MealWithNutritionRows?>

    @Transaction
    @Query(
        """
        SELECT * FROM meals
        WHERE (:fromEpochMillisInclusive IS NULL OR timestampEpochMillis >= :fromEpochMillisInclusive)
          AND (:toEpochMillisInclusive IS NULL OR timestampEpochMillis <= :toEpochMillisInclusive)
        ORDER BY timestampEpochMillis DESC
        """
    )
    fun observeMealsWithNutritionRows(
        fromEpochMillisInclusive: Long? = null,
        toEpochMillisInclusive: Long? = null,
    ): Flow<List<MealWithNutritionRows>>

    @Query(
        """
        SELECT
            COALESCE(SUM(nr.proteinsGrams), 0.0) AS actualProteinsGrams,
            COALESCE(SUM(nr.fatsGrams), 0.0) AS actualFatsGrams,
            COALESCE(SUM(nr.carbsGrams), 0.0) AS actualCarbsGrams,
            COALESCE(SUM(nr.caloriesKcal), 0.0) AS actualCaloriesKcal
        FROM meals m
        LEFT JOIN nutrition_rows nr ON nr.mealId = m.id
        WHERE m.timestampEpochMillis >= :dayStartEpochMillisUtc
          AND m.timestampEpochMillis < :dayEndEpochMillisUtcExclusive
        """
    )
    fun observeDailyMacroTotals(
        dayStartEpochMillisUtc: Long,
        dayEndEpochMillisUtcExclusive: Long,
    ): Flow<DailyMacroTotalsDbRow>
}
