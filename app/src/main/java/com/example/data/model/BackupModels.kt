package com.example.data.model

data class BackupPayload(
    val exportVersion: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val notes: List<BackupNote> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val folders: List<BackupFolder> = emptyList(),
    val tags: List<BackupTag> = emptyList()
)

data class BackupTag(
    val name: String,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class BackupFolder(
    val id: Long = 0,
    val name: String,
    val folderType: String = "NOTE", // "NOTE" or "TASK"
    val parentFolderId: Long? = null,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class BackupNote(
    val title: String,
    val content: String,
    val tags: String = "",
    val imageUris: String = "",
    val isStarred: Boolean = false,
    val colorIndex: Int = 0,
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BackupTask(
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isStarred: Boolean = false,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val reminderEnabled: Boolean = false,
    val priority: Int = 1,
    val tags: String = "",
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val subtasks: List<BackupSubTask> = emptyList()
)

data class BackupSubTask(
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)
