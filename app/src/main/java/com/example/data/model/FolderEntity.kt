package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentFolderId"]),
        Index(value = ["name"]),
        Index(value = ["folderType"])
    ]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val folderType: String = "NOTE", // "NOTE" or "TASK"
    val parentFolderId: Long? = null, // null = root level folder; Long = subfolder under parent
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val COMPLETED_TASKS_FOLDER_ID = -999L
        const val SHARED_LINKS_FOLDER_ID = -998L

        val PermanentCompletedFolder = FolderEntity(
            id = COMPLETED_TASKS_FOLDER_ID,
            name = "Completed Tasks",
            folderType = "TASK",
            colorIndex = 1 // Emerald green
        )

        val PermanentSharedLinksFolder = FolderEntity(
            id = SHARED_LINKS_FOLDER_ID,
            name = "Shared Links",
            folderType = "NOTE",
            colorIndex = 2 // Ocean Blue
        )
    }
}
