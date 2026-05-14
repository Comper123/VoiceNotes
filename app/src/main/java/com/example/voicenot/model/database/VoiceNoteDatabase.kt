package com.example.voicenot.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.voicenot.model.entities.VoiceNoteEntity
import com.example.voicenot.model.entities.FolderEntity

@Database(
    entities = [VoiceNoteEntity::class, FolderEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VoiceNoteDatabase : RoomDatabase() {
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun folderDao():   FolderDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceNoteDatabase? = null

        fun getInstance(context: Context): VoiceNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceNoteDatabase::class.java,
                    "voice_notes_v2.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}