package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentTaskId"]),
        Index(value = ["isCompleted"])
    ]
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentTaskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)
