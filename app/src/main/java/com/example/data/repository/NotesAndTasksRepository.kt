package com.example.data.repository

import android.content.Context
import com.example.data.db.FolderDao
import com.example.data.db.NoteDao
import com.example.data.db.TagDao
import com.example.data.db.TaskDao
import com.example.data.model.BackupFolder
import com.example.data.model.BackupNote
import com.example.data.model.BackupPayload
import com.example.data.model.BackupSubTask
import com.example.data.model.BackupTag
import com.example.data.model.BackupTask
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.SubTaskEntity
import com.example.data.model.TagEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithSubtasks
import com.example.data.sync.MarkdownImportSummary
import com.example.data.sync.MarkdownZipBundleManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAndTasksRepository(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao
) {
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val starredNotes: Flow<List<NoteEntity>> = noteDao.getStarredNotes()

    val allTasks: Flow<List<TaskWithSubtasks>> = taskDao.getAllTasksWithSubtasks()
    val starredTasks: Flow<List<TaskWithSubtasks>> = taskDao.getStarredTasksWithSubtasks()

    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val rootFolders: Flow<List<FolderEntity>> = folderDao.getRootFolders()
    val noteFolders: Flow<List<FolderEntity>> = folderDao.getFoldersByType("NOTE")
    val rootNoteFolders: Flow<List<FolderEntity>> = folderDao.getRootFoldersByType("NOTE")
    val taskFolders: Flow<List<FolderEntity>> = folderDao.getFoldersByType("TASK")
    val rootTaskFolders: Flow<List<FolderEntity>> = folderDao.getRootFoldersByType("TASK")

    fun getSubfolders(parentId: Long): Flow<List<FolderEntity>> = folderDao.getSubfolders(parentId)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    fun searchTasks(query: String): Flow<List<TaskWithSubtasks>> = taskDao.searchTasks(query)

    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>> = noteDao.getNotesByFolder(folderId)
    fun getTasksByFolder(folderId: Long): Flow<List<TaskWithSubtasks>> = taskDao.getTasksWithSubtasksByFolder(folderId)

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)
    fun getTaskWithSubtasksById(id: Long): Flow<TaskWithSubtasks?> = taskDao.getTaskWithSubtasksById(id)

    fun getFolderIdsForNoteFlow(noteId: Long): Flow<List<Long>> = folderDao.getFolderIdsForNoteFlow(noteId)
    fun getFoldersForNote(noteId: Long): Flow<List<FolderEntity>> = folderDao.getFoldersForNote(noteId)
    suspend fun getFolderIdsForNote(noteId: Long): List<Long> = withContext(Dispatchers.IO) {
        folderDao.getFolderIdsForNote(noteId)
    }

    fun getFolderIdsForTaskFlow(taskId: Long): Flow<List<Long>> = folderDao.getFolderIdsForTaskFlow(taskId)
    fun getFoldersForTask(taskId: Long): Flow<List<FolderEntity>> = folderDao.getFoldersForTask(taskId)
    suspend fun getFolderIdsForTask(taskId: Long): List<Long> = withContext(Dispatchers.IO) {
        folderDao.getFolderIdsForTask(taskId)
    }

    suspend fun createFolder(
        name: String,
        folderType: String = "NOTE",
        parentFolderId: Long? = null,
        colorIndex: Int = 0
    ): Long = withContext(Dispatchers.IO) {
        folderDao.insertFolder(
            FolderEntity(
                name = name.trim(),
                folderType = folderType,
                parentFolderId = parentFolderId,
                colorIndex = colorIndex
            )
        )
    }

    suspend fun updateFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
        folderDao.deleteNoteCrossRefsForFolder(folderId)
        folderDao.deleteTaskCrossRefsForFolder(folderId)
        folderDao.detachNotesFromFolder(folderId)
        folderDao.detachTasksFromFolder(folderId)
        folderDao.deleteSubfolders(folderId)
        folderDao.deleteFolder(folderId)
    }

    suspend fun createTag(name: String, colorIndex: Int = 0): Unit = withContext(Dispatchers.IO) {
        val clean = name.trim().removePrefix("#")
        if (clean.isNotBlank()) {
            tagDao.insertTag(TagEntity(name = clean, colorIndex = colorIndex))
        }
    }

    suspend fun deleteTag(name: String): Unit = withContext(Dispatchers.IO) {
        tagDao.deleteTag(name)
    }

    suspend fun ensureDefaultTags(): Unit = withContext(Dispatchers.IO) {
        if (tagDao.getTagCount() == 0) {
            val defaults = listOf(
                TagEntity("Work", colorIndex = 0),
                TagEntity("Personal", colorIndex = 1),
                TagEntity("Ideas", colorIndex = 2),
                TagEntity("Urgent", colorIndex = 3),
                TagEntity("Study", colorIndex = 4),
                TagEntity("Project", colorIndex = 5)
            )
            defaults.forEach { tagDao.insertTag(it) }
        }
    }

    suspend fun setNoteFolders(noteId: Long, folderIds: List<Long>) = withContext(Dispatchers.IO) {
        folderDao.deleteNoteFolderCrossRefsForNote(noteId)
        folderIds.distinct().forEach { fId ->
            folderDao.insertNoteFolderCrossRef(com.example.data.model.NoteFolderCrossRef(noteId, fId))
        }
        val firstFolderId = folderIds.firstOrNull()
        noteDao.setNoteFolder(noteId, firstFolderId)
    }

    suspend fun setTaskFolders(taskId: Long, folderIds: List<Long>) = withContext(Dispatchers.IO) {
        folderDao.deleteTaskFolderCrossRefsForTask(taskId)
        folderIds.distinct().forEach { fId ->
            folderDao.insertTaskFolderCrossRef(com.example.data.model.TaskFolderCrossRef(taskId, fId))
        }
        val firstFolderId = folderIds.firstOrNull()
        taskDao.setTaskFolder(taskId, firstFolderId)
    }

    suspend fun saveNoteWithFolders(note: NoteEntity, folderIds: List<Long>): Long = withContext(Dispatchers.IO) {
        val firstFolder = folderIds.firstOrNull()
        val noteToSave = note.copy(folderId = firstFolder)
        val noteId = if (noteToSave.id == 0L) {
            noteDao.insert(noteToSave)
        } else {
            noteDao.update(noteToSave.copy(updatedAt = System.currentTimeMillis()))
            noteToSave.id
        }
        folderDao.deleteNoteFolderCrossRefsForNote(noteId)
        folderIds.distinct().forEach { fId ->
            folderDao.insertNoteFolderCrossRef(com.example.data.model.NoteFolderCrossRef(noteId, fId))
        }
        noteId
    }

    suspend fun setNoteFolder(noteId: Long, folderId: Long?) = withContext(Dispatchers.IO) {
        setNoteFolders(noteId, if (folderId != null) listOf(folderId) else emptyList())
    }

    suspend fun setTaskFolder(taskId: Long, folderId: Long?) = withContext(Dispatchers.IO) {
        setTaskFolders(taskId, if (folderId != null) listOf(folderId) else emptyList())
    }

    suspend fun insertNote(note: NoteEntity): Long = withContext(Dispatchers.IO) {
        noteDao.insert(note)
    }

    suspend fun updateNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.delete(note)
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteById(id)
    }

    suspend fun toggleNoteStar(id: Long, currentStarred: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setStarred(id, !currentStarred)
    }

    // Task and subtask operations
    suspend fun insertTask(task: TaskEntity, subtasks: List<String> = emptyList()): Long = withContext(Dispatchers.IO) {
        val taskId = taskDao.insertTask(task)
        if (subtasks.isNotEmpty()) {
            val entities = subtasks.mapIndexed { index, title ->
                SubTaskEntity(
                    parentTaskId = taskId,
                    title = title,
                    isCompleted = false,
                    orderIndex = index
                )
            }
            taskDao.insertSubTasks(entities)
        }
        taskId
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveTaskWithSubtasks(
        task: TaskEntity,
        subtasks: List<SubTaskEntity>
    ): Long = withContext(Dispatchers.IO) {
        val taskId = if (task.id == 0L) {
            taskDao.insertTask(task)
        } else {
            taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
            task.id
        }

        // Replace subtasks
        taskDao.deleteSubTasksByTaskId(taskId)
        if (subtasks.isNotEmpty()) {
            val updatedSubtasks = subtasks.mapIndexed { idx, st ->
                st.copy(id = 0, parentTaskId = taskId, orderIndex = idx)
            }
            taskDao.insertSubTasks(updatedSubtasks)
        }
        taskId
    }

    suspend fun saveTaskWithFolders(
        task: TaskEntity,
        subtasks: List<SubTaskEntity>,
        folderIds: List<Long>
    ): Long = withContext(Dispatchers.IO) {
        val firstFolder = folderIds.firstOrNull()
        val taskToSave = task.copy(folderId = firstFolder)
        val taskId = saveTaskWithSubtasks(taskToSave, subtasks)
        folderDao.deleteTaskFolderCrossRefsForTask(taskId)
        folderIds.distinct().forEach { fId ->
            folderDao.insertTaskFolderCrossRef(com.example.data.model.TaskFolderCrossRef(taskId, fId))
        }
        taskId
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) = withContext(Dispatchers.IO) {
        taskDao.deleteTaskById(id)
    }

    suspend fun toggleTaskCompleted(id: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        taskDao.setTaskCompleted(id, isCompleted)
    }

    suspend fun toggleTaskStar(id: Long, currentStarred: Boolean) = withContext(Dispatchers.IO) {
        taskDao.setTaskStarred(id, !currentStarred)
    }

    suspend fun updateTaskReminder(id: Long, reminderTime: Long?, enabled: Boolean) = withContext(Dispatchers.IO) {
        taskDao.updateReminder(id, reminderTime, enabled)
    }

    suspend fun addSubTask(parentTaskId: Long, title: String): Long = withContext(Dispatchers.IO) {
        taskDao.insertSubTask(
            SubTaskEntity(
                parentTaskId = parentTaskId,
                title = title,
                isCompleted = false
            )
        )
    }

    suspend fun toggleSubTaskCompleted(subtaskId: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        taskDao.setSubTaskCompleted(subtaskId, isCompleted)
    }

    suspend fun deleteSubTask(subtaskId: Long) = withContext(Dispatchers.IO) {
        taskDao.deleteSubTaskById(subtaskId)
    }

    // Direct access
    suspend fun getNoteDirect(id: Long): NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getNoteByIdDirect(id)
    }

    suspend fun getTaskDirect(id: Long): TaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskByIdDirect(id)
    }

    suspend fun getTaskWithSubtasksDirect(id: Long): TaskWithSubtasks? = withContext(Dispatchers.IO) {
        taskDao.getTaskWithSubtasksByIdDirect(id)
    }

    suspend fun getActivePendingReminders(now: Long): List<TaskEntity> = withContext(Dispatchers.IO) {
        taskDao.getActivePendingReminders(now)
    }

    // Local Data Backup & Export / Import
    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        // Explicitly include permanent system folders in JSON backup
        val permanentFolders = listOf(
            BackupFolder(
                id = FolderEntity.COMPLETED_TASKS_FOLDER_ID,
                name = FolderEntity.PermanentCompletedFolder.name,
                folderType = FolderEntity.PermanentCompletedFolder.folderType,
                parentFolderId = null,
                colorIndex = FolderEntity.PermanentCompletedFolder.colorIndex,
                createdAt = FolderEntity.PermanentCompletedFolder.createdAt
            ),
            BackupFolder(
                id = FolderEntity.SHARED_LINKS_FOLDER_ID,
                name = FolderEntity.PermanentSharedLinksFolder.name,
                folderType = FolderEntity.PermanentSharedLinksFolder.folderType,
                parentFolderId = null,
                colorIndex = FolderEntity.PermanentSharedLinksFolder.colorIndex,
                createdAt = FolderEntity.PermanentSharedLinksFolder.createdAt
            )
        )
        val dbFolders = folderDao.getAllFoldersDirect().map {
            BackupFolder(
                id = it.id,
                name = it.name,
                folderType = it.folderType,
                parentFolderId = it.parentFolderId,
                colorIndex = it.colorIndex,
                createdAt = it.createdAt
            )
        }
        val folders = permanentFolders + dbFolders

        val tags = tagDao.getAllTagsDirect().map {
            BackupTag(
                name = it.name,
                colorIndex = it.colorIndex,
                createdAt = it.createdAt
            )
        }

        val notes = noteDao.getAllNotesDirect().map {
            BackupNote(
                title = it.title,
                content = it.content,
                tags = it.tags,
                imageUris = it.imageUris,
                isStarred = it.isStarred,
                colorIndex = it.colorIndex,
                folderId = it.folderId,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
        val tasks = taskDao.getAllTasksWithSubtasksDirect().map { tws ->
            val effectiveFolderId = when {
                tws.task.folderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
                tws.task.isCompleted && tws.task.folderId == null -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
                else -> tws.task.folderId
            }
            BackupTask(
                title = tws.task.title,
                description = tws.task.description,
                isCompleted = tws.task.isCompleted,
                isStarred = tws.task.isStarred,
                dueDate = tws.task.dueDate,
                reminderTime = tws.task.reminderTime,
                reminderEnabled = tws.task.reminderEnabled,
                priority = tws.task.priority,
                tags = tws.task.tags,
                folderId = effectiveFolderId,
                createdAt = tws.task.createdAt,
                updatedAt = tws.task.updatedAt,
                subtasks = tws.subtasks.map {
                    BackupSubTask(
                        title = it.title,
                        isCompleted = it.isCompleted,
                        orderIndex = it.orderIndex
                    )
                }
            )
        }

        val payload = BackupPayload(
            notes = notes,
            tasks = tasks,
            folders = folders,
            tags = tags
        )

        val adapter = moshi.adapter(BackupPayload::class.java)
        adapter.indent("  ").toJson(payload)
    }

    suspend fun importDatabaseFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(jsonString) ?: return@withContext Result.failure(Exception("Invalid JSON format"))

            var importedCount = 0
            val folderIdMap = mutableMapOf<Long, Long>()

            // Pre-seed permanent folder IDs so notes/tasks mapped to them point to the system IDs
            folderIdMap[FolderEntity.COMPLETED_TASKS_FOLDER_ID] = FolderEntity.COMPLETED_TASKS_FOLDER_ID
            folderIdMap[FolderEntity.SHARED_LINKS_FOLDER_ID] = FolderEntity.SHARED_LINKS_FOLDER_ID

            for (f in payload.folders) {
                if (f.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID || f.name.equals("Completed Tasks", ignoreCase = true) || f.name.equals("Permanent Archive", ignoreCase = true)) {
                    folderIdMap[f.id] = FolderEntity.COMPLETED_TASKS_FOLDER_ID
                    continue
                }
                if (f.id == FolderEntity.SHARED_LINKS_FOLDER_ID || f.name.equals("Shared Links", ignoreCase = true)) {
                    folderIdMap[f.id] = FolderEntity.SHARED_LINKS_FOLDER_ID
                    continue
                }
                val newId = folderDao.insertFolder(
                    FolderEntity(
                        name = f.name,
                        folderType = f.folderType ?: "NOTE",
                        parentFolderId = f.parentFolderId?.let { folderIdMap[it] },
                        colorIndex = f.colorIndex,
                        createdAt = f.createdAt
                    )
                )
                folderIdMap[f.id] = newId
                importedCount++
            }

            for (tag in payload.tags) {
                if (tag.name.isNotBlank()) {
                    createTag(tag.name, tag.colorIndex)
                }
            }

            for (note in payload.notes) {
                val mappedFolderId = when {
                    note.folderId == FolderEntity.SHARED_LINKS_FOLDER_ID -> FolderEntity.SHARED_LINKS_FOLDER_ID
                    note.folderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
                    note.folderId != null -> folderIdMap[note.folderId] ?: note.folderId
                    else -> null
                }
                val noteId = noteDao.insert(
                    NoteEntity(
                        title = note.title,
                        content = note.content,
                        tags = note.tags,
                        imageUris = note.imageUris,
                        isStarred = note.isStarred,
                        colorIndex = note.colorIndex,
                        folderId = mappedFolderId,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt
                    )
                )
                if (mappedFolderId != null) {
                    folderDao.insertNoteFolderCrossRef(com.example.data.model.NoteFolderCrossRef(noteId, mappedFolderId))
                }
                importedCount++
            }

            for (task in payload.tasks) {
                val mappedFolderId = when {
                    task.folderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
                    task.folderId == FolderEntity.SHARED_LINKS_FOLDER_ID -> FolderEntity.SHARED_LINKS_FOLDER_ID
                    task.isCompleted && (task.folderId == null || task.folderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID) -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
                    task.folderId != null -> folderIdMap[task.folderId] ?: task.folderId
                    else -> null
                }
                val taskId = taskDao.insertTask(
                    TaskEntity(
                        title = task.title,
                        description = task.description,
                        isCompleted = task.isCompleted,
                        isStarred = task.isStarred,
                        dueDate = task.dueDate,
                        reminderTime = task.reminderTime,
                        reminderEnabled = task.reminderEnabled,
                        priority = task.priority,
                        tags = task.tags,
                        folderId = mappedFolderId,
                        createdAt = task.createdAt,
                        updatedAt = task.updatedAt
                    )
                )
                if (mappedFolderId != null) {
                    folderDao.insertTaskFolderCrossRef(com.example.data.model.TaskFolderCrossRef(taskId, mappedFolderId))
                }
                if (task.subtasks.isNotEmpty()) {
                    val subEntities = task.subtasks.map {
                        SubTaskEntity(
                            parentTaskId = taskId,
                            title = it.title,
                            isCompleted = it.isCompleted,
                            orderIndex = it.orderIndex
                        )
                    }
                    taskDao.insertSubTasks(subEntities)
                }
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportMarkdownZipBundle(context: Context): File = withContext(Dispatchers.IO) {
        val dbFolders = folderDao.getAllFoldersDirect()
        val allFolders = (listOf(FolderEntity.PermanentCompletedFolder, FolderEntity.PermanentSharedLinksFolder) + dbFolders).distinctBy { it.id }
        val notes = noteDao.getAllNotesDirect()
        val tasks = taskDao.getAllTasksWithSubtasksDirect()
        MarkdownZipBundleManager.exportZipBundle(
            context = context,
            folders = allFolders,
            notes = notes,
            tasks = tasks
        )
    }

    suspend fun importMarkdownZipBundle(inputStream: java.io.InputStream): Result<MarkdownImportSummary> = withContext(Dispatchers.IO) {
        MarkdownZipBundleManager.importZipBundle(
            inputStream = inputStream,
            folderDao = folderDao,
            noteDao = noteDao,
            taskDao = taskDao
        )
    }

    suspend fun exportToMarkdownBundle(context: Context): File {
        return exportMarkdownZipBundle(context)
    }

    // Custom Data Deletion options
    suspend fun deleteNotesCustom(
        deleteAllNotes: Boolean,
        deleteUnstarredOnly: Boolean
    ) = withContext(Dispatchers.IO) {
        if (deleteAllNotes) {
            folderDao.deleteAllNoteCrossRefs()
            noteDao.deleteAllNotes()
        } else if (deleteUnstarredOnly) {
            noteDao.deleteUnstarredNotes()
        }
    }

    suspend fun deleteTasksCustom(
        deleteAllTasks: Boolean,
        deleteCompletedOnly: Boolean
    ) = withContext(Dispatchers.IO) {
        if (deleteAllTasks) {
            folderDao.deleteAllTaskCrossRefs()
            taskDao.deleteAllSubtasks()
            taskDao.deleteAllTasks()
        } else if (deleteCompletedOnly) {
            taskDao.deleteCompletedTasks()
        }
    }

    suspend fun deleteCustomFolders() = withContext(Dispatchers.IO) {
        folderDao.deleteAllCustomFolders()
    }

    suspend fun deleteCustomTags() = withContext(Dispatchers.IO) {
        tagDao.deleteAllTags()
    }

    suspend fun clearEntireAppData() = withContext(Dispatchers.IO) {
        folderDao.deleteAllNoteCrossRefs()
        folderDao.deleteAllTaskCrossRefs()
        taskDao.deleteAllSubtasks()
        taskDao.deleteAllTasks()
        noteDao.deleteAllNotes()
        folderDao.deleteAllFolders()
        tagDao.deleteAllTags()
        // Re-seed default permanent folders/tags
        ensureDefaultTags()
    }
}
