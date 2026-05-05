package com.example.voicenotes.data.entities

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import androidx.room3.Update

// data/entities/VoiceNoteEntity.kt
@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "duration")
    val duration: Long,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "transcription")
    val transcription: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "tags")
    val tags: String = ""  // JSON строка с тегами
)

// data/database/Converters.kt
class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(",")
    }
}

// data/database/VoiceNoteDao.kt
@Dao
interface VoiceNoteDao {
    // CRUD операции
    @Insert
    suspend fun insertNote(note: VoiceNoteEntity): Long

    @Update
    suspend fun updateNote(note: VoiceNoteEntity)

    @Delete
    suspend fun deleteNote(note: VoiceNoteEntity)

    // Query операции
    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): VoiceNoteEntity?

    @Query("SELECT * FROM voice_notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE title LIKE '%' || :query || '%' OR transcription LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<VoiceNoteEntity>>

    @Query("DELETE FROM voice_notes WHERE created_at < :olderThan")
    suspend fun deleteOldNotes(olderThan: Long)

    @Query("SELECT SUM(file_size) FROM voice_notes")
    fun getTotalStorageSize(): Flow<Long>
}

// data/database/VoiceNoteDatabase.kt
@Database(
    entities = [VoiceNoteEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VoiceNoteDatabase : RoomDatabase() {
    abstract fun voiceNoteDao(): VoiceNoteDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceNoteDatabase? = null

        fun getInstance(context: Context): VoiceNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    VoiceNoteDatabase::class.java,
                    "voice_notes.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Инициализация базы при создании
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_created_at ON voice_notes(created_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_favorite ON voice_notes(is_favorite)")
            }
        }
    }
}