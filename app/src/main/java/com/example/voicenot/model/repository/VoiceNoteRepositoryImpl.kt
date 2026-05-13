package com.example.voicenot.model.repository

import com.example.voicenot.model.VoiceNote
import com.example.voicenot.model.Folder
import com.example.voicenot.model.database.FolderDao
import com.example.voicenot.model.database.VoiceNoteDao
import com.example.voicenot.model.entities.VoiceNoteEntity
import com.example.voicenot.model.entities.FolderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class VoiceNoteRepositoryImpl(
    private val voiceNoteDao: VoiceNoteDao,
    private val folderDao: FolderDao
) : VoiceNoteRepository {

    override fun getAllNotes(): Flow<List<VoiceNote>> {
        return voiceNoteDao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNotesByFolder(folderId: Long): Flow<List<VoiceNote>> {
        return voiceNoteDao.getNotesByFolder(folderId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteNotes(): Flow<List<VoiceNote>> {
        return voiceNoteDao.getFavoriteNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchNotes(query: String): Flow<List<VoiceNote>> {
        return voiceNoteDao.searchNotes(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNotesByTag(tag: String): Flow<List<VoiceNote>> {
        return voiceNoteDao.getNotesByTag(tag).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: Long): VoiceNote? {
        return voiceNoteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun addNote(note: VoiceNote): Long {
        return voiceNoteDao.insertNote(note.toEntity())
    }

    override suspend fun updateNote(note: VoiceNote) {
        voiceNoteDao.updateNote(note.toEntity())
    }

    override suspend fun deleteNote(note: VoiceNote) {
        voiceNoteDao.deleteNoteById(note.id)
    }

    override suspend fun toggleFavorite(id: Long) {
        val note = voiceNoteDao.getNoteById(id)
        note?.let {
            voiceNoteDao.updateFavoriteStatus(id, !it.isFavorite)
        }
    }

    override suspend fun updateNoteContent(id: Long, content: String) {
        val note = voiceNoteDao.getNoteById(id)
        note?.let {
            voiceNoteDao.updateNote(it.copy(content = content))
        }
    }

    override suspend fun updateNoteTags(id: Long, tags: List<String>) {
        val note = voiceNoteDao.getNoteById(id)
        note?.let {
            voiceNoteDao.updateNote(it.copy(tags = tags.joinToString(",")))
        }
    }

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFolderById(id: Long): Folder? {
        return folderDao.getFolderById(id)?.toDomain()
    }

    override suspend fun addFolder(folder: Folder): Long {
        return folderDao.insertFolder(folder.toEntity())
    }

    override suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder.toEntity())
    }

    override suspend fun deleteFolder(id: Long) {
        folderDao.deleteFolderById(id)
        voiceNoteDao.moveNotesToFolder(id, 0L)
    }
}

// ========== Mappers ==========

private fun VoiceNoteEntity.toDomain(): VoiceNote {
    return VoiceNote(
        id = id,
        title = title,
        content = content,
        filePath = filePath,
        duration = duration,
        fileSize = fileSize,
        createdAt = createdAt,
        updatedAt = updatedAt,
        folderId = folderId,
        tags = if (tags.isNotEmpty()) tags.split(",").filter { it.isNotEmpty() } else emptyList(),
        isFavorite = isFavorite
    )
}

private fun VoiceNote.toEntity(): VoiceNoteEntity {
    return VoiceNoteEntity(
        id = id,
        title = title,
        content = content,
        filePath = filePath,
        duration = duration,
        fileSize = fileSize,
        createdAt = createdAt,
        updatedAt = updatedAt,
        folderId = folderId,
        tags = tags.joinToString(","),
        isFavorite = isFavorite
    )
}

private fun FolderEntity.toDomain(): Folder {
    return Folder(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt
    )
}

private fun Folder.toEntity(): FolderEntity {
    return FolderEntity(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt
    )
}