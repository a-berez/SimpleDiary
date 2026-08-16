package com.example.simplediary.data.files

import com.example.simplediary.data.local.dao.FoodItemDao
import com.example.simplediary.data.local.entity.FoodItemEntity
import com.example.simplediary.data.local.entity.FoodItemSource
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Shared upsert/dedup for the food library.
 *
 * Identity for content match: normalized name + rounded K/P/F/C (any source).
 * Grow Food rows are additionally keyed by [sourceKey]; re-import updates in place
 * without touching already saved diary [nutrition_rows].
 */
class FoodLibraryWriter(
    private val foodItemDao: FoodItemDao,
) {
    suspend fun markUsed(foodItemId: Long, usedAt: Long) {
        foodItemDao.markFoodItemUsed(foodItemId, usedAt)
    }

    /**
     * Inserts a MANUAL library row or bumps usage if name+macros already exist
     * (including an existing GROW_FOOD row with the same rounded macros).
     * Returns the library item id, or null if the row was skipped (blank/default name).
     */
    suspend fun upsertFromManualRow(
        name: String,
        caloriesKcal: Double?,
        proteinsGrams: Double?,
        fatsGrams: Double?,
        carbsGrams: Double?,
        usedAt: Long,
    ): Long? {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || trimmedName == DEFAULT_ITEM_NAME) return null

        val normalizedName = trimmedName.normalizedFoodName()
        val calories = caloriesKcal.roundCalories()
        val proteins = proteinsGrams.roundMacroGrams()
        val fats = fatsGrams.roundMacroGrams()
        val carbs = carbsGrams.roundMacroGrams()

        val existing = foodItemDao.findMatchingFoodItem(
            normalizedName = normalizedName,
            caloriesKcal = calories,
            proteinsGrams = proteins,
            fatsGrams = fats,
            carbsGrams = carbs,
        )
        if (existing != null) {
            foodItemDao.markFoodItemUsed(existing.id, usedAt)
            return existing.id
        }

        val now = System.currentTimeMillis()
        val insertedId = foodItemDao.insertFoodItem(
            FoodItemEntity(
                name = trimmedName,
                normalizedName = normalizedName,
                caloriesKcal = calories,
                proteinsGrams = proteins,
                fatsGrams = fats,
                carbsGrams = carbs,
                weightGrams = null,
                source = FoodItemSource.MANUAL,
                sourceKey = null,
                ingredients = null,
                useCount = 1,
                lastUsedAt = usedAt,
                createdAt = now,
                updatedAt = now,
            )
        )
        return insertedId.takeIf { it > 0L }
    }

    /**
     * Upserts a GROW_FOOD row by [sourceKey]. Existing rows are updated in place
     * (name/macros/ingredients/weight); usage counters and createdAt are preserved.
     */
    suspend fun upsertFromGrowFood(
        name: String,
        sourceKey: String,
        caloriesKcal: Double?,
        proteinsGrams: Double?,
        fatsGrams: Double?,
        carbsGrams: Double?,
        weightGrams: Double?,
        ingredients: String?,
        now: Long = System.currentTimeMillis(),
    ): FoodLibraryUpsertResult {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Grow Food dish name must not be blank" }
        require(sourceKey.isNotBlank()) { "Grow Food sourceKey must not be blank" }

        val normalizedName = trimmedName.normalizedFoodName()
        val existing = foodItemDao.getBySourceKey(
            source = FoodItemSource.GROW_FOOD,
            sourceKey = sourceKey,
        )
        val next = FoodItemEntity(
            id = existing?.id ?: 0L,
            name = trimmedName,
            normalizedName = normalizedName,
            caloriesKcal = caloriesKcal.roundCalories(),
            proteinsGrams = proteinsGrams.roundMacroGrams(),
            fatsGrams = fatsGrams.roundMacroGrams(),
            carbsGrams = carbsGrams.roundMacroGrams(),
            weightGrams = weightGrams.roundMacroGrams(),
            source = FoodItemSource.GROW_FOOD,
            sourceKey = sourceKey,
            ingredients = ingredients?.trim()?.ifBlank { null },
            useCount = existing?.useCount ?: 0,
            lastUsedAt = existing?.lastUsedAt,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        if (existing == null) {
            val insertedId = foodItemDao.insertFoodItem(next)
            if (insertedId > 0L) {
                return FoodLibraryUpsertResult.Inserted
            }
            val duplicate = foodItemDao.getBySourceKey(FoodItemSource.GROW_FOOD, sourceKey)
            return if (duplicate != null) {
                foodItemDao.updateFoodItem(next.copy(id = duplicate.id))
                FoodLibraryUpsertResult.Updated
            } else {
                FoodLibraryUpsertResult.Skipped
            }
        }

        foodItemDao.updateFoodItem(next)
        return FoodLibraryUpsertResult.Updated
    }
}

enum class FoodLibraryUpsertResult {
    Inserted,
    Updated,
    Skipped,
}

internal fun String.normalizedFoodName(): String {
    return trim()
        .lowercase(Locale.getDefault())
        .replace(Regex("\\s+"), " ")
}

internal fun Double?.roundCalories(): Double? {
    return this?.roundToInt()?.toDouble()
}

internal fun Double?.roundMacroGrams(): Double? {
    return this?.let { (it * 10.0).roundToInt() / 10.0 }
}

private const val DEFAULT_ITEM_NAME = "Item"
