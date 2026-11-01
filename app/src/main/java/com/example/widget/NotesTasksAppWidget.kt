package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Configurable Home Screen AppWidget (defaults to Pure White theme, customizable in App Settings).
 */
class NotesTasksAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, forceWhiteTheme = false)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            notifyWidgetUpdate(context)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            forceWhiteTheme: Boolean = false
        ) {
            val theme = if (forceWhiteTheme) WidgetTheme.LIGHT_CLEAN else WidgetTheme.getSelectedTheme(context)
            val layoutId = if (forceWhiteTheme) R.layout.widget_notes_tasks_white else R.layout.widget_notes_tasks
            val views = RemoteViews(context.packageName, layoutId)

            // Apply selected theme styling and high-contrast text color
            views.setInt(R.id.widget_root, "setBackgroundResource", theme.bgWidgetRes)
            views.setInt(R.id.widget_count_text, "setBackgroundResource", theme.bgItemRes)
            views.setTextColor(R.id.widget_title_text, theme.titleTextColor)
            views.setTextColor(R.id.widget_count_text, theme.countBadgeTextColor)
            views.setTextColor(R.id.widget_empty_view, theme.metaTextColor)

            // Open App on header click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                if (forceWhiteTheme) 2 else 0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header_container, openAppPendingIntent)

            // Set up RemoteViewsService for the scrollable list
            val serviceClass = if (forceWhiteTheme) NotesTasksWhiteWidgetService::class.java else NotesTasksWidgetService::class.java
            val serviceIntent = Intent(context, serviceClass).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("FORCE_WHITE_THEME", forceWhiteTheme)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            // PendingIntent template for list items
            val itemClickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val itemClickPendingIntent = PendingIntent.getActivity(
                context,
                if (forceWhiteTheme) 102 else 101,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)

            // Update item count badge
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val starredNotesCount = db.noteDao().getStarredNotesDirect().size
                    val starredTasksCount = db.taskDao().getStarredTasksWithSubtasksDirect().size
                    val total = starredNotesCount + starredTasksCount
                    views.setTextViewText(R.id.widget_count_text, "$total Starred")
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                } catch (_: Exception) {}
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun notifyWidgetUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)

                // 1. Update configurable widget instances
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, NotesTasksAppWidget::class.java)
                )
                if (ids.isNotEmpty()) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
                    for (id in ids) {
                        updateAppWidget(context, appWidgetManager, id, forceWhiteTheme = false)
                    }
                }

                // 2. Update dedicated White Theme widget instances
                val whiteIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, WhiteNotesTasksAppWidget::class.java)
                )
                if (whiteIds.isNotEmpty()) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(whiteIds, R.id.widget_list_view)
                    for (id in whiteIds) {
                        updateAppWidget(context, appWidgetManager, id, forceWhiteTheme = true)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

/**
 * Dedicated Pure White Themed Home Screen AppWidget available directly in the Android Widget Picker.
 */
class WhiteNotesTasksAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            NotesTasksAppWidget.updateAppWidget(context, appWidgetManager, appWidgetId, forceWhiteTheme = true)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            NotesTasksAppWidget.notifyWidgetUpdate(context)
        }
    }
}
