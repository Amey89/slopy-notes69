package com.example.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "task_folder_cross_ref",
    primaryKeys = ["taskId", "folderId"],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["taskId"])
    ]
)
data class TaskFolderCrossRef(
    val taskId: Long,
    val folderId: Long
)
