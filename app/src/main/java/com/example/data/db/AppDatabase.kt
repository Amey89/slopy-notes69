package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteFolderCrossRef
import com.example.data.model.SubTaskEntity
import com.example.data.model.TagEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskFolderCrossRef

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        SubTaskEntity::class,
        FolderEntity::class,
        NoteFolderCrossRef::class,
        TaskFolderCrossRef::class,
        TagEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes_tasks_master.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
