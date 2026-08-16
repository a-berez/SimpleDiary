package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meals",
    indices = [
        Index(value = ["timestampEpochMillis"]),
    ],
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val photoPath: String?,
    val timestampEpochMillis: Long,
    /** Hunger before the meal, 1–10, or null if not set. */
    val hungerBefore: Int? = null,
    /** Satiety after the meal, 1–10, or null if not set. */
    val satietyAfter: Int? = null,
)
