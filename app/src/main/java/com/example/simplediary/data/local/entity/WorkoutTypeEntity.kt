package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_types",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["name"]),
        Index(value = ["sortOrder"]),
    ],
)
data class WorkoutTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val categoryId: Long,
    val name: String,
    val sortOrder: Int,
)
