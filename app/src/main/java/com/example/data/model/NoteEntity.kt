package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["isStarred"]),
        Index(value = ["folderId"]),
        Index(value = ["updatedAt"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
