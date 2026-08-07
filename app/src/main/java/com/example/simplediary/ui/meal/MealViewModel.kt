package com.example.simplediary.ui.meal

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.simplediary.app.SimpleDiaryApplication
import com.example.simplediary.data.local.entity.FoodItemEntity
import com.example.simplediary.data.local.entity.FoodItemSource
import com.example.simplediary.data.local.entity.MealEntity
import com.example.simplediary.data.local.entity.NutritionRowEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.UUID

class MealViewModel(
    application: Application,
    private val mealId: Long?,
) : AndroidViewModel(application) {

    private val app = application as SimpleDiaryApplication
    private val mealDao = app.appDatabase.mealDao()
    private val nutritionRowDao = app.appDatabase.nutritionRowDao()
    private val foodItemDao = app.appDatabase.foodItemDao()
    private val photoCompressor = app.photoCompressor

    private val _uiState = MutableStateFlow(
        MealUiState(
            isEditMode = mealId != null,
            timestampEpochMillis = System.currentTimeMillis(),
            nutritionRows = listOf(NutritionRowInput()),
        )
    )
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MealEvent>()
    val events: SharedFlow<MealEvent> = _events.asSharedFlow()
    private var allFoodItems: List<FoodItemUiModel> = emptyList()

    init {
        viewModelScope.launch {
            foodItemDao.observeAllFoodItems().collectLatest { foodItems ->
                allFoodItems = foodItems.map { it.toUiModel() }
                updateVisibleFoodItems()
            }
        }
        if (mealId != null) {
            viewModelScope.launch {
                loadMeal(mealId)
            }
        }
    }

    fun onFoodSearchQueryChanged(query: String) {
        _uiState.update { it.copy(foodSearchQuery = query) }
        updateVisibleFoodItems()
    }

    fun onFoodItemSelected(foodItemId: Long, portionMultiplier: Double = 1.0) {
        val foodItem = allFoodItems.firstOrNull { it.id == foodItemId } ?: return
        val multiplier = portionMultiplier.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        _uiState.update { state ->
            state.copy(
                nutritionRows = state.nutritionRows + NutritionRowInput(
                    sourceFoodItemId = foodItem.id,
                    itemName = foodItem.name,
                    calories = foodItem.caloriesKcal.scaledBy(multiplier).orEmpty(),
                    proteins = foodItem.proteinsGrams.scaledBy(multiplier).orEmpty(),
                    fats = foodItem.fatsGrams.scaledBy(multiplier).orEmpty(),
                    carbs = foodItem.carbsGrams.scaledBy(multiplier).orEmpty(),
                )
            )
        }
    }

    fun onTimestampChanged(timestampEpochMillis: Long) {
        _uiState.update { it.copy(timestampEpochMillis = timestampEpochMillis) }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onAddNutritionRow() {
        _uiState.update { state ->
            state.copy(nutritionRows = state.nutritionRows + NutritionRowInput())
        }
    }

    fun onDeleteNutritionRow(rowId: String) {
        _uiState.update { state ->
            val updated = state.nutritionRows.filterNot { it.localId == rowId }
            state.copy(nutritionRows = if (updated.isEmpty()) listOf(NutritionRowInput()) else updated)
        }
    }

    fun onNutritionRowChanged(updatedRow: NutritionRowInput) {
        _uiState.update { state ->
            state.copy(
                nutritionRows = state.nutritionRows.map { row ->
                    if (row.localId == updatedRow.localId) updatedRow else row
                }
            )
        }
    }

    fun onPhotoRemoved() {
        _uiState.update { it.copy(photoPath = null) }
    }

    fun onPhotoPicked(sourceUri: Uri) {
        viewModelScope.launch {
            runCatching {
                val compressedPath = compressPickedPhoto(sourceUri)
                _uiState.update { it.copy(photoPath = compressedPath) }
            }.onFailure { throwable ->
                _events.emit(MealEvent.Error(throwable.message ?: "Unable to process selected image"))
            }
        }
    }

    fun onSaveClick() {
        val currentState = _uiState.value
        if (currentState.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            runCatching {
                saveMeal(currentState)
            }.onSuccess {
                _events.emit(MealEvent.Saved)
            }.onFailure { throwable ->
                _events.emit(MealEvent.Error(throwable.message ?: "Failed to save meal"))
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun onDeleteClick() {
        val id = mealId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                mealDao.deleteMealById(id)
            }.onSuccess {
                _events.emit(MealEvent.Deleted)
            }.onFailure { throwable ->
                _events.emit(MealEvent.Error(throwable.message ?: "Failed to delete meal"))
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun loadMeal(mealId: Long) {
        runCatching {
            val meal = mealDao.getMealById(mealId) ?: return
            val rows = nutritionRowDao.getByMealId(mealId)
            _uiState.update {
                it.copy(
                    timestampEpochMillis = meal.timestampEpochMillis,
                    note = meal.text,
                    photoPath = meal.photoPath,
                    nutritionRows = if (rows.isEmpty()) {
                        listOf(NutritionRowInput())
                    } else {
                        rows.map { row ->
                            NutritionRowInput(
                                localId = row.id.toString(),
                                itemName = row.itemName,
                                calories = row.caloriesKcal?.toCleanString().orEmpty(),
                                proteins = row.proteinsGrams?.toCleanString().orEmpty(),
                                fats = row.fatsGrams?.toCleanString().orEmpty(),
                                carbs = row.carbsGrams?.toCleanString().orEmpty(),
                            )
                        }
                    },
                )
            }
        }.onFailure { throwable ->
            _events.emit(MealEvent.Error(throwable.message ?: "Failed to load meal"))
        }
    }

    private suspend fun saveMeal(state: MealUiState) {
        val nutritionRows = state.nutritionRows
            .map { input ->
                input.toEntityOrNull()?.let { entity ->
                    NutritionRowToSave(
                        sourceFoodItemId = input.sourceFoodItemId,
                        entity = entity,
                    )
                }
            }
            .filterNotNull()

        app.appDatabase.withTransaction {
            val targetMealId = if (mealId == null) {
                mealDao.insertMeal(
                    MealEntity(
                        text = state.note.trim(),
                        photoPath = state.photoPath,
                        timestampEpochMillis = state.timestampEpochMillis,
                    )
                )
            } else {
                mealDao.updateMeal(
                    MealEntity(
                        id = mealId,
                        text = state.note.trim(),
                        photoPath = state.photoPath,
                        timestampEpochMillis = state.timestampEpochMillis,
                    )
                )
                nutritionRowDao.deleteByMealId(mealId)
                mealId
            }

            if (nutritionRows.isNotEmpty()) {
                nutritionRowDao.insertNutritionRows(
                    nutritionRows.map { rowToSave ->
                        rowToSave.entity.copy(mealId = targetMealId)
                    }
                )
                saveNutritionRowsToFoodLibrary(
                    rows = nutritionRows,
                    usedAt = state.timestampEpochMillis,
                )
            }
        }
    }

    private suspend fun saveNutritionRowsToFoodLibrary(
        rows: List<NutritionRowToSave>,
        usedAt: Long,
    ) {
        rows.forEach { rowToSave ->
            val sourceFoodItemId = rowToSave.sourceFoodItemId
            if (sourceFoodItemId != null) {
                foodItemDao.markFoodItemUsed(sourceFoodItemId, usedAt)
                return@forEach
            }

            val row = rowToSave.entity
            val name = row.itemName.trim()
            if (name.isBlank() || name == DEFAULT_ITEM_NAME) return@forEach

            val normalizedName = name.normalizedFoodName()
            val existing = foodItemDao.findMatchingFoodItem(
                normalizedName = normalizedName,
                source = FoodItemSource.MANUAL,
                caloriesKcal = row.caloriesKcal,
                proteinsGrams = row.proteinsGrams,
                fatsGrams = row.fatsGrams,
                carbsGrams = row.carbsGrams,
            )
            if (existing != null) {
                foodItemDao.markFoodItemUsed(existing.id, usedAt)
            } else {
                val now = System.currentTimeMillis()
                foodItemDao.insertFoodItem(
                    FoodItemEntity(
                        name = name,
                        normalizedName = normalizedName,
                        caloriesKcal = row.caloriesKcal,
                        proteinsGrams = row.proteinsGrams,
                        fatsGrams = row.fatsGrams,
                        carbsGrams = row.carbsGrams,
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
            }
        }
    }

    private fun updateVisibleFoodItems() {
        val query = _uiState.value.foodSearchQuery.normalizedFoodName()
        val visible = allFoodItems
            .asSequence()
            .filter { item -> query.isBlank() || item.normalizedName.contains(query) }
            .take(MAX_VISIBLE_FOOD_ITEMS)
            .toList()
        _uiState.update { it.copy(foodItems = visible) }
    }

    private fun FoodItemEntity.toUiModel(): FoodItemUiModel {
        return FoodItemUiModel(
            id = id,
            name = name,
            normalizedName = normalizedName,
            caloriesKcal = caloriesKcal,
            proteinsGrams = proteinsGrams,
            fatsGrams = fatsGrams,
            carbsGrams = carbsGrams,
            source = source,
        )
    }

    private suspend fun compressPickedPhoto(sourceUri: Uri): String {
        val cacheDir = app.cacheDir
        val inputFile = File(cacheDir, "picked_${UUID.randomUUID()}.img")
        val outputFile = File(app.filesDir, "photos/meal_${System.currentTimeMillis()}.jpg")

        copyUriToFile(sourceUri, inputFile)

        return photoCompressor.compressToJpeg(
            inputPath = inputFile.absolutePath,
            outputPath = outputFile.absolutePath,
            maxSidePx = 1200,
            qualityPercent = 85,
        ).also {
            inputFile.delete()
        }
    }

    private fun copyUriToFile(sourceUri: Uri, destination: File) {
        val inputStream = app.contentResolver.openInputStream(sourceUri)
            ?: if (sourceUri.scheme == "file") {
                sourceUri.path?.let { path -> FileInputStream(File(path)) }
            } else {
                null
            }
            ?: error("Unable to read picked image: $sourceUri")

        inputStream.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun NutritionRowInput.toEntityOrNull(): NutritionRowEntity? {
        val name = itemName.trim()
        val caloriesValue = calories.parseOptionalDouble()
        val proteinsValue = proteins.parseOptionalDouble()
        val fatsValue = fats.parseOptionalDouble()
        val carbsValue = carbs.parseOptionalDouble()
        val hasAnyValue = name.isNotEmpty() || caloriesValue != null || proteinsValue != null || fatsValue != null || carbsValue != null
        if (!hasAnyValue) return null
        return NutritionRowEntity(
            mealId = 0L,
            itemName = name.ifBlank { DEFAULT_ITEM_NAME },
            caloriesKcal = caloriesValue,
            proteinsGrams = proteinsValue,
            fatsGrams = fatsValue,
            carbsGrams = carbsValue,
        )
    }

    companion object {
        fun factory(application: Application, mealId: Long?): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(MealViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return MealViewModel(application, mealId) as T
                }
            }
        }
    }
}

data class MealUiState(
    val isEditMode: Boolean,
    val isBusy: Boolean = false,
    val timestampEpochMillis: Long,
    val photoPath: String? = null,
    val note: String = "",
    val nutritionRows: List<NutritionRowInput>,
    val foodSearchQuery: String = "",
    val foodItems: List<FoodItemUiModel> = emptyList(),
)

data class NutritionRowInput(
    val localId: String = UUID.randomUUID().toString(),
    val sourceFoodItemId: Long? = null,
    val itemName: String = "",
    val calories: String = "",
    val proteins: String = "",
    val fats: String = "",
    val carbs: String = "",
)

data class FoodItemUiModel(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val caloriesKcal: Double?,
    val proteinsGrams: Double?,
    val fatsGrams: Double?,
    val carbsGrams: Double?,
    val source: String,
)

private data class NutritionRowToSave(
    val sourceFoodItemId: Long?,
    val entity: NutritionRowEntity,
)

sealed interface MealEvent {
    data object Saved : MealEvent
    data object Deleted : MealEvent
    data class Error(val message: String) : MealEvent
}

private fun String.parseOptionalDouble(): Double? = trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()

private fun String.normalizedFoodName(): String {
    return trim()
        .lowercase(Locale.getDefault())
        .replace(Regex("\\s+"), " ")
}

private fun Double.toCleanString(): String {
    val longValue = toLong()
    return if (this == longValue.toDouble()) longValue.toString() else toString()
}

private fun Double?.scaledBy(multiplier: Double): String? {
    return this?.let { (it * multiplier).toCleanString() }
}

private const val DEFAULT_ITEM_NAME = "Item"
private const val MAX_VISIBLE_FOOD_ITEMS = 50
