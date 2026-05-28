package com.example.simplediary.ui.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplediary.app.SimpleDiaryApplication
import com.example.simplediary.data.local.entity.WorkoutCategoryEntity
import com.example.simplediary.data.local.entity.WorkoutEntity
import com.example.simplediary.data.local.entity.WorkoutTypeEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutViewModel(
    application: Application,
    private val workoutId: Long?,
) : AndroidViewModel(application) {

    private val app = application as SimpleDiaryApplication
    private val workoutDao = app.appDatabase.workoutDao()
    private val categoryDao = app.appDatabase.workoutCategoryDao()
    private val typeDao = app.appDatabase.workoutTypeDao()

    private val _uiState = MutableStateFlow(
        WorkoutUiState(
            isEditMode = workoutId != null,
            timestampEpochMillis = System.currentTimeMillis(),
        )
    )
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WorkoutEvent>()
    val events: SharedFlow<WorkoutEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            loadCategoriesAndMaybeWorkout()
        }
    }

    fun onTimestampChanged(timestampEpochMillis: Long) {
        _uiState.update { it.copy(timestampEpochMillis = timestampEpochMillis) }
    }

    fun onCategorySelected(categoryId: Long) {
        viewModelScope.launch {
            val types = typeDao.getTypesByCategory(categoryId)
            _uiState.update { state ->
                val keepTypeId = state.selectedTypeId?.takeIf { selected ->
                    types.any { it.id == selected }
                }
                state.copy(
                    selectedCategoryId = categoryId,
                    selectedTypeId = keepTypeId,
                    availableTypes = types,
                )
            }
        }
    }

    fun onTypeSelected(typeId: Long?) {
        _uiState.update { it.copy(selectedTypeId = typeId) }
    }

    fun onDurationChanged(duration: String) {
        _uiState.update { it.copy(durationMinutes = duration) }
    }

    fun onCaloriesChanged(calories: String) {
        _uiState.update { it.copy(caloriesBurned = calories) }
    }

    fun onDistanceChanged(distanceKm: String) {
        _uiState.update { it.copy(distanceKm = distanceKm) }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val categoryId = state.selectedCategoryId
            if (categoryId == null) {
                _events.emit(WorkoutEvent.Error("Select category"))
                return@launch
            }
            val duration = state.durationMinutes.toIntOrNull()
            if (duration == null || duration <= 0) {
                _events.emit(WorkoutEvent.Error("Duration must be > 0"))
                return@launch
            }
            val calories = state.caloriesBurned.toIntOrNull() ?: 0
            val distanceKm = state.distanceKm.toDoubleOrNull()
            if (state.isCardioCategory && state.distanceKm.isNotBlank() && distanceKm == null) {
                _events.emit(WorkoutEvent.Error("Distance must be a number"))
                return@launch
            }

            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                if (workoutId == null) {
                    workoutDao.insertWorkout(
                        WorkoutEntity(
                            dateEpochMillisUtcStart = state.timestampEpochMillis,
                            categoryId = categoryId,
                            typeId = state.selectedTypeId,
                            durationMinutes = duration,
                            caloriesBurned = calories,
                            distanceKm = if (state.isCardioCategory) distanceKm else null,
                            note = state.note.trim(),
                        )
                    )
                } else {
                    workoutDao.updateWorkout(
                        WorkoutEntity(
                            id = workoutId,
                            dateEpochMillisUtcStart = state.timestampEpochMillis,
                            categoryId = categoryId,
                            typeId = state.selectedTypeId,
                            durationMinutes = duration,
                            caloriesBurned = calories,
                            distanceKm = if (state.isCardioCategory) distanceKm else null,
                            note = state.note.trim(),
                        )
                    )
                }
            }.onSuccess {
                _events.emit(WorkoutEvent.Saved)
            }.onFailure { throwable ->
                _events.emit(WorkoutEvent.Error(throwable.message ?: "Failed to save workout"))
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun onDeleteClick() {
        val id = workoutId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                workoutDao.deleteWorkoutById(id)
            }.onSuccess {
                _events.emit(WorkoutEvent.Deleted)
            }.onFailure { throwable ->
                _events.emit(WorkoutEvent.Error(throwable.message ?: "Failed to delete workout"))
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun loadCategoriesAndMaybeWorkout() {
        runCatching {
            val categories = categoryDao.getAllCategories()
            val selectedCategory = categories.firstOrNull()?.id
            _uiState.update {
                it.copy(
                    categories = categories,
                    selectedCategoryId = selectedCategory,
                )
            }
            selectedCategory?.let { categoryId ->
                val types = typeDao.getTypesByCategory(categoryId)
                _uiState.update { state -> state.copy(availableTypes = types) }
            }

            if (workoutId != null) {
                loadWorkout(workoutId)
            }
        }.onFailure { throwable ->
            _events.emit(WorkoutEvent.Error(throwable.message ?: "Failed to load workout metadata"))
        }
    }

    private suspend fun loadWorkout(workoutId: Long) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return
        val types = typeDao.getTypesByCategory(workout.categoryId)
        _uiState.update {
            it.copy(
                timestampEpochMillis = workout.dateEpochMillisUtcStart,
                selectedCategoryId = workout.categoryId,
                selectedTypeId = workout.typeId,
                availableTypes = types,
                durationMinutes = workout.durationMinutes.toString(),
                caloriesBurned = workout.caloriesBurned.toString(),
                distanceKm = workout.distanceKm?.toString().orEmpty(),
                note = workout.note,
            )
        }
    }

    companion object {
        fun factory(application: Application, workoutId: Long?): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return WorkoutViewModel(application, workoutId) as T
                }
            }
        }
    }
}

data class WorkoutUiState(
    val isEditMode: Boolean,
    val isBusy: Boolean = false,
    val timestampEpochMillis: Long,
    val categories: List<WorkoutCategoryEntity> = emptyList(),
    val availableTypes: List<WorkoutTypeEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedTypeId: Long? = null,
    val durationMinutes: String = "",
    val caloriesBurned: String = "",
    val distanceKm: String = "",
    val note: String = "",
) {
    val isCardioCategory: Boolean
        get() = categories.firstOrNull { it.id == selectedCategoryId }?.name == "Кардио"
}

sealed interface WorkoutEvent {
    data object Saved : WorkoutEvent
    data object Deleted : WorkoutEvent
    data class Error(val message: String) : WorkoutEvent
}
