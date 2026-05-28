package com.example.simplediary.data.files

import android.content.Context
import android.net.Uri
import com.example.simplediary.core.files.CsvExporter
import com.example.simplediary.data.local.dao.MealDao
import com.example.simplediary.data.local.dao.StateNoteDao
import com.example.simplediary.data.local.dao.WorkoutCategoryDao
import com.example.simplediary.data.local.dao.WorkoutDao
import com.example.simplediary.data.local.dao.WorkoutTypeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipCsvExporter(
    private val context: Context,
    private val mealDao: MealDao,
    private val workoutDao: WorkoutDao,
    private val workoutCategoryDao: WorkoutCategoryDao,
    private val workoutTypeDao: WorkoutTypeDao,
    private val stateNoteDao: StateNoteDao,
) : CsvExporter {
    override suspend fun exportAllData(destinationZipUri: Uri): Uri = withContext(Dispatchers.IO) {
        val meals = mealDao.getAllMealsWithNutritionRows()
        val workouts = workoutDao.getAllWorkouts()
        val categoriesById = workoutCategoryDao.getAllCategories().associateBy { it.id }
        val typesById = workoutTypeDao.getAllTypes().associateBy { it.id }
        val stateNotes = stateNoteDao.getAllStateNotes()

        val resolver = context.contentResolver
        resolver.openOutputStream(destinationZipUri, "w")?.use { outputStream ->
            ZipOutputStream(outputStream).use { zip ->
                zip.writeEntry(
                    name = "meals.csv",
                    content = buildMealsCsv(meals),
                )
                zip.writeEntry(
                    name = "workouts.csv",
                    content = buildWorkoutsCsv(
                        workouts = workouts,
                        categoriesById = categoriesById,
                        typesById = typesById,
                    ),
                )
                zip.writeEntry(
                    name = "state_notes.csv",
                    content = buildStateNotesCsv(stateNotes),
                )
            }
        } ?: error("Unable to open destination URI for CSV export: $destinationZipUri")

        destinationZipUri
    }

    private fun buildMealsCsv(
        meals: List<com.example.simplediary.data.local.model.MealWithNutritionRows>
    ): String {
        val builder = StringBuilder()
        builder.append(
            "Дата;Заметка;Продукт;Калории;Белки;Жиры;Углеводы\n"
        )
        meals.forEach { mealWithRows ->
            val meal = mealWithRows.meal
            if (mealWithRows.nutritionRows.isEmpty()) {
                builder.appendCsvRow(
                    formatTimestamp(meal.timestampEpochMillis),
                    meal.text,
                    "",
                    "",
                    "",
                    "",
                    "",
                )
            } else {
                mealWithRows.nutritionRows.forEach { row ->
                    builder.appendCsvRow(
                        formatTimestamp(meal.timestampEpochMillis),
                        meal.text,
                        row.itemName,
                        row.caloriesKcal,
                        row.proteinsGrams,
                        row.fatsGrams,
                        row.carbsGrams,
                    )
                }
            }
        }
        return builder.toString()
    }

    private fun buildWorkoutsCsv(
        workouts: List<com.example.simplediary.data.local.entity.WorkoutEntity>,
        categoriesById: Map<Long, com.example.simplediary.data.local.entity.WorkoutCategoryEntity>,
        typesById: Map<Long, com.example.simplediary.data.local.entity.WorkoutTypeEntity>,
    ): String {
        val builder = StringBuilder()
        builder.append("Дата;Категория;Тип;Длительность (мин);Калории сожжено;Заметка\n")
        workouts.forEach { workout ->
            val categoryName = categoriesById[workout.categoryId]?.name.orEmpty()
            val typeName = workout.typeId?.let { typeId -> typesById[typeId]?.name }.orEmpty()
            builder.appendCsvRow(
                formatTimestamp(workout.dateEpochMillisUtcStart),
                categoryName,
                typeName,
                workout.durationMinutes,
                workout.caloriesBurned,
                workout.note,
            )
        }
        return builder.toString()
    }

    private fun buildStateNotesCsv(
        stateNotes: List<com.example.simplediary.data.local.entity.StateNoteEntity>
    ): String {
        val builder = StringBuilder()
        builder.append("Дата;Категория;Текст\n")
        stateNotes.forEach { note ->
            builder.appendCsvRow(
                formatTimestamp(note.timestampEpochMillis),
                note.category,
                note.text,
            )
        }
        return builder.toString()
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun StringBuilder.appendCsvRow(vararg values: Any?) {
        values.forEachIndexed { index, value ->
            if (index > 0) append(';')
            append(escapeCsv(value?.toString()))
        }
        append('\n')
    }

    private fun escapeCsv(raw: String?): String {
        val value = raw ?: ""
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun formatTimestamp(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DATE_TIME_FORMATTER)
    }

    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
