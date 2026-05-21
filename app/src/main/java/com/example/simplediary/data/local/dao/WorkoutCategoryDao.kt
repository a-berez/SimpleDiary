package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.simplediary.data.local.entity.WorkoutCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutCategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: WorkoutCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategories(categories: List<WorkoutCategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: WorkoutCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: WorkoutCategoryEntity)

    @Query("DELETE FROM workout_categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Query("SELECT * FROM workout_categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: Long): WorkoutCategoryEntity?

    @Query("SELECT * FROM workout_categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCategories(): List<WorkoutCategoryEntity>

    @Query("SELECT * FROM workout_categories ORDER BY sortOrder ASC, name ASC")
    fun observeAllCategories(): Flow<List<WorkoutCategoryEntity>>
}
