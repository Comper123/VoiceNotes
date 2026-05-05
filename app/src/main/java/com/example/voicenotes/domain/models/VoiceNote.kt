package com.example.voicenotes.domain.models


data class VoiceNote(
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val duration: Long,          // в миллисекундах
    val fileSize: Long,          // в байтах
    val createdAt: Long,         // timestamp
    val updatedAt: Long,         // timestamp
    val transcription: String? = null,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
)

// domain/models/AudioState.kt
data class AudioState(
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val currentPosition: Long = 0L,
    val totalDuration: Long = 0L
)

// domain/models/NoteFilters.kt
data class NoteFilters(
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val sortBy: SortBy = SortBy.DATE_DESC
)

enum class SortBy {
    DATE_DESC, DATE_ASC, DURATION, TITLE
}