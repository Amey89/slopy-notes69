package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskWithSubtasks
import com.example.ui.model.TaskUiItem

// Top-level static constants to prevent object allocations during 120Hz fast scrolling
private val TaskCardShape = RoundedCornerShape(26.dp)
private val SquircleCheckShape = RoundedCornerShape(13.dp)
private val ActionPillShape = RoundedCornerShape(14.dp)
private val CompletedBadgeShape = RoundedCornerShape(50)
private val BadgeShape = RoundedCornerShape(50)

private val HighPriorityColor = Color(0xFF71717A)
private val MediumPriorityColor = Color(0xFF52525B)
private val LowPriorityColor = Color(0xFF78716C)

/**
 * 120Hz Smooth Scrolling Optimized TaskCard — Distraction-Free Neutral Edition.
 * Uses pre-computed [TaskUiItem] where date formatting, priority styling,
 * and subtask metrics are pre-calculated.
 */
@Composable
fun TaskCard(
    uiItem: TaskUiItem,
    onClick: (TaskWithSubtasks) -> Unit,
    onToggleCompleted: (Long, Boolean) -> Unit,
    onToggleStar: (TaskWithSubtasks) -> Unit,
    onToggleSubtask: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    performanceMode: Boolean = false
) {
    val taskWithSubtasks = uiItem.taskWithSubtasks
    val task = taskWithSubtasks.task
    val isDark = isSystemInDarkTheme()

    val effectiveContainerColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val outline = MaterialTheme.colorScheme.outline
    val cardBorder = remember(task.isCompleted, task.isStarred, outlineVariant, outline) {
        when {
            task.isCompleted -> BorderStroke(1.dp, outlineVariant.copy(alpha = 0.5f))
            task.isStarred -> BorderStroke(1.2.dp, outline.copy(alpha = 0.8f))
            else -> BorderStroke(1.dp, outlineVariant.copy(alpha = 0.6f))
        }
    }

    val onCardClick = remember(onClick, taskWithSubtasks) { { onClick(taskWithSubtasks) } }
    val onCompletionClick = remember(onToggleCompleted, task.id, task.isCompleted) {
        { onToggleCompleted(task.id, !task.isCompleted) }
    }
    val onStarClick = remember(onToggleStar, taskWithSubtasks) { { onToggleStar(taskWithSubtasks) } }
    val onDeleteClick = remember(onDelete, task.id) { { onDelete(task.id) } }
    val onSubtaskToggle: (SubTaskEntity, Boolean) -> Unit = remember(onToggleSubtask) {
        { subtask, isDone -> onToggleSubtask(subtask.id, isDone) }
    }

    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = TaskCardShape,
        colors = CardDefaults.cardColors(containerColor = effectiveContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main row: Squircle Checkbox, Title, Priority, Neutral Star & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCompletionClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                color = if (task.isCompleted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = SquircleCheckShape
                            )
                            .border(
                                width = if (task.isCompleted) 0.dp else 1.5.dp,
                                color = if (task.isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline,
                                shape = SquircleCheckShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (task.isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CompletedBadgeShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onStarClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (task.isStarred) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ActionPillShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (task.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (task.isStarred) "Unstar" else "Star",
                                tint = if (task.isStarred) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ActionPillShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete task",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Badges Row: Neutral Pill Priority + Reminder + Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Badge Pill
                Surface(
                    shape = BadgeShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = uiItem.priorityText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Reminder Badge (Pre-formatted)
                if (uiItem.formattedReminder != null) {
                    Surface(
                        shape = BadgeShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Reminder active",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiItem.formattedReminder,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Due Date Badge (Pre-formatted)
                if (uiItem.formattedDueDate != null) {
                    Surface(
                        shape = BadgeShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = uiItem.formattedDueDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Expandable Subtasks
            if (taskWithSubtasks.subtasks.isNotEmpty()) {
                TaskCardSubtaskViewer(
                    subtasks = taskWithSubtasks.subtasks,
                    onToggleSubtask = onSubtaskToggle
                )
            }
        }
    }
}

/**
 * Backward-compatible overload for TaskCard directly taking [TaskWithSubtasks].
 */
@Composable
fun TaskCard(
    taskWithSubtasks: TaskWithSubtasks,
    onClick: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onToggleStar: () -> Unit,
    onToggleSubtask: (SubTaskEntity, Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    performanceMode: Boolean = false
) {
    val task = taskWithSubtasks.task
    val reminderStr = remember(task.reminderEnabled, task.reminderTime) {
        if (task.reminderEnabled && task.reminderTime != null) {
            AppDateFormatter.format(task.reminderTime)
        } else null
    }

    val dueStr = remember(task.dueDate, task.reminderTime) {
        if (task.dueDate != null && (task.reminderTime == null || task.dueDate != task.reminderTime)) {
            "Due: ${AppDateFormatter.format(task.dueDate)}"
        } else null
    }

    val (pText, pColor) = remember(task.priority) {
        when (task.priority) {
            2 -> "High" to HighPriorityColor
            0 -> "Low" to LowPriorityColor
            else -> "Medium" to MediumPriorityColor
        }
    }

    val uiItem = remember(taskWithSubtasks, reminderStr, dueStr, pText, pColor) {
        TaskUiItem(
            taskWithSubtasks = taskWithSubtasks,
            formattedReminder = reminderStr,
            formattedDueDate = dueStr,
            priorityText = pText,
            priorityColor = pColor,
            subtaskSummary = "${taskWithSubtasks.completedSubtaskCount}/${taskWithSubtasks.totalSubtaskCount} subtasks",
            subtaskProgress = taskWithSubtasks.subtaskProgress
        )
    }

    TaskCard(
        uiItem = uiItem,
        onClick = { onClick() },
        onToggleCompleted = { _, isDone -> onToggleCompleted(isDone) },
        onToggleStar = { onToggleStar() },
        onToggleSubtask = { id, isDone ->
            val subtask = taskWithSubtasks.subtasks.find { it.id == id }
            if (subtask != null) onToggleSubtask(subtask, isDone)
        },
        onDelete = { onDelete() },
        modifier = modifier,
        performanceMode = performanceMode
    )
}
