package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isStarred = 1 ORDER BY updatedAt DESC")
    fun getStarredNotes(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%' 
           OR tags LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdDirect(id: Long): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesDirect(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isStarred = 1 ORDER BY updatedAt DESC")
    suspend fun getStarredNotesDirect(): List<NoteEntity>

    @Query("""
        SELECT DISTINCT n.* FROM notes n 
        LEFT JOIN note_folder_cross_ref r ON n.id = r.noteId 
        WHERE r.folderId = :folderId OR n.folderId = :folderId 
        ORDER BY n.updatedAt DESC
    """)
    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET folderId = :folderId WHERE id = :noteId")
    suspend fun setNoteFolder(noteId: Long, folderId: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM notes WHERE isStarred = 0")
    suspend fun deleteUnstarredNotes()

    @Query("DELETE FROM notes WHERE isStarred = 1")
    suspend fun deleteStarredNotes()

    @Query("DELETE FROM notes WHERE folderId IS NOT NULL")
    suspend fun deleteNotesInFolders()

    @Query("UPDATE notes SET isStarred = :isStarred, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setStarred(id: Long, isStarred: Boolean, updatedAt: Long = System.currentTimeMillis())
}
