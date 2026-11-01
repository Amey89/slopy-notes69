package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.data.model.NoteEntity
import com.example.data.model.TaskWithSubtasks
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskEditScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.appBackground
import com.example.ui.viewmodel.NotesTasksViewModel

sealed interface Screen {
    data object Home : Screen
    data class NoteEdit(val note: NoteEntity?) : Screen
    data class TaskEdit(val taskWithSubtasks: TaskWithSubtasks?) : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {

    private val viewModel: NotesTasksViewModel by viewModels()
    private val intentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intentState.value = intent

        setContent {
            val darkModeSetting by viewModel.darkModeSetting.collectAsState()
            val themePreset by viewModel.themePreset.collectAsState()
            val backgroundPattern by viewModel.backgroundPattern.collectAsState()
            val currentIntent by intentState

            val isDark = when (darkModeSetting) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(
                darkTheme = isDark,
                themePreset = themePreset
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .appBackground(themePreset, backgroundPattern)
                    ) {
                        MainAppNav(
                            viewModel = viewModel,
                            currentIntent = currentIntent
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
    }
}

@Composable
fun MainAppNav(
    viewModel: NotesTasksViewModel,
    currentIntent: Intent?
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val context = LocalContext.current

    // Show toast for link saving or sync notifications
    LaunchedEffect(Unit) {
        viewModel.shareToastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // System Back gesture & back button navigation for Settings
    // (NoteEditScreen and TaskEditScreen manage their own auto-saving BackHandler)
    BackHandler(enabled = currentScreen is Screen.Settings) {
        currentScreen = Screen.Home
    }

    // Check & request notification permissions for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(350)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Automatic Google Drive sync on app open if user enabled the toggle
        viewModel.checkAndPerformDriveAutoSync()
    }

    // Handle incoming Intents (Quick-add from widget, and ACTION_SEND from external apps)
    LaunchedEffect(currentIntent) {
        val intent = currentIntent ?: return@LaunchedEffect

        // 1. Quick-add from widget
        if (intent.getBooleanExtra("ACTION_QUICK_ADD", false)) {
            currentScreen = Screen.TaskEdit(null)
        }

        // 2. Classical Android share intent (Share link to our app)
        if (intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                ?: ""
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            if (sharedText.isNotBlank()) {
                viewModel.handleIncomingShare(sharedText, subject) {
                    currentScreen = Screen.Home
                }
            }
        }
    }

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(durationMillis = 120),
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            is Screen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenNote = { note -> currentScreen = Screen.NoteEdit(note) },
                    onOpenTask = { task -> currentScreen = Screen.TaskEdit(task) },
                    onOpenSettings = { currentScreen = Screen.Settings }
                )
            }

            is Screen.NoteEdit -> {
                val allFolders by viewModel.allFolders.collectAsState()
                val selectedFolder by viewModel.selectedFolder.collectAsState()
                NoteEditScreen(
                    initialNote = screen.note,
                    allFolders = allFolders,
                    defaultFolderId = selectedFolder?.id,
                    viewModel = viewModel,
                    onSave = { title, content, tags, imageUris, colorIndex, isStarred, folderIds ->
                        viewModel.saveNoteWithFolders(
                            id = screen.note?.id ?: 0L,
                            title = title,
                            content = content,
                            tags = tags,
                            imageUris = imageUris,
                            colorIndex = colorIndex,
                            isStarred = isStarred,
                            folderIds = folderIds
                        )
                        currentScreen = Screen.Home
                    },
                    onDismiss = { currentScreen = Screen.Home }
                )
            }

            is Screen.TaskEdit -> {
                val allFolders by viewModel.allFolders.collectAsState()
                val selectedFolder by viewModel.selectedFolder.collectAsState()
                TaskEditScreen(
                    initialTaskWithSubtasks = screen.taskWithSubtasks,
                    allFolders = allFolders,
                    defaultFolderId = selectedFolder?.id,
                    viewModel = viewModel,
                    onSave = { title, desc, priority, dueDate, reminderTime, reminderEnabled, isStarred, subtasks, folderIds ->
                        viewModel.saveTaskWithFolders(
                            id = screen.taskWithSubtasks?.task?.id ?: 0L,
                            title = title,
                            description = desc,
                            priority = priority,
                            dueDate = dueDate,
                            reminderTime = reminderTime,
                            reminderEnabled = reminderEnabled,
                            isStarred = isStarred,
                            subtasks = subtasks,
                            folderIds = folderIds
                        )
                        currentScreen = Screen.Home
                    },
                    onDismiss = { currentScreen = Screen.Home }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
