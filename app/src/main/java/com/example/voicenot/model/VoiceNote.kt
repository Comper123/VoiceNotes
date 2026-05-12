package com.example.voicenot.model

data class VoiceNote(
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val duration: Long,
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val transcription: String? = null,
    val isFavorite: Boolean = false
)