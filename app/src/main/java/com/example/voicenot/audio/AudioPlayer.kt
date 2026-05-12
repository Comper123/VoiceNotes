package com.example.voicenot.audio

import android.media.MediaPlayer

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var onCompletionListener: (() -> Unit)? = null
    private var onErrorListener: (() -> Unit)? = null

    fun play(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener {
                    onCompletionListener?.invoke()
                    release()
                }
                setOnErrorListener { _, _, _ ->
                    onErrorListener?.invoke()
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onErrorListener?.invoke()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }

    fun setOnErrorListener(listener: () -> Unit) {
        onErrorListener = listener
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}