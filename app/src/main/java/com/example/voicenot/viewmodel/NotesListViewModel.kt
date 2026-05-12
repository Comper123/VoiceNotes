package com.example.voicenot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenot.model.VoiceNote
import com.example.voicenot.model.repository.VoiceNoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesListUiState(
    val notes: List<VoiceNote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class NotesListEvent {
    data class DeleteNote(val note: VoiceNote) : NotesListEvent()
    data class ToggleFavorite(val note: VoiceNote) : NotesListEvent()
    object LoadNotes : NotesListEvent()
    object Retry : NotesListEvent()
}

class NotesListViewModel(
    private val repository: VoiceNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesListUiState())
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllNotes().collect { notes ->
                    _uiState.update { it.copy(notes = notes, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onEvent(event: NotesListEvent) {
        when (event) {
            is NotesListEvent.DeleteNote -> deleteNote(event.note)
            is NotesListEvent.ToggleFavorite -> toggleFavorite(event.note)
            is NotesListEvent.LoadNotes -> loadNotes()
            is NotesListEvent.Retry -> loadNotes()
        }
    }

    private fun deleteNote(note: VoiceNote) {
        viewModelScope.launch {
            try {
                repository.deleteNote(note)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка удаления: ${e.message}") }
            }
        }
    }

    private fun toggleFavorite(note: VoiceNote) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(note.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка: ${e.message}") }
            }
        }
    }
}