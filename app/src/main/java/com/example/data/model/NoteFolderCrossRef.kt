package com.example.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "note_folder_cross_ref",
    primaryKeys = ["noteId", "folderId"],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["noteId"])
    ]
)
data class NoteFolderCrossRef(
    val noteId: Long,
    val folderId: Long
)
