package com.example.voicenot.model.repository

import com.example.voicenot.model.VoiceNote
import com.example.voicenot.model.database.VoiceNoteDao
import com.example.voicenot.model.entities.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VoiceNoteRepositoryImpl(
    private val dao: VoiceNoteDao
) : VoiceNoteRepository {

    override fun getAllNotes(): Flow<List<VoiceNote>> {
        return dao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteNotes(): Flow<List<VoiceNote>> {
        return dao.getFavoriteNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchNotes(query: String): Flow<List<VoiceNote>> {
        return dao.searchNotes(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: Long): VoiceNote? {
        return dao.getNoteById(id)?.toDomain()
    }

    override suspend fun addNote(note: VoiceNote): Long {
        return dao.insertNote(note.toEntity())
    }

    override suspend fun updateNote(note: VoiceNote) {
        dao.updateNote(note.toEntity())
    }

    override suspend fun deleteNote(note: VoiceNote) {
        dao.deleteNoteById(note.id)
    }

    override suspend fun toggleFavorite(id: Long) {
        val note = dao.getNoteById(id)
        note?.let {
            dao.updateFavoriteStatus(id, !it.isFavorite)
        }
    }
}

// Mapper functions
private fun VoiceNoteEntity.toDomain(): VoiceNote {
    return VoiceNote(
        id = id,
        title = title,
        filePath = filePath,
        duration = duration,
        fileSize = fileSize,
        createdAt = createdAt,
        updatedAt = updatedAt,
        transcription = transcription,
        isFavorite = isFavorite
    )
}

private fun VoiceNote.toEntity(): VoiceNoteEntity {
    return VoiceNoteEntity(
        id = id,
        title = title,
        filePath = filePath,
        duration = duration,
        fileSize = fileSize,
        createdAt = createdAt,
        updatedAt = updatedAt,
        transcription = transcription,
        isFavorite = isFavorite
    )
}