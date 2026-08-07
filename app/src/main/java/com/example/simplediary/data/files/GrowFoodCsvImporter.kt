package com.example.simplediary.data.files

import android.content.Context
import android.net.Uri
import com.example.simplediary.data.local.dao.FoodItemDao
import com.example.simplediary.data.local.entity.FoodItemEntity
import com.example.simplediary.data.local.entity.FoodItemSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class GrowFoodCsvImporter(
    private val context: Context,
    private val foodItemDao: FoodItemDao,
) {
    suspend fun importFoodItems(sourceUri: Uri): GrowFoodImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val rows = resolver.openInputStream(sourceUri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).use { reader ->
                parseCsv(reader.readText())
            }
        } ?: error("Unable to open Grow Food CSV: $sourceUri")

        var inserted = 0
        var updated = 0
        var skipped = 0

        rows.forEach { row ->
            if (row.isRemoved()) {
                skipped += 1
                return@forEach
            }

            val name = row.firstValue("pack_name", "name", "item_name").trim()
            if (name.isBlank()) {
                skipped += 1
                return@forEach
            }

            val normalizedName = name.normalizedFoodName()
            val sourceKey = row.sourceKey(normalizedName)
            val now = System.currentTimeMillis()
            val existing = foodItemDao.getBySourceKey(
                source = FoodItemSource.GROW_FOOD,
                sourceKey = sourceKey,
            )
            val next = FoodItemEntity(
                id = existing?.id ?: 0L,
                name = name,
                normalizedName = normalizedName,
                caloriesKcal = row.firstValue("calories").parseOptionalDouble(),
                proteinsGrams = row.firstValue("proteins").parseOptionalDouble(),
                fatsGrams = row.firstValue("fats").parseOptionalDouble(),
                carbsGrams = row.firstValue("carbs").parseOptionalDouble(),
                weightGrams = row.firstValue("weight").parseOptionalDouble(),
                source = FoodItemSource.GROW_FOOD,
                sourceKey = sourceKey,
                ingredients = row.firstValue("ingredients").trim().ifBlank { null },
                useCount = existing?.useCount ?: 0,
                lastUsedAt = existing?.lastUsedAt,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )

            if (existing == null) {
                val insertedId = foodItemDao.insertFoodItem(next)
                if (insertedId > 0L) {
                    inserted += 1
                } else {
                    foodItemDao.getBySourceKey(FoodItemSource.GROW_FOOD, sourceKey)?.let { duplicate ->
                        foodItemDao.updateFoodItem(next.copy(id = duplicate.id))
                        updated += 1
                    } ?: run {
                        skipped += 1
                    }
                }
            } else {
                foodItemDao.updateFoodItem(next)
                updated += 1
            }
        }

        GrowFoodImportResult(
            inserted = inserted,
            updated = updated,
            skipped = skipped,
        )
    }

    private fun parseCsv(content: String): List<Map<String, String>> {
        val headerLine = content.lineSequence().firstOrNull().orEmpty().removePrefix("\uFEFF")
        if (headerLine.isBlank()) return emptyList()
        val delimiter = detectDelimiter(headerLine)
        val records = parseCsvRecords(content, delimiter)
            .filter { record -> record.any { it.isNotBlank() } }
        if (records.isEmpty()) return emptyList()

        val headers = records.first().mapIndexed { index, header ->
            if (index == 0) header.removePrefix("\uFEFF").trim() else header.trim()
        }
        if (headers.isEmpty()) return emptyList()

        return records.drop(1).mapNotNull { values ->
            if (values.all { it.isBlank() }) {
                null
            } else {
                headers.mapIndexed { index, header ->
                    header to values.getOrElse(index) { "" }
                }.toMap()
            }
        }
    }

    private fun detectDelimiter(header: String): Char {
        return if (header.count { it == ';' } > header.count { it == ',' }) ';' else ','
    }

    private fun parseCsvRecords(content: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < content.length) {
            val char = content[index]
            when {
                char == '"' && inQuotes && index + 1 < content.length && content[index + 1] == '"' -> {
                    current.append('"')
                    index += 1
                }
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    row += current.toString()
                    current.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    row += current.toString()
                    current.clear()
                    records += row.toList()
                    row.clear()
                    if (char == '\r' && index + 1 < content.length && content[index + 1] == '\n') {
                        index += 1
                    }
                }
                else -> current.append(char)
            }
            index += 1
        }
        if (current.isNotEmpty() || row.isNotEmpty()) {
            row += current.toString()
            records += row.toList()
        }
        return records
    }

    private fun Map<String, String>.firstValue(vararg keys: String): String {
        keys.forEach { expected ->
            entries.firstOrNull { (key, _) -> key.equals(expected, ignoreCase = true) }
                ?.value
                ?.let { return it }
        }
        return ""
    }

    private fun Map<String, String>.isRemoved(): Boolean {
        return firstValue("removed", "remove").trim().lowercase(Locale.US) in setOf("true", "1", "yes", "y")
    }

    private fun Map<String, String>.sourceKey(normalizedName: String): String {
        val packId = firstValue("pack_id", "packId", "id").trim()
        if (packId.isNotBlank()) return "pack:$packId"
        return listOf(
            normalizedName,
            firstValue("calories"),
            firstValue("proteins"),
            firstValue("fats"),
            firstValue("carbs"),
            firstValue("weight"),
        ).joinToString("|")
    }

    private fun String.parseOptionalDouble(): Double? {
        return trim()
            .takeIf { it.isNotEmpty() }
            ?.replace(',', '.')
            ?.toDoubleOrNull()
    }

    private fun String.normalizedFoodName(): String {
        return trim()
            .lowercase(Locale.getDefault())
            .replace(Regex("\\s+"), " ")
    }
}

data class GrowFoodImportResult(
    val inserted: Int,
    val updated: Int,
    val skipped: Int,
)
