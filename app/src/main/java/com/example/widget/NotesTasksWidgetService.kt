package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.SubTaskEntity
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class StarredWidgetItem {
    data class NoteItem(
        val id: Long,
        val title: String,
        val content: String,
        val tags: String,
        val updatedAt: Long
    ) : StarredWidgetItem()

    data class TaskItem(
        val id: Long,
        val title: String,
        val description: String,
        val isCompleted: Boolean,
        val priorityText: String,
        val reminderText: String?,
        val subtasks: List<SubTaskEntity>,
        val updatedAt: Long
    ) : StarredWidgetItem()
}

class NotesTasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val forceWhite = intent.getBooleanExtra("FORCE_WHITE_THEME", false)
        return NotesTasksRemoteViewsFactory(applicationContext, forceWhiteTheme = forceWhite)
    }
}

class NotesTasksWhiteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NotesTasksRemoteViewsFactory(applicationContext, forceWhiteTheme = true)
    }
}

class NotesTasksRemoteViewsFactory(
    private val context: Context,
    private val forceWhiteTheme: Boolean = false
) : RemoteViewsService.RemoteViewsFactory {
    private var items: List<StarredWidgetItem> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                val starredNotes = db.noteDao().getStarredNotesDirect()
                val starredTasks = db.taskDao().getStarredTasksWithSubtasksDirect()

                val noteItems = starredNotes.map { note ->
                    StarredWidgetItem.NoteItem(
                        id = note.id,
                        title = note.title.ifBlank { "Untitled Note" },
                        content = note.content,
                        tags = if (note.tags.isNotBlank()) note.tags.split(",").joinToString(" ") { "#${it.trim()}" } else "",
                        updatedAt = note.updatedAt
                    )
                }

                val taskItems = starredTasks.map { tws ->
                    val task = tws.task
                    val priorityStr = when (task.priority) {
                        2 -> "High Priority"
                        0 -> "Low Priority"
                        else -> "Normal"
                    }
                    val reminderStr = if (task.reminderEnabled && task.reminderTime != null) {
                        "⏰ ${dateFormat.format(Date(task.reminderTime))}"
                    } else null

                    StarredWidgetItem.TaskItem(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        isCompleted = task.isCompleted,
                        priorityText = priorityStr,
                        reminderText = reminderStr,
                        subtasks = tws.subtasks,
                        updatedAt = task.updatedAt
                    )
                }

                val combined = mutableListOf<StarredWidgetItem>()
                combined.addAll(noteItems)
                combined.addAll(taskItems)
                combined.sortByDescending {
                    when (it) {
                        is StarredWidgetItem.NoteItem -> it.updatedAt
                        is StarredWidgetItem.TaskItem -> it.updatedAt
                    }
                }
                items = combined
            } catch (e: Exception) {
                items = emptyList()
            }
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in items.indices) return RemoteViews(context.packageName, R.layout.widget_item_starred)
        val views = RemoteViews(context.packageName, R.layout.widget_item_starred)
        val theme = if (forceWhiteTheme) WidgetTheme.LIGHT_CLEAN else WidgetTheme.getSelectedTheme(context)

        // Apply theme background and dynamic text colors (high-contrast slate/black on white, white on dark)
        views.setInt(R.id.widget_item_container, "setBackgroundResource", theme.bgItemRes)
        views.setTextColor(R.id.widget_item_title, theme.titleTextColor)
        views.setTextColor(R.id.widget_item_content, theme.contentTextColor)
        views.setTextColor(R.id.widget_item_meta, theme.metaTextColor)

        when (val item = items[position]) {
            is StarredWidgetItem.NoteItem -> {
                views.setTextViewText(R.id.widget_item_type, "⭐ NOTE")
                views.setTextColor(
                    R.id.widget_item_type,
                    if (theme.isLight) android.graphics.Color.parseColor("#D97706")
                    else android.graphics.Color.parseColor("#F59E0B")
                )
                views.setTextViewText(R.id.widget_item_title, item.title)
                views.setTextViewText(R.id.widget_item_meta, item.tags)

                // Show rendered markdown (checkboxes ☐ / ☑, bold, headings, lists)
                val renderedSpanned = WidgetMarkdownRenderer.renderMarkdownToSpanned(item.content)
                views.setTextViewText(R.id.widget_item_content, renderedSpanned)

                val fillInIntent = Intent().apply {
                    putExtra("EXTRA_NOTE_ID", item.id)
                }
                views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)
            }
            is StarredWidgetItem.TaskItem -> {
                val statusBadge = if (item.isCompleted) "⭐ TASK (COMPLETED)" else "⭐ TASK"
                views.setTextViewText(R.id.widget_item_type, statusBadge)
                val badgeColor = if (item.isCompleted) {
                    if (theme.isLight) android.graphics.Color.parseColor("#059669")
                    else android.graphics.Color.parseColor("#10B981")
                } else {
                    if (theme.isLight) android.graphics.Color.parseColor("#4F46E5")
                    else android.graphics.Color.parseColor("#38BDF8")
                }
                views.setTextColor(R.id.widget_item_type, badgeColor)

                val metaParts = mutableListOf<String>()
                metaParts.add(item.priorityText)
                item.reminderText?.let { metaParts.add(it) }
                views.setTextViewText(R.id.widget_item_meta, metaParts.joinToString(" • "))

                val titleText: CharSequence = if (item.isCompleted) {
                    android.text.Html.fromHtml("<s>${android.text.Html.escapeHtml(item.title)}</s>", android.text.Html.FROM_HTML_MODE_COMPACT)
                } else item.title
                views.setTextViewText(R.id.widget_item_title, titleText)

                // Render task description and formatted subtasks with rendered checkboxes ☐ / ☑
                val taskSpanned = WidgetMarkdownRenderer.renderTaskContextToSpanned(item.description, item.subtasks)
                views.setTextViewText(R.id.widget_item_content, taskSpanned)

                val fillInIntent = Intent().apply {
                    putExtra("EXTRA_TASK_ID", item.id)
                }
                views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)
            }
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
