package com.example.voicenot.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenot.audio.AudioRecorder
import com.example.voicenot.model.database.VoiceNoteDatabase
import com.example.voicenot.model.entities.VoiceNoteEntity
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.Date

data class RecorderState(
    val isRecording: Boolean = false,
    val duration: Long = 0L,
    val amplitude: Int = 0,
    val noteTitle: String = "",
    val tags: String = "",
    val transcription: String = "",
    val isTranscribing: Boolean = false,
    val error: String? = null
)

sealed class RecorderEvent {
    object StartRecording : RecorderEvent()
    object StopRecording : RecorderEvent()
    data class UpdateTitle(val title: String) : RecorderEvent()
    data class UpdateTags(val tags: String) : RecorderEvent()
    object ClearTranscription : RecorderEvent()
    object StartSpeechRecognition : RecorderEvent()
}

class RecorderViewModel : ViewModel() {
    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private var audioRecorder: AudioRecorder? = null
    private var recordingJob: kotlinx.coroutines.Job? = null
    private var currentFilePath: String? = null

    // Vosk
    private var voskModel: Model? = null
    private var recognizer: Recognizer? = null

    fun initRecorder(context: Context) {
        audioRecorder = AudioRecorder(context)
        initVosk(context)
    }

    private fun initVosk(context: Context) {
        try {
            val modelPath = copyModelFromAssets(context)
            voskModel = Model(modelPath)
        } catch (e: Exception) {
            _state.update { it.copy(error = "Ошибка инициализации Vosk: ${e.message}") }
        }
    }

    private fun copyModelFromAssets(context: Context): String {
        val modelName = "vosk-model-small-ru-0.22"
        val targetDir = File(context.filesDir, modelName)

        if (!targetDir.exists()) {
            targetDir.mkdirs()
            val assets = context.assets.list(modelName) ?: emptyArray()
            for (assetFile in assets) {
                copyAssetFile(context, "$modelName/$assetFile", File(targetDir, assetFile))
            }
        }

        return targetDir.absolutePath
    }

    private fun copyAssetFile(context: Context, assetPath: String, targetFile: File) {
        context.assets.open(assetPath).use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun startRecording() {
        try {
            if (audioRecorder == null) return

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
            while (currentCoroutineContext().isActive && _state.value.isRecording) {
                _state.update { state ->
                    state.copy(
                        duration = audioRecorder?.getCurrentDuration() ?: 0,
                        amplitude = audioRecorder?.getMaxAmplitude() ?: 0
                    )
                }
                delay(50)
            }
        }
    }

    fun stopRecording(onSaved: () -> Unit) {
        recordingJob?.cancel()
        val filePath = audioRecorder?.stopRecording()
        currentFilePath = filePath
        _state.update { it.copy(isRecording = false) }

        if (filePath != null) {
            saveNoteToDatabase(filePath)
            onSaved()
        }
    }

    private fun saveNoteToDatabase(filePath: String) {
        viewModelScope.launch {
            try {
                val file = File(filePath)
                val tagsList = _state.value.tags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                // Здесь сохраняем в базу данных
                // Временно сохраняем в лог
                println("Сохранение заметки: ${_state.value.noteTitle}, путь: $filePath, размер: ${file.length()}")

            } catch (e: Exception) {
                _state.update { it.copy(error = "Ошибка сохранения: ${e.message}") }
            }
        }
    }

    fun updateTitle(title: String) {
        _state.update { it.copy(noteTitle = title) }
    }

    fun updateTags(tags: String) {
        _state.update { it.copy(tags = tags) }
    }

    fun clearTranscription() {
        _state.update { it.copy(transcription = "") }
    }

    fun startSpeechRecognition(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            _state.update { it.copy(error = "Нет разрешения на запись аудио") }
            return
        }

        if (voskModel == null) {
            _state.update { it.copy(error = "Модель Vosk не загружена. Скачайте модель в assets") }
            return
        }

        if (currentFilePath == null) {
            _state.update { it.copy(error = "Нет аудиофайла для распознавания") }
            return
        }

        _state.update { it.copy(isTranscribing = true, error = null) }

        viewModelScope.launch {
            try {
                recognizer = Recognizer(voskModel, 16000.0f)

                val audioFile = File(currentFilePath)
                if (audioFile.exists()) {
                    // Здесь будет распознавание аудиофайла
                    _state.update {
                        it.copy(
                            transcription = "Распознавание речи через Vosk готово. " +
                                    "Для полноценной работы нужна аудиозапись.",
                            isTranscribing = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            transcription = "Аудиофайл не найден",
                            isTranscribing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isTranscribing = false,
                        error = "Ошибка Vosk: ${e.message}"
                    )
                }
            }
        }
    }

    fun onEvent(event: RecorderEvent) {
        when (event) {
            is RecorderEvent.StartRecording -> startRecording()
            is RecorderEvent.StopRecording -> stopRecording {}
            is RecorderEvent.UpdateTitle -> updateTitle(event.title)
            is RecorderEvent.UpdateTags -> updateTags(event.tags)
            is RecorderEvent.ClearTranscription -> clearTranscription()
            is RecorderEvent.StartSpeechRecognition -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        audioRecorder?.stopRecording()
        recognizer?.close()
        voskModel?.close()
    }
}