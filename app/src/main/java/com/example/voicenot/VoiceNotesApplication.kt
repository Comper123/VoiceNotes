package com.example.voicenot


import android.app.Application
import com.example.voicenot.di.AppContainer

class VoiceNotesApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}