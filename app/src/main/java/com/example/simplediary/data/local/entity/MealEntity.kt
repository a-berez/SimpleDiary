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
    /** Unified hunger/satiety scale (1–10) before the meal, or null if not set. */
    val hungerBefore: Int? = null,
    /** Same hunger/satiety scale (1–10) after the meal, or null if not set. */
    val satietyAfter: Int? = null,
)
