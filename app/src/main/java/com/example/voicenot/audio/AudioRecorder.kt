package com.example.voicenot.audio

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var startTime: Long = 0L
    private var currentFilePath: String = ""

    fun startRecording(): Boolean {
        return try {
            // Создаём папку для записей
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
                Log.d("AudioRecorder", "Создана папка: ${recordingsDir.absolutePath}")
            }

            currentFilePath = File(recordingsDir, "recording_${System.currentTimeMillis()}.m4a").absolutePath
            Log.d("AudioRecorder", "Файл: $currentFilePath")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentFilePath)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
            Log.d("AudioRecorder", "Запись начата")
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Ошибка: ${e.message}", e)
            false
        }
    }

    fun stopRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("AudioRecorder", "Запись остановлена. Путь: $currentFilePath")

            val file = File(currentFilePath)
            if (file.exists() && file.length() > 0) {
                Log.d("AudioRecorder", "Файл сохранён, размер: ${file.length()} байт")
                currentFilePath
            } else {
                Log.e("AudioRecorder", "Файл пустой или не существует!")
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Ошибка: ${e.message}", e)
            null
        }
    }

    fun getCurrentDuration(): Long {
        return if (mediaRecorder != null) {
            System.currentTimeMillis() - startTime
        } else 0L
    }
}