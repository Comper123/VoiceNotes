package com.example.voicenot.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var startTime: Long = 0L
    private var currentFilePath: String = ""

    fun startRecording(): Boolean {
        return try {
            // Создаём уникальное имя файла
            val timestamp = System.currentTimeMillis()
            currentFilePath = File(context.cacheDir, "recording_$timestamp.m4a").absolutePath

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentFilePath)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopRecording(): String? {
        return try {
            if (mediaRecorder != null) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    release()
                }
                mediaRecorder = null
            }
            currentFilePath.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCurrentDuration(): Long {
        return if (mediaRecorder != null) {
            System.currentTimeMillis() - startTime
        } else 0L
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}