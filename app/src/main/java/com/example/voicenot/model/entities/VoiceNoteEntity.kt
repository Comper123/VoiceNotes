// model/entities/VoiceNoteEntity.kt
package com.example.voicenot.model.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.voicenot.model.database.Converters
import java.util.Date

@Entity(tableName = "voice_notes")
@TypeConverters(Converters::class)
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val filePath: String,
    val duration: Long,
    val fileSize: Long,
    val createdAt: Date,
    val updatedAt: Date,
    val folderId: Long,
    val tags: String, // JSON строка
    val isFavorite: Boolean
)