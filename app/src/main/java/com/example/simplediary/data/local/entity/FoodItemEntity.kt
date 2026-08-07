package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_items",
    indices = [
        Index(value = ["normalizedName"]),
        Index(value = ["source", "sourceKey"], unique = true),
        Index(value = ["lastUsedAt"]),
        Index(value = ["useCount"]),
    ],
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val normalizedName: String,
    val caloriesKcal: Double?,
    val proteinsGrams: Double?,
    val fatsGrams: Double?,
    val carbsGrams: Double?,
    val weightGrams: Double?,
    val source: String,
    val sourceKey: String?,
    val ingredients: String?,
    val useCount: Int,
    val lastUsedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

object FoodItemSource {
    const val MANUAL = "MANUAL"
    const val GROW_FOOD = "GROW_FOOD"
}
