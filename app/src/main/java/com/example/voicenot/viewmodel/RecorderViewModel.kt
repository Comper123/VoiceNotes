package com.example.voicenot.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenot.audio.AudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class RecorderState(
    val isRecording: Boolean = false,
    val duration: Long = 0L,
    val error: String? = null
)

class RecorderViewModel : ViewModel() {
    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private var audioRecorder: AudioRecorder? = null
    private var recordingJob: kotlinx.coroutines.Job? = null

    fun initRecorder(context: Context) {
        audioRecorder = AudioRecorder(context)
        Log.d("RecorderVM", "AudioRecorder инициализирован")
    }

    fun startRecording() {
        try {
            if (audioRecorder == null) {
                _state.update { it.copy(error = "Рекордер не инициализирован") }
                return
            }

            val success = audioRecorder!!.startRecording()
            if (success) {
                _state.update {
                    it.copy(
                        isRecording = true,
                        duration = 0L,
                        error = null
                    )
                }
                startProgressUpdates()
            } else {
                _state.update { it.copy(error = "Не удалось начать запись") }
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = "Ошибка: ${e.message}") }
        }
    }

    private fun startProgressUpdates() {
        recordingJob = viewModelScope.launch {
            while (_state.value.isRecording) {
                _state.update { state ->
                    state.copy(
                        duration = audioRecorder?.getCurrentDuration() ?: 0
                    )
                }
                delay(50)
            }
        }
    }

    fun stopRecording(onSaved: () -> Unit) {
        recordingJob?.cancel()
        val filePath = audioRecorder?.stopRecording()
        _state.update { it.copy(isRecording = false) }

        if (!filePath.isNullOrEmpty()) {
            val file = File(filePath)
            Log.d("RecorderVM", "Файл сохранён: ${file.absolutePath}, размер: ${file.length()}")
            onSaved()
        } else {
            _state.update { it.copy(error = "Не удалось сохранить запись") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        audioRecorder?.stopRecording()
    }
}