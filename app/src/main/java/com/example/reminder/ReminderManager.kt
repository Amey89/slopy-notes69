package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object ReminderManager {
    private const val TAG = "ReminderManager"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_DESC = "extra_task_desc"
    const val EXTRA_TASK_PRIORITY = "extra_task_priority"

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun scheduleTaskReminder(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        priority: Int,
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule reminder in the past for task $taskId")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.aistudio.notestasks.ACTION_TASK_REMINDER"
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_DESC, description)
            putExtra(EXTRA_TASK_PRIORITY, priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val showIntent = PendingIntent.getActivity(
                context,
                (taskId * 10).toInt(),
                Intent(context, FullScreenReminderActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TASK_TITLE, title)
                    putExtra(EXTRA_TASK_DESC, description)
                    putExtra(EXTRA_TASK_PRIORITY, priority)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Scheduled alarm clock reminder for task $taskId at $triggerAtMillis")
        } catch (e: Exception) {
            Log.w(TAG, "setAlarmClock failed (${e.message}), falling back to setExactAndAllowWhileIdle")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled exact fallback reminder for task $taskId at $triggerAtMillis")
            } catch (se: SecurityException) {
                Log.e(TAG, "SecurityException scheduling exact alarm: ${se.message}. Falling back to inexact.", se)
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.aistudio.notestasks.ACTION_TASK_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled reminder for task $taskId")
    }

    fun snoozeTaskReminder(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        priority: Int,
        snoozeMinutes: Int = 10
    ): Long {
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        scheduleTaskReminder(context, taskId, title, description, priority, snoozeTime)
        return snoozeTime
    }
}
