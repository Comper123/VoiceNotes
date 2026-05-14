// viewmodel/MainViewModel.kt
package com.example.voicenot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenot.model.database.VoiceNoteDao
import com.example.voicenot.model.database.FolderDao
import com.example.voicenot.model.entities.VoiceNoteEntity
import com.example.voicenot.model.entities.FolderEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

data class Stats(
    val totalNotes: Int = 0,
    val totalFolders: Int = 0,
    val favoriteNotes: Int = 0
)

class MainViewModel(
    private val voiceNoteDao: VoiceNoteDao,
    private val folderDao: FolderDao
) : ViewModel() {

    private val _notes = MutableStateFlow<List<VoiceNoteEntity>>(emptyList())
    val notes: StateFlow<List<VoiceNoteEntity>> = _notes.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderEntity>>(emptyList())
    val folders: StateFlow<List<FolderEntity>> = _folders.asStateFlow()

    private val _searchResults = MutableStateFlow<List<VoiceNoteEntity>>(emptyList())
    val searchResults: StateFlow<List<VoiceNoteEntity>> = _searchResults.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    val selectedTab = MutableStateFlow(0)

    init {
        loadNotes()
        loadFolders()
        loadStats()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            voiceNoteDao.getAllNotes().collect { notes ->
                _notes.value = notes
            }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            folderDao.getAllFolders().collect { folders ->
                _folders.value = folders
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            combine(_notes, _folders) { notes, folders ->
                Stats(
                    totalNotes = notes.size,
                    totalFolders = folders.size,
                    favoriteNotes = notes.count { it.isFavorite }
                )
            }.collect { stats ->
                _stats.value = stats
            }
        }
    }

    fun createNote(title: String, filePath: String, duration: Long, fileSize: Long, folderId: Long) {
        viewModelScope.launch {
            val newNote = VoiceNoteEntity(
                title = title.ifEmpty { "Заметка" },
                content = "",
                filePath = filePath,
                duration = duration,
                fileSize = fileSize,
                createdAt = Date(),
                updatedAt = Date(),
                folderId = folderId,
                tags = "",
                isFavorite = false
            )
            voiceNoteDao.insertNote(newNote)
            loadNotes()
            loadStats()
        }
    }

    fun createFolder(name: String, color: String) {
        viewModelScope.launch {
            val newFolder = FolderEntity(
                name = name,
                color = color,
                createdAt = Date()
            )
            folderDao.insertFolder(newFolder)
            loadFolders()
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            folderDao.deleteFolderById(folderId)
            loadFolders()
            loadNotes()
            loadStats()
        }
    }

    fun deleteNote(note: VoiceNoteEntity) {
        viewModelScope.launch {
            voiceNoteDao.deleteNote(note)
            loadNotes()
            loadStats()
        }
    }

    fun searchNotes(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                voiceNoteDao.searchNotes(query).collect { results ->
                    _searchResults.value = results
                }
            }
        }
    }

    fun toggleFavorite(noteId: Long) {
        viewModelScope.launch {
            val note = voiceNoteDao.getNoteById(noteId)
            note?.let {
                voiceNoteDao.updateFavoriteStatus(noteId, !it.isFavorite)
            }
            loadNotes()
            loadStats()
        }
    }

    fun updateNoteContent(noteId: Long, content: String) {
        viewModelScope.launch {
            voiceNoteDao.updateNoteContent(noteId, content)
            loadNotes()
        }
    }

    fun updateNoteTags(noteId: Long, tags: String) {
        viewModelScope.launch {
            voiceNoteDao.updateNoteTags(noteId, tags)
            loadNotes()
        }
    }
}