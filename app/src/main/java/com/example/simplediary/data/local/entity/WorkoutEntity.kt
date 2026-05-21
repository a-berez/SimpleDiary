package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["typeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["dateEpochMillisUtcStart"]),
        Index(value = ["categoryId"]),
        Index(value = ["typeId"]),
    ],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateEpochMillisUtcStart: Long,
    val categoryId: Long,
    val typeId: Long?,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val note: String,
)
