package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.simplediary.data.local.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoodItem(foodItem: FoodItemEntity): Long

    @Update
    suspend fun updateFoodItem(foodItem: FoodItemEntity)

    @Query("DELETE FROM food_items WHERE id = :foodItemId")
    suspend fun deleteFoodItemById(foodItemId: Long)

    @Query("SELECT * FROM food_items ORDER BY useCount DESC, lastUsedAt DESC, name COLLATE NOCASE ASC")
    fun observeAllFoodItems(): Flow<List<FoodItemEntity>>

    @Query(
        """
        SELECT * FROM food_items
        WHERE normalizedName LIKE '%' || :query || '%'
        ORDER BY useCount DESC, lastUsedAt DESC, name COLLATE NOCASE ASC
        """
    )
    fun observeFoodItemsByQuery(query: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :foodItemId LIMIT 1")
    suspend fun getFoodItemById(foodItemId: Long): FoodItemEntity?

    @Query(
        """
        SELECT * FROM food_items
        WHERE source = :source
          AND sourceKey = :sourceKey
        LIMIT 1
        """
    )
    suspend fun getBySourceKey(source: String, sourceKey: String): FoodItemEntity?

    @Query(
        """
        SELECT * FROM food_items
        WHERE normalizedName = :normalizedName
          AND (
            (caloriesKcal IS NULL AND :caloriesKcal IS NULL)
            OR ROUND(caloriesKcal, 0) = ROUND(:caloriesKcal, 0)
          )
          AND (
            (proteinsGrams IS NULL AND :proteinsGrams IS NULL)
            OR ROUND(proteinsGrams, 1) = ROUND(:proteinsGrams, 1)
          )
          AND (
            (fatsGrams IS NULL AND :fatsGrams IS NULL)
            OR ROUND(fatsGrams, 1) = ROUND(:fatsGrams, 1)
          )
          AND (
            (carbsGrams IS NULL AND :carbsGrams IS NULL)
            OR ROUND(carbsGrams, 1) = ROUND(:carbsGrams, 1)
          )
        ORDER BY useCount DESC, lastUsedAt DESC
        LIMIT 1
        """
    )
    suspend fun findMatchingFoodItem(
        normalizedName: String,
        caloriesKcal: Double?,
        proteinsGrams: Double?,
        fatsGrams: Double?,
        carbsGrams: Double?,
    ): FoodItemEntity?


    @Query(
        """
        UPDATE food_items
        SET useCount = useCount + 1,
            lastUsedAt = :usedAt,
            updatedAt = :usedAt
        WHERE id = :foodItemId
        """
    )
    suspend fun markFoodItemUsed(foodItemId: Long, usedAt: Long)
}
