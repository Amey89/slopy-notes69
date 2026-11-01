package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "channel_task_reminders_fullscreen"
        const val CHANNEL_NAME = "Task Reminders (Full Screen)"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(ReminderManager.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val desc = intent.getStringExtra(ReminderManager.EXTRA_TASK_DESC) ?: ""
        val priority = intent.getIntExtra(ReminderManager.EXTRA_TASK_PRIORITY, 1)

        Log.d("ReminderReceiver", "Reminder triggered for taskId: $taskId, title: $title")

        // Temporary wake lock to ensure the screen turns on even in sleep
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "NotesTasks:ReminderWakeLock"
        )
        wakeLock?.acquire(30000L) // 30 seconds max

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context, notificationManager)

        // Full-screen intent to launch FullScreenReminderActivity
        val fullScreenIntent = Intent(context, FullScreenReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
            putExtra(ReminderManager.EXTRA_TASK_TITLE, title)
            putExtra(ReminderManager.EXTRA_TASK_DESC, desc)
            putExtra(ReminderManager.EXTRA_TASK_PRIORITY, priority)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (taskId * 10).toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action (10 mins)
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "com.aistudio.notestasks.ACTION_SNOOZE_TASK"
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
            putExtra(ReminderManager.EXTRA_TASK_TITLE, title)
            putExtra(ReminderManager.EXTRA_TASK_DESC, desc)
            putExtra(ReminderManager.EXTRA_TASK_PRIORITY, priority)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 1).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action
        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "com.aistudio.notestasks.ACTION_COMPLETE_TASK"
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Reminder: $title")
            .setContentText(desc.ifBlank { "Tap to view full screen or complete task" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setLights(Color.MAGENTA, 1000, 500)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_rotate, "Snooze (10m)", snoozePendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Done", completePendingIntent)
            .build()

        notificationManager.notify(taskId.toInt(), notification)

        // Also launch full screen activity directly if possible
        try {
            context.startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Could not start FullScreenReminderActivity directly: ${e.message}")
        }
    }

    private fun createNotificationChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent task alarms and full-screen reminders"
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                    setSound(alarmSound, audioAttributes)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
