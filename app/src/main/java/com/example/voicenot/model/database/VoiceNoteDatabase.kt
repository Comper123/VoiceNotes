package com.example.voicenot.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.voicenot.model.entities.VoiceNoteEntity

@Database(
    entities = [VoiceNoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VoiceNoteDatabase : RoomDatabase() {
    abstract fun voiceNoteDao(): VoiceNoteDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceNoteDatabase? = null

        fun getInstance(context: Context): VoiceNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceNoteDatabase::class.java,
                    "voice_notes.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}