// viewmodel/MainViewModel.kt
package com.example.voicenot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenot.model.VoiceNote
import com.example.voicenot.model.Folder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Stats(
    val totalNotes: Int = 0,
    val totalFolders: Int = 0,
    val favoriteNotes: Int = 0
)

class MainViewModel : ViewModel() {
    private val _notes = MutableStateFlow<List<VoiceNote>>(emptyList())
    val notes: StateFlow<List<VoiceNote>> = _notes.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _searchResults = MutableStateFlow<List<VoiceNote>>(emptyList())
    val searchResults: StateFlow<List<VoiceNote>> = _searchResults.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    val selectedTab = MutableStateFlow(0)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Загрузка из базы данных
            _stats.value = Stats(10, 3, 2)
        }
    }

    fun createNote(title: String, folderId: Long) {
        viewModelScope.launch {
            val newNote = VoiceNote(
                title = title,
                content = "",
                filePath = "",
                duration = 0,
                folderId = folderId
            )
            // Сохранение в БД
        }
    }

    fun createFolder(name: String, color: String) {
        viewModelScope.launch {
            val newFolder = Folder(
                name = name,
                color = color
            )
            // Сохранение в БД
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            // Удаление из БД
        }
    }

    fun searchNotes(query: String) {
        viewModelScope.launch {
            // Поиск в БД
        }
    }

    fun toggleFavorite(noteId: Long) {
        viewModelScope.launch {
            // Обновление в БД
        }
    }
}