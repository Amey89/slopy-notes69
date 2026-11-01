package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, dueDate ASC, updatedAt DESC")
    fun getAllTasksWithSubtasks(): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isStarred = 1 ORDER BY isCompleted ASC, priority DESC, dueDate ASC, updatedAt DESC")
    fun getStarredTasksWithSubtasks(): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY isCompleted ASC, priority DESC, updatedAt DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskWithSubtasksById(id: Long): Flow<TaskWithSubtasks?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdDirect(id: Long): TaskEntity?

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskWithSubtasksByIdDirect(id: Long): TaskWithSubtasks?

    @Transaction
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun getAllTasksWithSubtasksDirect(): List<TaskWithSubtasks>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isStarred = 1 ORDER BY isCompleted ASC, priority DESC, dueDate ASC, updatedAt DESC")
    suspend fun getStarredTasksWithSubtasksDirect(): List<TaskWithSubtasks>

    @Transaction
    @Query("""
        SELECT DISTINCT t.* FROM tasks t 
        LEFT JOIN task_folder_cross_ref r ON t.id = r.taskId 
        WHERE r.folderId = :folderId OR t.folderId = :folderId 
        ORDER BY t.isCompleted ASC, t.priority DESC, t.dueDate ASC, t.updatedAt DESC
    """)
    fun getTasksWithSubtasksByFolder(folderId: Long): Flow<List<TaskWithSubtasks>>

    @Query("UPDATE tasks SET folderId = :folderId WHERE id = :taskId")
    suspend fun setTaskFolder(taskId: Long, folderId: Long?)

    @Query("SELECT * FROM tasks WHERE reminderEnabled = 1 AND isCompleted = 0 AND reminderTime IS NOT NULL AND reminderTime > :now")
    suspend fun getActivePendingReminders(now: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    @Query("DELETE FROM tasks WHERE isCompleted = 0")
    suspend fun deletePendingTasks()

    @Query("DELETE FROM tasks WHERE isStarred = 1")
    suspend fun deleteStarredTasks()

    @Query("DELETE FROM subtasks")
    suspend fun deleteAllSubtasks()

    @Query("UPDATE tasks SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isStarred = :isStarred, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTaskStarred(id: Long, isStarred: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET reminderTime = :reminderTime, reminderEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateReminder(id: Long, reminderTime: Long?, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    // Subtasks operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(subTasks: List<SubTaskEntity>): List<Long>

    @Update
    suspend fun updateSubTask(subTask: SubTaskEntity)

    @Delete
    suspend fun deleteSubTask(subTask: SubTaskEntity)

    @Query("DELETE FROM subtasks WHERE parentTaskId = :taskId")
    suspend fun deleteSubTasksByTaskId(taskId: Long)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubTaskById(subtaskId: Long)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subtaskId")
    suspend fun setSubTaskCompleted(subtaskId: Long, isCompleted: Boolean)
}
