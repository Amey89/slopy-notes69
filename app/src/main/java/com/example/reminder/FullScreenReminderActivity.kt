package com.example.reminder

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskWithSubtasks
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullScreenReminderActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLockScreenFlags()
        enableEdgeToEdge()

        val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
        val initialTitle = intent.getStringExtra(ReminderManager.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val initialDesc = intent.getStringExtra(ReminderManager.EXTRA_TASK_DESC) ?: ""
        val priority = intent.getIntExtra(ReminderManager.EXTRA_TASK_PRIORITY, 1)

        startAlarmSoundAndVibration()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                FullScreenReminderScreen(
                    taskId = taskId,
                    initialTitle = initialTitle,
                    initialDesc = initialDesc,
                    priority = priority,
                    onSnooze = { minutes ->
                        stopAlarm()
                        dismissNotification(taskId)
                        snoozeTask(taskId, initialTitle, initialDesc, priority, minutes)
                        finish()
                    },
                    onComplete = {
                        stopAlarm()
                        dismissNotification(taskId)
                        completeTask(taskId)
                        finish()
                    },
                    onDismiss = {
                        stopAlarm()
                        dismissNotification(taskId)
                        finish()
                    },
                    onOpenApp = {
                        stopAlarm()
                        dismissNotification(taskId)
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("OPEN_TASK_ID", taskId)
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun setupLockScreenFlags() {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
    }

    private fun startAlarmSoundAndVibration() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@FullScreenReminderActivity, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            val pattern = longArrayOf(0, 500, 300, 500, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissNotification(taskId: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())
    }

    private fun completeTask(taskId: Long) {
        if (taskId <= 0) return
        val db = AppDatabase.getDatabase(this)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            db.taskDao().setTaskCompleted(taskId, true)
        }
    }

    private fun snoozeTask(taskId: Long, title: String, desc: String, priority: Int, minutes: Int) {
        if (taskId <= 0) return
        val newTriggerTime = ReminderManager.snoozeTaskReminder(
            context = this,
            taskId = taskId,
            title = title,
            description = desc,
            priority = priority,
            snoozeMinutes = minutes
        )
        val db = AppDatabase.getDatabase(this)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            db.taskDao().updateReminder(taskId, newTriggerTime, true)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullScreenReminderScreen(
    taskId: Long,
    initialTitle: String,
    initialDesc: String,
    priority: Int,
    onSnooze: (Int) -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    val taskWithSubtasks by db.taskDao()
        .getTaskWithSubtasksById(taskId)
        .collectAsState(initial = null)

    val currentTask = taskWithSubtasks?.task
    val title = currentTask?.title ?: initialTitle
    val description = currentTask?.description ?: initialDesc
    val subtasks = taskWithSubtasks?.subtasks ?: emptyList()

    // Pulsing animation for alarm beacon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Deep night slate
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1B4B), // Deep indigo
                            Color(0xFF0F172A), // Slate 900
                            Color(0xFF030712)  // Dark void
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (priority) {
                                    2 -> Color(0xFFDC2626) // High Red
                                    0 -> Color(0xFF3B82F6) // Low Blue
                                    else -> Color(0xFFF59E0B) // Amber
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (priority) {
                                2 -> "HIGH PRIORITY"
                                0 -> "LOW PRIORITY"
                                else -> "TASK REMINDER"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dismiss", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Pulsing Alarm Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(pulseScale)
                            .background(Color(0xFF6366F1).copy(alpha = 0.25f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF4F46E5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alarm ringing",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Task Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = description,
                        color = Color(0xFFCBD5E1),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }

                // Subtasks preview (if any)
                if (subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "SUB-TASKS (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(subtasks, key = { it.id }) { subtask ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Checkbox(
                                            checked = subtask.isCompleted,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    db.taskDao().setSubTaskCompleted(subtask.id, checked)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF10B981),
                                                uncheckedColor = Color(0xFF64748B),
                                                checkmarkColor = Color.White
                                            )
                                        )
                                        Text(
                                            text = subtask.title,
                                            color = if (subtask.isCompleted) Color(0xFF64748B) else Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Snooze Options Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SNOOZE REMINDER",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val snoozeOptions = listOf(5 to "+5m", 10 to "+10m", 15 to "+15m", 30 to "+30m", 60 to "+1h")
                        snoozeOptions.forEach { (mins, label) ->
                            FilterChip(
                                selected = false,
                                onClick = { onSnooze(mins) },
                                label = { Text(label, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Snooze,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    labelColor = Color.White,
                                    iconColor = Color(0xFFFBBF24)
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action: Mark Completed
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Mark as Completed",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: Open Task in App
                OutlinedButton(
                    onClick = onOpenApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Task Details in App", fontSize = 14.sp)
                }
            }
        }
    }
}
