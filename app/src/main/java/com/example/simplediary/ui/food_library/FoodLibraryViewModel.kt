package com.example.simplediary.ui.food_library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplediary.app.SimpleDiaryApplication
import com.example.simplediary.data.local.entity.FoodItemEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class FoodLibraryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as SimpleDiaryApplication
    private val foodItemDao = app.appDatabase.foodItemDao()
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<FoodLibraryUiState> = combine(
        foodItemDao.observeAllFoodItems(),
        searchQuery,
    ) { items, query ->
        val normalizedQuery = query.normalizedFoodName()
        FoodLibraryUiState(
            searchQuery = query,
            items = items
                .asSequence()
                .filter { item -> normalizedQuery.isBlank() || item.normalizedName.contains(normalizedQuery) }
                .map { it.toUiModel() }
                .toList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FoodLibraryUiState(),
    )

    private val _events = MutableSharedFlow<FoodLibraryEvent>()
    val events: SharedFlow<FoodLibraryEvent> = _events.asSharedFlow()

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun updateFoodItem(input: FoodItemEditInput) {
        val name = input.name.trim()
        if (name.isBlank()) {
            viewModelScope.launch { _events.emit(FoodLibraryEvent.Message("Name is required")) }
            return
        }
        viewModelScope.launch {
            runCatching {
                val current = foodItemDao.getFoodItemById(input.id) ?: return@runCatching
                foodItemDao.updateFoodItem(
                    current.copy(
                        name = name,
                        normalizedName = name.normalizedFoodName(),
                        caloriesKcal = input.calories.parseOptionalDouble(),
                        proteinsGrams = input.proteins.parseOptionalDouble(),
                        fatsGrams = input.fats.parseOptionalDouble(),
                        carbsGrams = input.carbs.parseOptionalDouble(),
                        weightGrams = input.weight.parseOptionalDouble(),
                        ingredients = input.ingredients.trim().ifBlank { null },
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }.onSuccess {
                _events.emit(FoodLibraryEvent.Message("Food item updated"))
            }.onFailure { throwable ->
                _events.emit(FoodLibraryEvent.Message(throwable.message ?: "Failed to update food item"))
            }
        }
    }

    fun deleteFoodItem(foodItemId: Long) {
        viewModelScope.launch {
            runCatching { foodItemDao.deleteFoodItemById(foodItemId) }
                .onSuccess { _events.emit(FoodLibraryEvent.Message("Food item deleted")) }
                .onFailure { throwable ->
                    _events.emit(FoodLibraryEvent.Message(throwable.message ?: "Failed to delete food item"))
                }
        }
    }

    private fun FoodItemEntity.toUiModel(): FoodLibraryItemUi {
        return FoodLibraryItemUi(
            id = id,
            name = name,
            calories = caloriesKcal.toInputString(),
            proteins = proteinsGrams.toInputString(),
            fats = fatsGrams.toInputString(),
            carbs = carbsGrams.toInputString(),
            weight = weightGrams.toInputString(),
            ingredients = ingredients.orEmpty(),
            source = source,
            useCount = useCount,
        )
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(FoodLibraryViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return FoodLibraryViewModel(application) as T
                }
            }
        }
    }
}

data class FoodLibraryUiState(
    val searchQuery: String = "",
    val items: List<FoodLibraryItemUi> = emptyList(),
)

data class FoodLibraryItemUi(
    val id: Long,
    val name: String,
    val calories: String,
    val proteins: String,
    val fats: String,
    val carbs: String,
    val weight: String,
    val ingredients: String,
    val source: String,
    val useCount: Int,
)

data class FoodItemEditInput(
    val id: Long,
    val name: String,
    val calories: String,
    val proteins: String,
    val fats: String,
    val carbs: String,
    val weight: String,
    val ingredients: String,
)

sealed interface FoodLibraryEvent {
    data class Message(val text: String) : FoodLibraryEvent
}

fun FoodLibraryItemUi.toEditInput(): FoodItemEditInput {
    return FoodItemEditInput(
        id = id,
        name = name,
        calories = calories,
        proteins = proteins,
        fats = fats,
        carbs = carbs,
        weight = weight,
        ingredients = ingredients,
    )
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

private fun Double?.toInputString(): String {
    val value = this ?: return ""
    val longValue = value.toLong()
    return if (value == longValue.toDouble()) longValue.toString() else value.toString()
}
