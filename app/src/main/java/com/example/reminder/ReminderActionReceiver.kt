package com.example.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())

        val database = AppDatabase.getDatabase(context)
        val taskDao = database.taskDao()

        when (intent.action) {
            "com.aistudio.notestasks.ACTION_COMPLETE_TASK" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    taskDao.setTaskCompleted(taskId, true)
                }
                Toast.makeText(context, "Task marked as completed! ✓", Toast.LENGTH_SHORT).show()
            }

            "com.aistudio.notestasks.ACTION_SNOOZE_TASK" -> {
                val title = intent.getStringExtra(ReminderManager.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val desc = intent.getStringExtra(ReminderManager.EXTRA_TASK_DESC) ?: ""
                val priority = intent.getIntExtra(ReminderManager.EXTRA_TASK_PRIORITY, 1)
                val snoozeMinutes = intent.getIntExtra("extra_snooze_minutes", 10)

                val newTriggerTime = ReminderManager.snoozeTaskReminder(
                    context = context,
                    taskId = taskId,
                    title = title,
                    description = desc,
                    priority = priority,
                    snoozeMinutes = snoozeMinutes
                )

                CoroutineScope(Dispatchers.IO).launch {
                    taskDao.updateReminder(taskId, newTriggerTime, true)
                }

                Toast.makeText(context, "Reminder snoozed for $snoozeMinutes minutes ⏰", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
