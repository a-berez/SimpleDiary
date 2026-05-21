package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.simplediary.data.local.entity.WorkoutTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTypeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertType(type: WorkoutTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTypes(types: List<WorkoutTypeEntity>): List<Long>

    @Update
    suspend fun updateType(type: WorkoutTypeEntity)

    @Delete
    suspend fun deleteType(type: WorkoutTypeEntity)

    @Query("DELETE FROM workout_types WHERE id = :typeId")
    suspend fun deleteTypeById(typeId: Long)

    @Query("SELECT * FROM workout_types WHERE id = :typeId LIMIT 1")
    suspend fun getTypeById(typeId: Long): WorkoutTypeEntity?

    @Query("SELECT * FROM workout_types WHERE categoryId = :categoryId ORDER BY sortOrder ASC, name ASC")
    suspend fun getTypesByCategory(categoryId: Long): List<WorkoutTypeEntity>

    @Query("SELECT * FROM workout_types WHERE categoryId = :categoryId ORDER BY sortOrder ASC, name ASC")
    fun observeTypesByCategory(categoryId: Long): Flow<List<WorkoutTypeEntity>>

    @Query("SELECT * FROM workout_types ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllTypes(): List<WorkoutTypeEntity>

    @Query("SELECT * FROM workout_types ORDER BY sortOrder ASC, name ASC")
    fun observeAllTypes(): Flow<List<WorkoutTypeEntity>>
}
