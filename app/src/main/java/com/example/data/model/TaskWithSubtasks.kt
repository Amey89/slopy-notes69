package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation

@Immutable
data class TaskWithSubtasks(
    @Embedded
    val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentTaskId"
    )
    val subtasks: List<SubTaskEntity> = emptyList()
) {
    val completedSubtaskCount: Int
        get() = subtasks.count { it.isCompleted }

    val totalSubtaskCount: Int
        get() = subtasks.size

    val subtaskProgress: Float
        get() = if (totalSubtaskCount == 0) 0f else completedSubtaskCount.toFloat() / totalSubtaskCount

    val hasReminders: Boolean
        get() = task.reminderEnabled && task.reminderTime != null
}
