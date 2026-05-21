package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.simplediary.data.local.entity.NutritionRowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionRowDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNutritionRow(row: NutritionRowEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNutritionRows(rows: List<NutritionRowEntity>): List<Long>

    @Query("DELETE FROM nutrition_rows WHERE mealId = :mealId")
    suspend fun deleteByMealId(mealId: Long)

    @Query("SELECT * FROM nutrition_rows WHERE mealId = :mealId ORDER BY id ASC")
    fun observeByMealId(mealId: Long): Flow<List<NutritionRowEntity>>
}
