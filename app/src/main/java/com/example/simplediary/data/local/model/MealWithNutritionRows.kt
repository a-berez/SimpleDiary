package com.example.simplediary.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.simplediary.data.local.entity.MealEntity
import com.example.simplediary.data.local.entity.NutritionRowEntity

data class MealWithNutritionRows(
    @Embedded
    val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId",
    )
    val nutritionRows: List<NutritionRowEntity>,
)
