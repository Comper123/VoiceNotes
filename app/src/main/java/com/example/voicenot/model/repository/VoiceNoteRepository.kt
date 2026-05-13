package com.example.voicenot.model.repository

import com.example.voicenot.model.VoiceNote
import com.example.voicenot.model.Folder
import kotlinx.coroutines.flow.Flow

interface VoiceNoteRepository {
    // Voice Notes
    fun getAllNotes(): Flow<List<VoiceNote>>
    fun getNotesByFolder(folderId: Long): Flow<List<VoiceNote>>
    fun getFavoriteNotes(): Flow<List<VoiceNote>>
    fun searchNotes(query: String): Flow<List<VoiceNote>>
    fun getNotesByTag(tag: String): Flow<List<VoiceNote>>
    suspend fun getNoteById(id: Long): VoiceNote?
    suspend fun addNote(note: VoiceNote): Long
    suspend fun updateNote(note: VoiceNote)
    suspend fun deleteNote(note: VoiceNote)
    suspend fun toggleFavorite(id: Long)
    suspend fun updateNoteContent(id: Long, content: String)
    suspend fun updateNoteTags(id: Long, tags: List<String>)

    // Folders
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun getFolderById(id: Long): Folder?
    suspend fun addFolder(folder: Folder): Long
    suspend fun updateFolder(folder: Folder)
    suspend fun deleteFolder(id: Long)
}