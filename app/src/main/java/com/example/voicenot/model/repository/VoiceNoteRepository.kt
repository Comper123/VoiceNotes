package com.example.voicenot.model.repository

import com.example.voicenot.model.VoiceNote
import kotlinx.coroutines.flow.Flow

interface VoiceNoteRepository {
    fun getAllNotes(): Flow<List<VoiceNote>>
    fun getFavoriteNotes(): Flow<List<VoiceNote>>
    fun searchNotes(query: String): Flow<List<VoiceNote>>
    suspend fun getNoteById(id: Long): VoiceNote?
    suspend fun addNote(note: VoiceNote): Long
    suspend fun updateNote(note: VoiceNote)
    suspend fun deleteNote(note: VoiceNote)
    suspend fun toggleFavorite(id: Long)
}