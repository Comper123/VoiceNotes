package com.example.voicenot.model.database

import androidx.room.*
import com.example.voicenot.model.entities.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {

    @Query("SELECT * FROM voice_notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): VoiceNoteEntity?

    @Query("SELECT * FROM voice_notes WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE title LIKE '%' || :query || '%' OR transcription LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<VoiceNoteEntity>>

    @Insert
    suspend fun insertNote(note: VoiceNoteEntity): Long

    @Update
    suspend fun updateNote(note: VoiceNoteEntity)

    @Delete
    suspend fun deleteNote(note: VoiceNoteEntity)

    @Query("UPDATE voice_notes SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}