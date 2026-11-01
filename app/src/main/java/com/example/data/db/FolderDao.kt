package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FolderEntity
import com.example.data.model.NoteFolderCrossRef
import com.example.data.model.TaskFolderCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY name ASC")
    suspend fun getAllFoldersDirect(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE folderType = :folderType ORDER BY name ASC")
    fun getFoldersByType(folderType: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE folderType = :folderType AND parentFolderId IS NULL ORDER BY name ASC")
    fun getRootFoldersByType(folderType: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId IS NULL ORDER BY name ASC")
    fun getRootFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId ORDER BY name ASC")
    fun getSubfolders(parentId: Long): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    @Query("DELETE FROM folders WHERE id != -999 AND id != -998")
    suspend fun deleteAllCustomFolders()

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("DELETE FROM note_folder_cross_ref")
    suspend fun deleteAllNoteCrossRefs()

    @Query("DELETE FROM task_folder_cross_ref")
    suspend fun deleteAllTaskCrossRefs()

    @Query("DELETE FROM folders WHERE parentFolderId = :folderId")
    suspend fun deleteSubfolders(folderId: Long)

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun detachNotesFromFolder(folderId: Long)

    @Query("UPDATE tasks SET folderId = NULL WHERE folderId = :folderId")
    suspend fun detachTasksFromFolder(folderId: Long)

    // Many-to-Many Note Folder Cross-Ref Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteFolderCrossRef(crossRef: NoteFolderCrossRef)

    @Query("DELETE FROM note_folder_cross_ref WHERE noteId = :noteId")
    suspend fun deleteNoteFolderCrossRefsForNote(noteId: Long)

    @Query("DELETE FROM note_folder_cross_ref WHERE folderId = :folderId")
    suspend fun deleteNoteCrossRefsForFolder(folderId: Long)

    @Query("DELETE FROM note_folder_cross_ref WHERE noteId = :noteId AND folderId = :folderId")
    suspend fun deleteNoteFolderCrossRef(noteId: Long, folderId: Long)

    @Query("SELECT folderId FROM note_folder_cross_ref WHERE noteId = :noteId")
    suspend fun getFolderIdsForNote(noteId: Long): List<Long>

    @Query("SELECT folderId FROM note_folder_cross_ref WHERE noteId = :noteId")
    fun getFolderIdsForNoteFlow(noteId: Long): Flow<List<Long>>

    @Query("""
        SELECT f.* FROM folders f
        INNER JOIN note_folder_cross_ref r ON f.id = r.folderId
        WHERE r.noteId = :noteId
        ORDER BY f.name ASC
    """)
    fun getFoldersForNote(noteId: Long): Flow<List<FolderEntity>>

    @Query("SELECT COUNT(*) FROM note_folder_cross_ref WHERE folderId = :folderId")
    fun getNoteCountForFolder(folderId: Long): Flow<Int>

    // Many-to-Many Task Folder Cross-Ref Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskFolderCrossRef(crossRef: TaskFolderCrossRef)

    @Query("DELETE FROM task_folder_cross_ref WHERE taskId = :taskId")
    suspend fun deleteTaskFolderCrossRefsForTask(taskId: Long)

    @Query("DELETE FROM task_folder_cross_ref WHERE folderId = :folderId")
    suspend fun deleteTaskCrossRefsForFolder(folderId: Long)

    @Query("DELETE FROM task_folder_cross_ref WHERE taskId = :taskId AND folderId = :folderId")
    suspend fun deleteTaskFolderCrossRef(taskId: Long, folderId: Long)

    @Query("SELECT folderId FROM task_folder_cross_ref WHERE taskId = :taskId")
    suspend fun getFolderIdsForTask(taskId: Long): List<Long>

    @Query("SELECT folderId FROM task_folder_cross_ref WHERE taskId = :taskId")
    fun getFolderIdsForTaskFlow(taskId: Long): Flow<List<Long>>

    @Query("""
        SELECT f.* FROM folders f
        INNER JOIN task_folder_cross_ref r ON f.id = r.folderId
        WHERE r.taskId = :taskId
        ORDER BY f.name ASC
    """)
    fun getFoldersForTask(taskId: Long): Flow<List<FolderEntity>>

    @Query("SELECT COUNT(*) FROM task_folder_cross_ref WHERE folderId = :folderId")
    fun getTaskCountForFolder(folderId: Long): Flow<Int>
}
