package com.example.simplediary.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplediary.app.SimpleDiaryApplication
import com.example.simplediary.core.settings.DEFAULT_NOTE_CATEGORIES
import com.example.simplediary.core.settings.loadNoteCategories
import com.example.simplediary.core.settings.saveNoteCategories
import com.example.simplediary.data.local.entity.DailyTargetEntity
import com.example.simplediary.data.local.entity.WorkoutCategoryEntity
import com.example.simplediary.data.local.entity.WorkoutTypeEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as SimpleDiaryApplication
    private val dailyTargetDao = app.appDatabase.dailyTargetDao()
    private val workoutCategoryDao = app.appDatabase.workoutCategoryDao()
    private val workoutTypeDao = app.appDatabase.workoutTypeDao()
    private val preferences = application.getSharedPreferences(NOTE_CATEGORIES_PREFS, Application.MODE_PRIVATE)

    private val zoneId = ZoneId.systemDefault()
    private val todayStartEpochMillis = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()

    private val targetInputs = MutableStateFlow(TargetInputs())
    private val noteCategoriesState = MutableStateFlow(preferences.loadNoteCategories())

    val uiState: StateFlow<SettingsUiState> = combine(
        workoutCategoryDao.observeAllCategories(),
        workoutTypeDao.observeAllTypes(),
        dailyTargetDao.observeTargetEffectiveForDay(todayStartEpochMillis),
        targetInputs,
        noteCategoriesState,
    ) { categories, types, dailyTarget, inputs, noteCategories ->
        val hydratedInputs = hydrateInputsFromTarget(inputs, dailyTarget)
        val categoriesUi = categories.map { category ->
            SettingsCategoryUi(
                id = category.id,
                name = category.name,
                sortOrder = category.sortOrder,
                types = types
                    .filter { it.categoryId == category.id }
                    .sortedBy { it.sortOrder }
                    .map { type ->
                        SettingsTypeUi(
                            id = type.id,
                            categoryId = type.categoryId,
                            name = type.name,
                            sortOrder = type.sortOrder,
                        )
                    },
            )
        }
        SettingsUiState(
            targetCalories = hydratedInputs.calories,
            targetProteins = hydratedInputs.proteins,
            targetFats = hydratedInputs.fats,
            targetCarbs = hydratedInputs.carbs,
            categories = categoriesUi,
            noteCategories = noteCategories,
            isBusy = hydratedInputs.isBusy,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun onTargetCaloriesChanged(value: String) = targetInputs.update { it.copy(calories = value) }
    fun onTargetProteinsChanged(value: String) = targetInputs.update { it.copy(proteins = value) }
    fun onTargetFatsChanged(value: String) = targetInputs.update { it.copy(fats = value) }
    fun onTargetCarbsChanged(value: String) = targetInputs.update { it.copy(carbs = value) }

    fun onSaveTargetsClick() {
        viewModelScope.launch {
            targetInputs.update { it.copy(isBusy = true) }
            runCatching {
                val current = targetInputs.value
                dailyTargetDao.insertDailyTarget(
                    DailyTargetEntity(
                        effectiveFrom = todayStartEpochMillis,
                        targetCaloriesKcal = current.calories.toDoubleOrNull() ?: 0.0,
                        targetProteinsGrams = current.proteins.toDoubleOrNull() ?: 0.0,
                        targetFatsGrams = current.fats.toDoubleOrNull() ?: 0.0,
                        targetCarbsGrams = current.carbs.toDoubleOrNull() ?: 0.0,
                    )
                )
            }.onSuccess {
                _events.emit(SettingsEvent.Message("Daily targets saved"))
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Failed to save targets"))
            }
            targetInputs.update { it.copy(isBusy = false) }
        }
    }

    fun addCategory(name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val nextOrder = (workoutCategoryDao.getAllCategories().maxOfOrNull { it.sortOrder } ?: 0) + 1
                workoutCategoryDao.insertCategory(
                    WorkoutCategoryEntity(
                        name = normalized,
                        sortOrder = nextOrder,
                    )
                )
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Failed to add category"))
            }
        }
    }

    fun renameCategory(categoryId: Long, newName: String) {
        val normalized = newName.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val category = workoutCategoryDao.getCategoryById(categoryId) ?: return@runCatching
                workoutCategoryDao.updateCategory(category.copy(name = normalized))
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Failed to rename category"))
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            runCatching {
                workoutCategoryDao.deleteCategoryById(categoryId)
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Failed to delete category"))
            }
        }
    }

    fun addType(categoryId: Long, name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val nextOrder = (workoutTypeDao.getTypesByCategory(categoryId).maxOfOrNull { it.sortOrder } ?: 0) + 1
                workoutTypeDao.insertType(
                    WorkoutTypeEntity(
                        categoryId = categoryId,
                        name = normalized,
                        sortOrder = nextOrder,
                    )
                )
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Failed to add type"))
            }
        }
    }

    fun renameType(typeId: Long, newName: String) {
        val normalized = newName.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val type = workoutTypeDao.getTypeById(typeId) ?: return@runCatching
                workoutTypeDao.updateType(type.copy(name = normalized))
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Failed to rename type"))
            }
        }
    }

    fun deleteType(typeId: Long) {
        viewModelScope.launch {
            runCatching {
                workoutTypeDao.deleteTypeById(typeId)
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Failed to delete type"))
            }
        }
    }

    fun addNoteCategory(name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        updateNoteCategories(noteCategoriesState.value + normalized)
    }

    fun renameNoteCategory(oldName: String, newName: String) {
        val oldNormalized = oldName.trim()
        val newNormalized = newName.trim()
        if (oldNormalized.isBlank() || newNormalized.isBlank()) return
        val replaced = noteCategoriesState.value.map { current ->
            if (current == oldNormalized) newNormalized else current
        }
        updateNoteCategories(replaced)
    }

    fun deleteNoteCategory(name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        val filtered = noteCategoriesState.value.filterNot { it == normalized }
        updateNoteCategories(filtered)
    }

    fun exportCsv(destinationUri: Uri) {
        viewModelScope.launch {
            runCatching { app.csvExporter.exportAllData(destinationUri) }
                .onSuccess { _events.emit(SettingsEvent.Message("CSV export completed")) }
                .onFailure { throwable ->
                    _events.emit(SettingsEvent.Message(throwable.message ?: "CSV export failed"))
                }
        }
    }

    fun backupZip(destinationUri: Uri) {
        viewModelScope.launch {
            runCatching { app.backupManager.createZipBackup(destinationUri) }
                .onSuccess { _events.emit(SettingsEvent.Message("Backup ZIP created")) }
                .onFailure { throwable ->
                    _events.emit(SettingsEvent.Message(throwable.message ?: "Backup failed"))
                }
        }
    }

    fun restoreZip(sourceUri: Uri) {
        viewModelScope.launch {
            runCatching { app.backupManager.restoreFromZip(sourceUri) }
                .onSuccess { _events.emit(SettingsEvent.Message("Restore completed")) }
                .onFailure { throwable ->
                    _events.emit(SettingsEvent.Message(throwable.message ?: "Restore failed"))
                }
        }
    }

    private fun hydrateInputsFromTarget(inputs: TargetInputs, target: DailyTargetEntity?): TargetInputs {
        if (inputs.edited) return inputs
        return inputs.copy(
            calories = target?.targetCaloriesKcal?.format0or1().orEmpty(),
            proteins = target?.targetProteinsGrams?.format0or1().orEmpty(),
            fats = target?.targetFatsGrams?.format0or1().orEmpty(),
            carbs = target?.targetCarbsGrams?.format0or1().orEmpty(),
        )
    }

    private fun MutableStateFlow<TargetInputs>.update(transform: (TargetInputs) -> TargetInputs) {
        value = transform(value).copy(edited = true)
    }

    private fun updateNoteCategories(categories: List<String>) {
        val normalized = categories.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            .ifEmpty { DEFAULT_NOTE_CATEGORIES }
        noteCategoriesState.value = normalized
        preferences.saveNoteCategories(normalized)
    }

    companion object {
        private const val NOTE_CATEGORIES_PREFS = "note_categories_preferences"

        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return SettingsViewModel(application) as T
                }
            }
        }
    }
}

data class SettingsUiState(
    val targetCalories: String = "",
    val targetProteins: String = "",
    val targetFats: String = "",
    val targetCarbs: String = "",
    val categories: List<SettingsCategoryUi> = emptyList(),
    val noteCategories: List<String> = DEFAULT_NOTE_CATEGORIES,
    val isBusy: Boolean = false,
)

data class SettingsCategoryUi(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val types: List<SettingsTypeUi>,
)

data class SettingsTypeUi(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val sortOrder: Int,
)

sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent
}

private data class TargetInputs(
    val calories: String = "",
    val proteins: String = "",
    val fats: String = "",
    val carbs: String = "",
    val edited: Boolean = false,
    val isBusy: Boolean = false,
)

private fun Double.format0or1(): String {
    val asInt = toInt()
    return if (this == asInt.toDouble()) asInt.toString() else String.format(java.util.Locale.US, "%.1f", this)
}
