package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device rebooted. Restoring active task alarms...")
            val database = AppDatabase.getDatabase(context)
            val taskDao = database.taskDao()

            CoroutineScope(Dispatchers.IO).launch {
                val now = System.currentTimeMillis()
                val pendingTasks = taskDao.getActivePendingReminders(now)
                for (task in pendingTasks) {
                    task.reminderTime?.let { reminderTime ->
                        ReminderManager.scheduleTaskReminder(
                            context = context,
                            taskId = task.id,
                            title = task.title,
                            description = task.description,
                            priority = task.priority,
                            triggerAtMillis = reminderTime
                        )
                    }
                }
                Log.d("BootReceiver", "Restored ${pendingTasks.size} reminders.")
            }
        }
    }
}
