package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["isCompleted"]),
        Index(value = ["isStarred"]),
        Index(value = ["folderId"]),
        Index(value = ["reminderTime"]),
        Index(value = ["dueDate"]),
        Index(value = ["updatedAt"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isStarred: Boolean = false,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val reminderEnabled: Boolean = false,
    val priority: Int = 1, // 0 = Low, 1 = Normal, 2 = High
    val tags: String = "",
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
