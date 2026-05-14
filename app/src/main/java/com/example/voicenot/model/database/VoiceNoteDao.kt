package com.example.voicenot.model.database

import androidx.room.*
import com.example.voicenot.model.entities.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE tags LIKE '%' || :tag || '%'")
    fun getNotesByTag(tag: String): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): VoiceNoteEntity?

    @Insert
    suspend fun insertNote(note: VoiceNoteEntity): Long

    @Update
    suspend fun updateNote(note: VoiceNoteEntity)

    @Delete
    suspend fun deleteNote(note: VoiceNoteEntity)

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE voice_notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE voice_notes SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    suspend fun moveNotesToFolder(oldFolderId: Long, newFolderId: Long)

    @Query("UPDATE voice_notes SET content = :content WHERE id = :id")
    suspend fun updateNoteContent(id: Long, content: String)

    @Query("UPDATE voice_notes SET tags = :tags WHERE id = :id")
    suspend fun updateNoteTags(id: Long, tags: String)
}