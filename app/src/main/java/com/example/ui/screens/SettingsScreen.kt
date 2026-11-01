package com.example.ui.screens

import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wallpaper
import com.example.NotesTasksApp
import com.example.data.sync.DriveConnectionState
import com.example.data.sync.DriveSyncState
import com.example.data.sync.GoogleDriveSyncManager
import com.example.data.sync.GoogleSignInOutcome
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.appBackground
import com.example.ui.viewmodel.NotesTasksViewModel
import com.example.widget.WidgetTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesTasksViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val darkModeSetting by viewModel.darkModeSetting.collectAsState()
    val themePreset by viewModel.themePreset.collectAsState()
    val backgroundPattern by viewModel.backgroundPattern.collectAsState()
    val performanceMode by viewModel.performanceMode.collectAsState()

    // Google Drive Sync State
    val driveConnectionState by viewModel.driveConnectionState.collectAsState()
    val driveSyncState by viewModel.driveSyncState.collectAsState()
    val driveAutoSyncOnOpen by viewModel.driveAutoSyncOnOpen.collectAsState()
    val driveLastSyncTime by viewModel.driveLastSyncTime.collectAsState()
    val driveLastSyncSummary by viewModel.driveLastSyncSummary.collectAsState()

    // Developer Error 10 Diagnostic Dialog
    var showDeveloperError10Dialog by remember { mutableStateOf(false) }
    var developerError10Data by remember { mutableStateOf<GoogleSignInOutcome.DeveloperError10?>(null) }
    var consentPendingEmail by remember { mutableStateOf<String?>(null) }

    // Manual Token Input Dialog
    var showManualTokenDialog by remember { mutableStateOf(false) }
    var manualEmailInput by remember { mutableStateOf("") }
    var manualTokenInput by remember { mutableStateOf("") }
    var isVerifyingToken by remember { mutableStateOf(false) }

    // Client ID Edit Dialog
    var showEditClientIdDialog by remember { mutableStateOf(false) }
    var customClientIdInput by remember { mutableStateOf(viewModel.getActiveClientId()) }

    // Restore from Google Drive confirmation dialog
    var showRestoreDriveConfirmDialog by remember { mutableStateOf(false) }

    // Password Protection for Client ID
    var showPasswordAuthDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var pendingProtectedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Custom Deletion Dialog & Checkboxes
    var showCustomDeletionDialog by remember { mutableStateOf(false) }
    var deleteEntireAppDataSelected by remember { mutableStateOf(false) }
    var deleteNotesSelected by remember { mutableStateOf(false) }
    var deleteNotesUnstarredOnly by remember { mutableStateOf(false) }
    var deleteTasksSelected by remember { mutableStateOf(false) }
    var deleteTasksCompletedOnly by remember { mutableStateOf(false) }
    var deleteCustomFoldersSelected by remember { mutableStateOf(false) }
    var deleteCustomTagsSelected by remember { mutableStateOf(false) }
    var showConfirmDeleteWarningDialog by remember { mutableStateOf(false) }

    // Consent Launcher for UserRecoverableAuthException
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val email = consentPendingEmail ?: viewModel.getDriveAccountEmail() ?: "user"
        coroutineScope.launch {
            val res = viewModel.fetchDriveTokenAfterConsent(email)
            res.onSuccess {
                Toast.makeText(context, "Connected to Google Drive as $email ✓", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Drive authorization pending: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            when (val outcome = viewModel.handleGoogleSignInResult(result.data)) {
                is GoogleSignInOutcome.Success -> {
                    Toast.makeText(context, "Connected to Google Drive as ${outcome.email} ✓", Toast.LENGTH_SHORT).show()
                }
                is GoogleSignInOutcome.NeedsUserConsent -> {
                    consentPendingEmail = viewModel.getDriveAccountEmail()
                    consentLauncher.launch(outcome.intent)
                }
                is GoogleSignInOutcome.DeveloperError10 -> {
                    developerError10Data = outcome
                    showDeveloperError10Dialog = true
                }
                is GoogleSignInOutcome.Error -> {
                    Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // JSON file picker for Restore
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    val result = viewModel.importJsonBackup(jsonString)
                    result.onSuccess { count ->
                        Toast.makeText(context, "Successfully restored $count items from JSON! ✓", Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read backup file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ZIP file picker for Markdown Bundle Import
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        Toast.makeText(context, "Could not open selected ZIP file", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val result = viewModel.importMarkdownZipBundle(inputStream)
                    result.onSuccess { summary ->
                        Toast.makeText(
                            context,
                            "Imported: ${summary.notesCount} notes, ${summary.tasksCount} tasks, ${summary.foldersCount} folders, ${summary.subtasksCount} subtasks! ✓",
                            Toast.LENGTH_LONG
                        ).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Zip import failed: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read ZIP bundle: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Android back button & gesture navigation
    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings & Data Hub",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(start = 10.dp, end = 4.dp)
                            .size(42.dp)
                            .testTag("settings_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Appearance / Dark Mode / Themes / Backgrounds
            SettingsSection(title = "Appearance & Theming", icon = Icons.Default.Palette) {
                Text(
                    text = "Theme Preference",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val themes = listOf(
                            Triple(0, "System Default", Icons.Default.Palette),
                            Triple(1, "Light Mode", Icons.Default.LightMode),
                            Triple(2, "Dark Mode", Icons.Default.DarkMode)
                        )
                        themes.forEach { (mode, label, modeIcon) ->
                            val isSelected = darkModeSetting == mode
                            Surface(
                                onClick = { viewModel.setDarkMode(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Color Palette",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose accent and background tone harmonies",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Theme Presets Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeManager.Presets.forEach { preset ->
                        val isSelected = themePreset == preset.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .clickable { viewModel.setThemePreset(preset.id) },
                            shape = RoundedCornerShape(15.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(preset.primaryColor, CircleShape)
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.25f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "App Background Pattern",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "GPU-accelerated subtle textures for clean contrast",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Background Patterns List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeManager.Patterns.forEach { pattern ->
                        val isSelected = backgroundPattern == pattern.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .clickable { viewModel.setBackgroundPattern(pattern.id) },
                            shape = RoundedCornerShape(15.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wallpaper,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pattern.name,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = pattern.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Home Screen Widget Theme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose Pure White or Dark styling for your home screen widget (or add the dedicated 'Notes & Tasks (White)' widget directly from your launcher)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                var selectedWidgetTheme by remember {
                    mutableStateOf(WidgetTheme.getSelectedTheme(context))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetTheme.entries.forEach { wTheme ->
                        val isSelected = selectedWidgetTheme == wTheme
                        val previewBg = remember(wTheme.previewColorHex) {
                            Color(android.graphics.Color.parseColor(wTheme.previewColorHex))
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .clickable {
                                    selectedWidgetTheme = wTheme
                                    WidgetTheme.setSelectedTheme(context, wTheme)
                                    Toast.makeText(
                                        context,
                                        "Home screen widget updated to ${wTheme.displayName} ✓",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("widget_theme_${wTheme.key.lowercase()}"),
                            shape = RoundedCornerShape(15.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(previewBg, RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            if (wTheme.isLight) Color(0xFFCBD5E1) else Color(0xFF334155),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (wTheme.isLight) Color(0xFF4F46E5) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = wTheme.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = wTheme.subtitle,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Smooth Performance Note
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF16A34A).copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "High Refresh Rate Smoothness (60 / 90 / 120 FPS)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Engineered with native GPU drawBehind canvas rendering and memoized list structures for buttery, stutter-free performance.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Performance & Organization Mode (Tailored for 10,000+ notes & tasks)
            SettingsSection(title = "Performance Mode (Organization Scale)", icon = Icons.Default.Speed) {
                Text(
                    text = "Engineered for organizations managing 10s of thousands of notes and tasks without stutter or UI latency.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.setPerformanceMode(!performanceMode) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (performanceMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                    border = BorderStroke(
                        width = if (performanceMode) 1.5.dp else 1.dp,
                        color = if (performanceMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "High-Performance Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (performanceMode) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "ENABLED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Removes heavy visual card animations, simplifies preview rendering, disables regex computations on scroll, and optimizes Lazy recycling for huge organizational catalogs.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = performanceMode,
                            onCheckedChange = { viewModel.setPerformanceMode(it) },
                            modifier = Modifier.testTag("performance_mode_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // 3. Google Drive Cloud Sync
            SettingsSection(title = "Google Drive Cloud Sync", icon = Icons.Default.CloudSync) {
                Text(
                    text = "Sync your notes and tasks to Google Drive with automated cloud backup and storage-efficient overwrite.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Connection Card
                when (val conn = driveConnectionState) {
                    is DriveConnectionState.Connected -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFDCFCE7), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Connected & Ready",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A)
                                            )
                                            Text(
                                                text = conn.email,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.disconnectGoogleDrive() },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f),
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Auto-Sync on App Open Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Sync on App Launch",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Automatically sync to Google Drive as soon as user opens app",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = driveAutoSyncOnOpen,
                                        onCheckedChange = { viewModel.setDriveAutoSyncOnOpen(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("drive_auto_sync_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Sync and Restore Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val isSyncing = driveSyncState is DriveSyncState.Syncing

                                    Button(
                                        onClick = {
                                            viewModel.syncToGoogleDrive { result ->
                                                result.onSuccess {
                                                    Toast.makeText(context, "Google Drive sync complete! ✓", Toast.LENGTH_SHORT).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "Sync error: ${err.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        enabled = !isSyncing,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("sync_to_drive_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync to Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showRestoreDriveConfirmDialog = true },
                                        enabled = !isSyncing,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("restore_from_drive_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Sync status message
                                Spacer(modifier = Modifier.height(10.dp))
                                val statusText = when (val state = driveSyncState) {
                                    is DriveSyncState.Syncing -> state.message
                                    is DriveSyncState.Success -> state.message
                                    is DriveSyncState.Error -> "Error: ${state.message}"
                                    is DriveSyncState.Idle -> {
                                        if (driveLastSyncTime > 0) {
                                            "Last synced: ${viewModel.googleDriveSyncManager.formatTimestamp(driveLastSyncTime)}"
                                        } else {
                                            "Never synced to Google Drive yet"
                                        }
                                    }
                                }

                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    color = if (driveSyncState is DriveSyncState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is DriveConnectionState.Connecting -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Connecting to Google Drive...", fontSize = 13.sp)
                            }
                        }
                    }

                    is DriveConnectionState.Disconnected -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Connect with OAuth 2.0 Client ID",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (viewModel.getActiveClientId() != com.example.data.sync.GoogleDriveSyncManager.CLIENT_ID) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.resetActiveClientId()
                                                    customClientIdInput = com.example.data.sync.GoogleDriveSyncManager.CLIENT_ID
                                                    Toast.makeText(context, "Reset to default Client ID", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("Reset", fontSize = 11.sp)
                                            }
                                        }
                                        TextButton(
                                            onClick = {
                                                customClientIdInput = viewModel.getActiveClientId()
                                                showEditClientIdDialog = true
                                            }
                                        ) {
                                            Text("Edit ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                val activeId = viewModel.getActiveClientId()
                                Text(
                                    text = "Client ID: ${if (activeId.length > 30) activeId.take(28) + "..." else activeId}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        try {
                                            val client = viewModel.getGoogleSignInClient()
                                            googleSignInLauncher.launch(client.signInIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Google Sign-In prompt failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            showManualTokenDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("google_drive_signin_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google Drive", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showManualTokenDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(13.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Manual Token", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            developerError10Data = GoogleSignInOutcome.DeveloperError10(
                                                packageName = context.packageName,
                                                sha1 = NotesTasksApp.getCertificateFingerprint(context, "SHA-1"),
                                                clientId = viewModel.getActiveClientId(),
                                                message = "Google Play Services DEVELOPER_ERROR (Status 10) resolution guide"
                                            )
                                            showDeveloperError10Dialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(13.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Error 10 Guide", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Storage Efficiency Explanation Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Storage Efficiency: Sync uses a single-file JSON overwrite pattern (${GoogleDriveSyncManager.BACKUP_FILENAME}) to keep Google Drive storage usage at minimum.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 3. Markdown Bundle Import & Export (Zipped Folder)
            SettingsSection(title = "Markdown Bundle (.zip) Export & Import", icon = Icons.Default.FolderZip) {
                Text(
                    text = "All notes and tasks are saved as their own individual .md files, organized cleanly inside their corresponding folders and packed into one .zip file.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export Markdown ZIP Bundle
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val zipFile = viewModel.exportMarkdownZipBundle(context)
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Notes & Tasks Markdown Backup")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export Markdown Bundle (.zip)"))
                                    Toast.makeText(context, "Markdown ZIP created: ${zipFile.name}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("export_markdown_zip_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export ZIP Bundle", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Import Markdown ZIP Bundle
                    OutlinedButton(
                        onClick = {
                            zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("import_markdown_zip_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import ZIP Bundle", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Individual notes & tasks are converted to standard .md files with YAML frontmatter\n• Preserves subtasks (- [ ] and - [x]), starred items, priority, reminders, and tags\n• Keeps exact folder hierarchies when exported and restored",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 4. Local JSON Backup & Restore
            SettingsSection(title = "Raw JSON Database Backup", icon = Icons.Default.Backup) {
                Text(
                    text = "Complete database snapshot in JSON format for quick offline device-to-device transfers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val json = viewModel.exportJsonBackup()
                                    val cacheFile = File(context.cacheDir, "notes_tasks_backup_${System.currentTimeMillis()}.json")
                                    FileOutputStream(cacheFile).use { it.write(json.toByteArray()) }

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export JSON Backup"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { jsonPickerLauncher.launch("application/json") },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 5. System Permissions Manager
            SettingsSection(title = "System Permissions Manager", icon = Icons.Default.Security) {
                Text(
                    text = "Ensure background alarms and full-screen reminders pop up reliably.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Notifications
                val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                PermissionRow(
                    title = "Notifications",
                    subtitle = "Required for task reminder banners and heads-up alerts",
                    isGranted = notificationsEnabled,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Exact Alarms
                val canExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    am?.canScheduleExactAlarms() ?: true
                } else true

                PermissionRow(
                    title = "Exact Alarms",
                    subtitle = "Required for pinpoint alarm delivery at the exact minute",
                    isGranted = canExactAlarm,
                    onOpenSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // 6. Developer & GCP Identifiers
            SettingsSection(title = "GCP & OAuth 2.0 Credentials", icon = Icons.Default.Key) {
                Text(
                    text = "Google Cloud Console OAuth 2.0 configuration identifiers required to authorize this app:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                val activeClientId = viewModel.getActiveClientId()
                CredentialCopyBox(
                    label = "Client ID",
                    value = activeClientId,
                    isProtected = true,
                    onProtectedCopyRequest = {
                        pendingProtectedAction = {
                            copyToClipboard(context, "Client ID", activeClientId)
                        }
                        passwordInput = ""
                        passwordError = null
                        showPasswordAuthDialog = true
                    },
                    onCopy = {
                        copyToClipboard(context, "Client ID", activeClientId)
                    },
                    onEdit = {
                        pendingProtectedAction = {
                            customClientIdInput = activeClientId
                            showEditClientIdDialog = true
                        }
                        passwordInput = ""
                        passwordError = null
                        showPasswordAuthDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CredentialCopyBox(
                    label = "Package Name",
                    value = context.packageName,
                    onCopy = {
                        copyToClipboard(context, "Package Name", context.packageName)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val sha1 = remember { NotesTasksApp.getCertificateFingerprint(context, "SHA-1") }
                CredentialCopyBox(
                    label = "SHA-1 Fingerprint",
                    value = sha1,
                    onCopy = {
                        copyToClipboard(context, "SHA-1 Fingerprint", sha1)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.cloud.google.com/apis/credentials"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open GCP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            developerError10Data = GoogleSignInOutcome.DeveloperError10(
                                packageName = context.packageName,
                                sha1 = sha1,
                                clientId = activeClientId,
                                message = "Resolution guide for DEVELOPER_ERROR 10"
                            )
                            showDeveloperError10Dialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Error 10 Guide", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 7. Custom Data Deletion & Storage Management
            SettingsSection(title = "Data Reset & Deletion Menu", icon = Icons.Default.DeleteForever) {
                Text(
                    text = "Selectively remove notes, tasks, custom tags, folders, or completely wipe app data.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    onClick = {
                        deleteEntireAppDataSelected = false
                        deleteNotesSelected = false
                        deleteNotesUnstarredOnly = false
                        deleteTasksSelected = false
                        deleteTasksCompletedOnly = false
                        deleteCustomFoldersSelected = false
                        deleteCustomTagsSelected = false
                        showCustomDeletionDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Open Custom Deletion Menu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Developer Error 10 Diagnostic & Resolution Dialog
    if (showDeveloperError10Dialog) {
        val pkgName = developerError10Data?.packageName ?: context.packageName
        val sha1Fingerprint = developerError10Data?.sha1 ?: NotesTasksApp.getCertificateFingerprint(context, "SHA-1")
        val currentClientId = developerError10Data?.clientId ?: viewModel.getActiveClientId()

        AlertDialog(
            onDismissRequest = { showDeveloperError10Dialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Sign-In Error 10 Fix Guide",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Google Play Services reported DEVELOPER_ERROR (Status 10).\n\n" +
                                "This occurs because the Google Cloud Console credentials must have an 'Android' OAuth 2.0 client matching this app's package name and SHA-1 certificate.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Required Credentials (Tap to copy):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            CredentialCopyBox(
                                label = "Package Name",
                                value = pkgName,
                                onCopy = { copyToClipboard(context, "Package Name", pkgName) }
                            )

                            CredentialCopyBox(
                                label = "SHA-1 Fingerprint",
                                value = sha1Fingerprint,
                                onCopy = { copyToClipboard(context, "SHA-1 Fingerprint", sha1Fingerprint) }
                            )

                            CredentialCopyBox(
                                label = "Client ID",
                                value = currentClientId,
                                onCopy = { copyToClipboard(context, "Client ID", currentClientId) },
                                onEdit = {
                                    customClientIdInput = currentClientId
                                    showEditClientIdDialog = true
                                }
                            )
                        }
                    }

                    Text(
                        text = "Step-by-step checklist to resolve:\n" +
                                "1. Android Client ID: Verified matched! (Package: $pkgName, SHA-1: $sha1Fingerprint)\n" +
                                "2. Propagation delay: Google Cloud Console takes 5 to 15 minutes for new client IDs to take effect on Google's auth servers.\n" +
                                "3. Clear device cache: Go to Android Settings → Apps → Google Play Services → Storage & cache → Clear Cache (removes stale failure responses).\n" +
                                "4. Test users: In Google Cloud Console under 'Google Auth Platform' → 'Audience', add your Google email under Test Users.\n" +
                                "5. Note on API Keys: API Keys only identify projects for quota; private Google Drive sync requires user OAuth tokens.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.cloud.google.com/apis/credentials"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Google Cloud Console", fontSize = 12.sp)
                    }

                    Text(
                        text = "Fast alternative: You can also generate a temporary access token from Google OAuth Playground and paste it via 'Enter Token Manually' to test sync right away!",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeveloperError10Dialog = false
                        showManualTokenDialog = true
                    }
                ) {
                    Text("Enter Token Manually")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeveloperError10Dialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Manual Access Token Dialog
    if (showManualTokenDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isVerifyingToken) showManualTokenDialog = false
            },
            title = { Text("Connect Google Drive Token") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "You can quickly obtain a test OAuth Bearer token from Google OAuth 2.0 Playground to sync with Google Drive immediately:",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developers.google.com/oauthplayground"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open OAuth 2.0 Playground", fontSize = 12.sp)
                    }

                    Text(
                        text = "Quick 4-step token generation:\n" +
                                "1. In Step 1, select 'Drive API v3' → check 'https://www.googleapis.com/auth/drive.file'\n" +
                                "2. Click 'Authorize APIs' and sign in with your Google account\n" +
                                "3. In Step 2, click 'Exchange authorization code for tokens'\n" +
                                "4. Copy the 'Access token' (starts with ya29...) and paste below:",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualTokenInput,
                        onValueChange = { manualTokenInput = it },
                        label = { Text("OAuth Access Token (Bearer)") },
                        placeholder = { Text("ya29.a0A...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = manualEmailInput,
                        onValueChange = { manualEmailInput = it },
                        label = { Text("Account Email (optional)") },
                        placeholder = { Text("user@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isVerifyingToken) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying token with Google Drive API...", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        enabled = manualTokenInput.isNotBlank() && !isVerifyingToken,
                        onClick = {
                            val token = manualTokenInput.trim()
                            viewModel.connectGoogleDriveManually(manualEmailInput, token)
                            showManualTokenDialog = false
                            Toast.makeText(context, "Google Drive token saved!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Save Directly", fontSize = 12.sp)
                    }

                    Button(
                        enabled = manualTokenInput.isNotBlank() && !isVerifyingToken,
                        onClick = {
                            val token = manualTokenInput.trim()
                            isVerifyingToken = true
                            coroutineScope.launch {
                                val verifyResult = viewModel.verifyAndConnectDriveToken(token)
                                isVerifyingToken = false
                                verifyResult.onSuccess { email ->
                                    manualEmailInput = email
                                    showManualTokenDialog = false
                                    Toast.makeText(context, "Connected to Google Drive as $email ✓", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, "Verification note: ${err.message}", Toast.LENGTH_LONG).show()
                                    viewModel.connectGoogleDriveManually(manualEmailInput, token)
                                    showManualTokenDialog = false
                                }
                            }
                        }
                    ) {
                        Text(if (isVerifyingToken) "Verifying..." else "Verify & Connect", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isVerifyingToken,
                    onClick = { showManualTokenDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Client ID Dialog
    if (showEditClientIdDialog) {
        AlertDialog(
            onDismissRequest = { showEditClientIdDialog = false },
            title = { Text("Edit OAuth 2.0 Client ID") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify the OAuth 2.0 Client ID for Google Sign-In:",
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = customClientIdInput,
                        onValueChange = { customClientIdInput = it },
                        label = { Text("Client ID") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    TextButton(
                        onClick = {
                            customClientIdInput = GoogleDriveSyncManager.CLIENT_ID
                        }
                    ) {
                        Text("Reset to Default Client ID", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setActiveClientId(customClientIdInput.trim())
                        showEditClientIdDialog = false
                        Toast.makeText(context, "Client ID updated!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditClientIdDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore from Google Drive confirmation dialog
    if (showRestoreDriveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDriveConfirmDialog = false },
            title = { Text("Restore from Google Drive?") },
            text = {
                Text(
                    "This will download the latest backup from Google Drive and merge or update notes, tasks, and folders in your app database.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDriveConfirmDialog = false
                        viewModel.restoreFromGoogleDrive { result ->
                            result.onSuccess { count ->
                                Toast.makeText(context, "Successfully restored $count items from Google Drive! ✓", Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDriveConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Password Protection Dialog for Client ID
    if (showPasswordAuthDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordAuthDialog = false
                pendingProtectedAction = null
                passwordInput = ""
                passwordError = null
            },
            icon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text("Password Required", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This credential is encrypted/protected. Enter the administrator password to view, copy, or edit your Client ID.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("Enter Password") },
                        isError = passwordError != null,
                        supportingText = {
                            if (passwordError != null) {
                                Text(passwordError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput == "amey44") {
                            showPasswordAuthDialog = false
                            val action = pendingProtectedAction
                            pendingProtectedAction = null
                            passwordInput = ""
                            passwordError = null
                            action?.invoke()
                        } else {
                            passwordError = "Incorrect password. Access denied."
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordAuthDialog = false
                        pendingProtectedAction = null
                        passwordInput = ""
                        passwordError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Custom Deletion Selection Menu Dialog
    if (showCustomDeletionDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDeletionDialog = false },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = {
                Text("Custom Data Deletion", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Choose precisely what data categories to wipe. You can select individual subsets or check 'Wipe Entire App Data'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option: Entire App Data
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (deleteEntireAppDataSelected) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            1.dp,
                            if (deleteEntireAppDataSelected) MaterialTheme.colorScheme.error else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                deleteEntireAppDataSelected = !deleteEntireAppDataSelected
                                if (deleteEntireAppDataSelected) {
                                    deleteNotesSelected = false
                                    deleteNotesUnstarredOnly = false
                                    deleteTasksSelected = false
                                    deleteTasksCompletedOnly = false
                                    deleteCustomFoldersSelected = false
                                    deleteCustomTagsSelected = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = deleteEntireAppDataSelected,
                                onCheckedChange = { checked ->
                                    deleteEntireAppDataSelected = checked
                                    if (checked) {
                                        deleteNotesSelected = false
                                        deleteNotesUnstarredOnly = false
                                        deleteTasksSelected = false
                                        deleteTasksCompletedOnly = false
                                        deleteCustomFoldersSelected = false
                                        deleteCustomTagsSelected = false
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    "Wipe Entire App Data",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Completely erases all notes, tasks, subtasks, folders, tags, and resets app to pristine state.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!deleteEntireAppDataSelected) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Selective Item Deletion:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        // 1. Delete Notes
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteNotesSelected = !deleteNotesSelected }
                        ) {
                            Checkbox(
                                checked = deleteNotesSelected,
                                onCheckedChange = { deleteNotesSelected = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete Notes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Removes all notes from local database", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (!deleteNotesSelected) {
                            // Sub-option: Unstarred Notes only
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp)
                                    .clickable { deleteNotesUnstarredOnly = !deleteNotesUnstarredOnly }
                            ) {
                                Checkbox(
                                    checked = deleteNotesUnstarredOnly,
                                    onCheckedChange = { deleteNotesUnstarredOnly = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Unstarred notes only", fontSize = 12.sp)
                                    Text("Keep your starred/favorited notes safe", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 2. Delete Tasks
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteTasksSelected = !deleteTasksSelected }
                        ) {
                            Checkbox(
                                checked = deleteTasksSelected,
                                onCheckedChange = { deleteTasksSelected = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete Tasks", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Removes all tasks and checklists", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (!deleteTasksSelected) {
                            // Sub-option: Completed Tasks only
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp)
                                    .clickable { deleteTasksCompletedOnly = !deleteTasksCompletedOnly }
                            ) {
                                Checkbox(
                                    checked = deleteTasksCompletedOnly,
                                    onCheckedChange = { deleteTasksCompletedOnly = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Completed tasks only", fontSize = 12.sp)
                                    Text("Cleans up finished to-dos and archives", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 3. Delete Custom Folders
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteCustomFoldersSelected = !deleteCustomFoldersSelected }
                        ) {
                            Checkbox(
                                checked = deleteCustomFoldersSelected,
                                onCheckedChange = { deleteCustomFoldersSelected = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete Custom Folders", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Removes created folders (items moved to root)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // 4. Delete Custom Tags
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteCustomTagsSelected = !deleteCustomTagsSelected }
                        ) {
                            Checkbox(
                                checked = deleteCustomTagsSelected,
                                onCheckedChange = { deleteCustomTagsSelected = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete Custom Tags", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Resets custom tags list to default", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val hasAnySelection = deleteEntireAppDataSelected ||
                        deleteNotesSelected || deleteNotesUnstarredOnly ||
                        deleteTasksSelected || deleteTasksCompletedOnly ||
                        deleteCustomFoldersSelected || deleteCustomTagsSelected

                Button(
                    enabled = hasAnySelection,
                    onClick = {
                        showConfirmDeleteWarningDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Proceed to Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDeletionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Final Confirmation Dialog for Deletion
    if (showConfirmDeleteWarningDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteWarningDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = {
                Text(
                    if (deleteEntireAppDataSelected) "Wipe Entire App Data?" else "Confirm Selected Deletion?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (deleteEntireAppDataSelected) {
                        "Are you absolutely sure? This will permanently delete all notes, tasks, subtasks, folders, and tags. This operation cannot be undone."
                    } else {
                        "Are you sure you want to delete the selected data categories? Selected items will be permanently erased from your local database."
                    },
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteWarningDialog = false
                        showCustomDeletionDialog = false

                        if (deleteEntireAppDataSelected) {
                            viewModel.clearEntireAppData {
                                Toast.makeText(context, "All app data has been wiped successfully.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            if (deleteNotesSelected || deleteNotesUnstarredOnly) {
                                viewModel.deleteNotesCustom(deleteNotesSelected, deleteNotesUnstarredOnly)
                            }
                            if (deleteTasksSelected || deleteTasksCompletedOnly) {
                                viewModel.deleteTasksCustom(deleteTasksSelected, deleteTasksCompletedOnly)
                            }
                            if (deleteCustomFoldersSelected) {
                                viewModel.deleteCustomFolders()
                            }
                            if (deleteCustomTagsSelected) {
                                viewModel.deleteCustomTags()
                            }
                            Toast.makeText(context, "Selected data deleted successfully.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Delete Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Granted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CredentialCopyBox(
    label: String,
    value: String,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)? = null,
    isProtected: Boolean = false,
    onProtectedCopyRequest: (() -> Unit)? = null
) {
    var isRevealed by remember { mutableStateOf(!isProtected) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isProtected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Protected",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val displayValue = if (isProtected && !isRevealed) {
                    if (value.length > 12) {
                        "${value.take(6)}••••••••••••••••••••••••${value.takeLast(6)}"
                    } else "••••••••••••••••"
                } else value

                Text(
                    text = displayValue,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isProtected && !isRevealed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isProtected) {
                    Surface(
                        onClick = {
                            if (isRevealed) {
                                isRevealed = false
                            } else {
                                onProtectedCopyRequest?.invoke()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isRevealed) "Hide $label" else "Show/Unlock $label",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                if (onEdit != null) {
                    Surface(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Edit $label",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Surface(
                    onClick = {
                        if (isProtected && !isRevealed) {
                            onProtectedCopyRequest?.invoke()
                        } else {
                            onCopy()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isProtected && !isRevealed) Icons.Default.Lock else Icons.Default.ContentCopy,
                            contentDescription = "Copy $label",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}
