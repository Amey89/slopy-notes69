package com.example.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.data.model.NoteEntity
import com.example.data.model.TaskWithSubtasks

/**
 * Pre-computed, fully immutable UI model for Notes.
 * All regex cleaning, markdown parsing, string splitting, URL extraction,
 * and date formatting are performed off the main thread in the ViewModel,
 * ensuring 120Hz zero-jank scrolling inside LazyColumn.
 */
@Immutable
data class NoteUiItem(
    val note: NoteEntity,
    val displayTitle: String,
    val formattedDate: String,
    val cleanPreview: String,
    val imageList: List<String>,
    val tagsList: List<String>,
    val extractedUrl: String?,
    val displayUrl: String?,
    val isSharedLinkNote: Boolean
)

/**
 * Pre-computed, fully immutable UI model for Tasks.
 * All date formatting, priority colors, and subtask progress calculations
 * are pre-calculated to prevent recomposition churn and runtime allocations.
 */
@Immutable
data class TaskUiItem(
    val taskWithSubtasks: TaskWithSubtasks,
    val formattedReminder: String?,
    val formattedDueDate: String?,
    val priorityText: String,
    val priorityColor: Color,
    val subtaskSummary: String,
    val subtaskProgress: Float
)
