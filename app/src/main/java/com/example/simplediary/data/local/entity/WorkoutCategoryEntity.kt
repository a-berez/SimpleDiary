package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_categories",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["sortOrder"]),
    ],
)
data class WorkoutCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val sortOrder: Int,
)
