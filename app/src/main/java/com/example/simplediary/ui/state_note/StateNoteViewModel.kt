package com.example.simplediary.ui.state_note

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplediary.app.SimpleDiaryApplication
import com.example.simplediary.core.settings.DEFAULT_NOTE_CATEGORIES
import com.example.simplediary.core.settings.loadNoteCategories
import com.example.simplediary.data.local.entity.StateNoteEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.util.UUID

class StateNoteViewModel(
    application: Application,
    private val stateNoteId: Long?,
) : AndroidViewModel(application) {
    private val app = application as SimpleDiaryApplication
    private val stateNoteDao = app.appDatabase.stateNoteDao()
    private val photoCompressor = app.photoCompressor
    private val preferences = application.getSharedPreferences(NOTE_CATEGORIES_PREFS, Application.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        StateNoteUiState(
            isEditMode = stateNoteId != null,
            timestampEpochMillis = System.currentTimeMillis(),
        )
    )
    val uiState: StateFlow<StateNoteUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StateNoteEvent>()
    val events: SharedFlow<StateNoteEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            loadCategorySuggestions()
            if (stateNoteId != null) {
                loadStateNote(stateNoteId)
            }
        }
    }

    fun onTimestampChanged(timestampEpochMillis: Long) {
        _uiState.update { it.copy(timestampEpochMillis = timestampEpochMillis) }
    }

    fun onTextChanged(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun onCategoryChanged(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun onPhotoRemoved() {
        _uiState.update { it.copy(photoPath = null) }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val compressedPath = compressPickedPhoto(uri)
                _uiState.update { it.copy(photoPath = compressedPath) }
            }.onFailure { throwable ->
                _events.emit(StateNoteEvent.Error(throwable.message ?: "Unable to process selected image"))
            }
        }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val text = state.text.trim()
            if (text.isBlank()) {
                _events.emit(StateNoteEvent.Error("Text note is required"))
                return@launch
            }
            val category = state.category.trim().ifBlank { DEFAULT_NOTE_CATEGORY }

            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                if (stateNoteId == null) {
                    stateNoteDao.insertStateNote(
                        StateNoteEntity(
                            category = category,
                            text = text,
                            photoPath = state.photoPath,
                            timestampEpochMillis = state.timestampEpochMillis,
                        )
                    )
                } else {
                    stateNoteDao.updateStateNote(
                        StateNoteEntity(
                            id = stateNoteId,
                            category = category,
                            text = text,
                            photoPath = state.photoPath,
                            timestampEpochMillis = state.timestampEpochMillis,
                        )
                    )
                }
            }.onSuccess {
                _events.emit(StateNoteEvent.Saved)
            }.onFailure { throwable ->
                _events.emit(StateNoteEvent.Error(throwable.message ?: "Failed to save state note"))
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun onDeleteClick() {
        val id = stateNoteId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching { stateNoteDao.deleteStateNoteById(id) }
                .onSuccess { _events.emit(StateNoteEvent.Deleted) }
                .onFailure { throwable ->
                    _events.emit(StateNoteEvent.Error(throwable.message ?: "Failed to delete state note"))
                }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun loadStateNote(stateNoteId: Long) {
        runCatching {
            val note = stateNoteDao.getStateNoteById(stateNoteId) ?: return
            _uiState.update {
                val available = if (it.availableCategories.contains(note.category)) {
                    it.availableCategories
                } else {
                    it.availableCategories + note.category
                }
                it.copy(
                    timestampEpochMillis = note.timestampEpochMillis,
                    category = note.category,
                    availableCategories = available,
                    text = note.text,
                    photoPath = note.photoPath,
                )
            }
        }.onFailure { throwable ->
            _events.emit(StateNoteEvent.Error(throwable.message ?: "Failed to load state note"))
        }
    }

    private suspend fun loadCategorySuggestions() {
        val merged = preferences.loadNoteCategories()
        _uiState.update { state ->
            state.copy(
                availableCategories = merged,
                category = state.category.ifBlank { DEFAULT_NOTE_CATEGORY },
            )
        }
    }

    private suspend fun compressPickedPhoto(sourceUri: Uri): String {
        val cacheDir = app.cacheDir
        val inputFile = File(cacheDir, "picked_${UUID.randomUUID()}.img")
        val outputFile = File(app.filesDir, "photos/state_note_${System.currentTimeMillis()}.jpg")
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
            ?: error("Unable to read selected image: $sourceUri")

        inputStream.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    companion object {
        private const val NOTE_CATEGORIES_PREFS = "note_categories_preferences"
        private const val DEFAULT_NOTE_CATEGORY = "🙂"

        fun factory(application: Application, stateNoteId: Long?): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(StateNoteViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return StateNoteViewModel(application, stateNoteId) as T
                }
            }
        }
    }
}

data class StateNoteUiState(
    val isEditMode: Boolean,
    val isBusy: Boolean = false,
    val timestampEpochMillis: Long,
    val category: String = "🙂",
    val availableCategories: List<String> = DEFAULT_NOTE_CATEGORIES,
    val text: String = "",
    val photoPath: String? = null,
)

sealed interface StateNoteEvent {
    data object Saved : StateNoteEvent
    data object Deleted : StateNoteEvent
    data class Error(val message: String) : StateNoteEvent
}
