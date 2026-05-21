package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nutrition_rows",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["mealId"]),
    ],
)
data class NutritionRowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mealId: Long,
    val itemName: String,
    val proteinsGrams: Double?,
    val fatsGrams: Double?,
    val carbsGrams: Double?,
    val caloriesKcal: Double?,
)
