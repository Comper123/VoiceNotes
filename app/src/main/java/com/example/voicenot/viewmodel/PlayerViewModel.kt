package com.example.voicenot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenot.audio.AudioPlayer
import com.example.voicenot.model.database.VoiceNoteDao
import com.example.voicenot.model.entities.VoiceNoteEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val note: VoiceNoteEntity? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class PlayerEvent {
    object Play : PlayerEvent()
    object Pause : PlayerEvent()
    object Resume : PlayerEvent()
    object Stop : PlayerEvent()
    object ToggleFavorite : PlayerEvent()
}

class PlayerViewModel(
    private val voiceNoteDao: VoiceNoteDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var audioPlayer: AudioPlayer? = null
    private var progressJob: kotlinx.coroutines.Job? = null

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val note = voiceNoteDao.getNoteById(noteId)
                _uiState.update { it.copy(note = note, isLoading = false) }

                note?.let {
                    audioPlayer = AudioPlayer().apply {
                        setOnCompletionListener {
                            _uiState.update { state ->
                                state.copy(isPlaying = false, currentPosition = 0)
                            }
                            progressJob?.cancel()
                        }
                        setOnErrorListener {
                            _uiState.update { it.copy(error = "Ошибка воспроизведения") }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.Play -> play()
            is PlayerEvent.Pause -> pause()
            is PlayerEvent.Resume -> resume()
            is PlayerEvent.Stop -> stop()
            is PlayerEvent.ToggleFavorite -> toggleFavorite()
        }
    }

    private fun play() {
        val note = _uiState.value.note ?: return

        audioPlayer?.play(note.filePath)
        _uiState.update { it.copy(isPlaying = true) }

        startProgressUpdates()
    }

    private fun pause() {
        audioPlayer?.pause()
        _uiState.update { it.copy(isPlaying = false) }
        progressJob?.cancel()
    }

    private fun resume() {
        audioPlayer?.resume()
        _uiState.update { it.copy(isPlaying = true) }
        startProgressUpdates()
    }

    private fun stop() {
        audioPlayer?.stop()
        _uiState.update { it.copy(isPlaying = false, currentPosition = 0) }
        progressJob?.cancel()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _uiState.value.isPlaying) {
                audioPlayer?.getCurrentPosition()?.let { position ->
                    _uiState.update { state ->
                        state.copy(currentPosition = position)
                    }
                }
                delay(100)
            }
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            _uiState.value.note?.let { note ->
                try {
                    voiceNoteDao.updateFavoriteStatus(note.id, !note.isFavorite)
                    val updatedNote = voiceNoteDao.getNoteById(note.id)
                    _uiState.update { it.copy(note = updatedNote) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Ошибка: ${e.message}") }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer?.release()
        progressJob?.cancel()
    }
}