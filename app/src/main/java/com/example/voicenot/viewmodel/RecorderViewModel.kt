package com.example.voicenot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenot.audio.AudioRecorder
import com.example.voicenot.model.VoiceNote
import com.example.voicenot.model.repository.VoiceNoteRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class RecorderUiState(
    val isRecording: Boolean = false,
    val duration: Long = 0L,
    val amplitude: Int = 0,
    val error: String? = null
)

sealed class RecorderEvent {
    object StartRecording : RecorderEvent()
    object StopRecording : RecorderEvent()
}

class RecorderViewModel(
    private val repository: VoiceNoteRepository,
    private val audioRecorder: AudioRecorder
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    private var recordingJob: kotlinx.coroutines.Job? = null

    fun onEvent(event: RecorderEvent) {
        when (event) {
            is RecorderEvent.StartRecording -> startRecording()
            is RecorderEvent.StopRecording -> stopRecording()
        }
    }

    private fun startRecording() {
        try {
            // Проверяем, не идёт ли уже запись
            if (_uiState.value.isRecording) {
                return
            }

            val success = audioRecorder.startRecording()
            if (success) {
                _uiState.update {
                    it.copy(isRecording = true, duration = 0L, error = null)
                }

                recordingJob = viewModelScope.launch {
                    while (isActive && _uiState.value.isRecording) {
                        _uiState.update { state ->
                            state.copy(
                                duration = audioRecorder.getCurrentDuration(),
                                amplitude = audioRecorder.getMaxAmplitude()
                            )
                        }
                        delay(50)
                    }
                }
            } else {
                _uiState.update { it.copy(error = "Не удалось начать запись") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(error = "Ошибка: ${e.message}") }
        }
    }

    private fun stopRecording() {
        try {
            recordingJob?.cancel()
            recordingJob = null

            val filePath = audioRecorder.stopRecording()
            _uiState.update { it.copy(isRecording = false) }

            if (!filePath.isNullOrEmpty()) {
                viewModelScope.launch {
                    try {
                        val file = File(filePath)
                        if (file.exists() && file.length() > 0) {
                            val note = VoiceNote(
                                title = "Заметка ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                                filePath = filePath,
                                duration = _uiState.value.duration,
                                fileSize = file.length()
                            )
                            repository.addNote(note)
                        } else {
                            _uiState.update { it.copy(error = "Файл записи пуст") }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _uiState.update { it.copy(error = "Ошибка сохранения: ${e.message}") }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(error = "Ошибка остановки записи: ${e.message}") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        if (_uiState.value.isRecording) {
            try {
                audioRecorder.stopRecording()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}