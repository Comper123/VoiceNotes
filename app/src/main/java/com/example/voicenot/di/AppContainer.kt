package com.example.voicenot.di

import android.content.Context
import com.example.voicenot.audio.AudioPlayer
import com.example.voicenot.audio.AudioRecorder
import com.example.voicenot.model.database.VoiceNoteDatabase
import com.example.voicenot.model.repository.VoiceNoteRepository
import com.example.voicenot.model.repository.VoiceNoteRepositoryImpl

class AppContainer(private val context: Context) {

    private val database by lazy {
        VoiceNoteDatabase.getInstance(context)
    }

    private val voiceNoteDao by lazy {
        database.voiceNoteDao()
    }

    private val folderDao by lazy {
        database.folderDao()
    }

    val repository: VoiceNoteRepository by lazy {
        VoiceNoteRepositoryImpl(voiceNoteDao, folderDao)
    }

    val audioRecorder: AudioRecorder by lazy {
        AudioRecorder(context)
    }

    val audioPlayer: AudioPlayer by lazy {
        AudioPlayer()
    }
}