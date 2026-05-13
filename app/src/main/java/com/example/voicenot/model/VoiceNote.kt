package com.example.voicenot.model

import java.util.Date

data class VoiceNote(
    val id: Long = 0,
    val title: String,
    val content: String = "",           // транскрипция/текст
    val filePath: String,
    val duration: Long,
    val fileSize: Long = 0L,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val folderId: Long = 0,             // ID папки (0 = "Все заметки")
    val tags: List<String> = emptyList(), // теги
    val isFavorite: Boolean = false
)

data class Folder(
    val id: Long = 0,
    val name: String,
    val color: String = "#6200EE",
    val createdAt: Date = Date()
)