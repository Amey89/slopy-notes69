package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskWithSubtasks
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.FolderColors
import com.example.ui.components.MultiFolderSelector
import com.example.ui.components.NestedSubtaskEditor
import com.example.ui.viewmodel.NotesTasksViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    initialTaskWithSubtasks: TaskWithSubtasks?,
    allFolders: List<FolderEntity> = emptyList(),
    defaultFolderId: Long? = null,
    viewModel: NotesTasksViewModel? = null,
    onSave: (
        title: String,
        desc: String,
        priority: Int,
        dueDate: Long?,
        reminderTime: Long?,
        reminderEnabled: Boolean,
        isStarred: Boolean,
        subtasks: List<SubTaskEntity>,
        folderIds: List<Long>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val task = initialTaskWithSubtasks?.task

    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: 1) } // 0: Low, 1: Medium, 2: High
    var isStarred by remember { mutableStateOf(task?.isStarred ?: false) }

    var selectedFolderIds by remember {
        mutableStateOf(
            if (task?.folderId != null) setOf(task.folderId)
            else if (defaultFolderId != null) setOf(defaultFolderId)
            else emptySet()
        )
    }

    LaunchedEffect(task?.id) {
        if (task != null && viewModel != null) {
            val crossRefIds = viewModel.getFolderIdsForTask(task.id)
            if (crossRefIds.isNotEmpty()) {
                selectedFolderIds = (selectedFolderIds + crossRefIds).toSet()
            }
        }
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val taskFolders = remember(allFolders) {
        allFolders.filter { it.folderType == "TASK" || it.folderType == "ALL" }
    }

    var reminderEnabled by remember { mutableStateOf(task?.reminderEnabled ?: false) }
    var reminderTime by remember {
        mutableStateOf(task?.reminderTime ?: (System.currentTimeMillis() + 60 * 60 * 1000L))
    }

    var subtasks by remember {
        mutableStateOf(initialTaskWithSubtasks?.subtasks ?: emptyList())
    }

    val dateTimeFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }

    // Auto-save logic on back gesture, back button, or save button
    val performAutoSaveAndExit = {
        val trimmedTitle = title.trim()
        val trimmedDesc = description.trim()
        val hasContent = trimmedTitle.isNotBlank() || trimmedDesc.isNotBlank() || subtasks.isNotEmpty()

        if (hasContent || initialTaskWithSubtasks != null) {
            val finalTitle = if (trimmedTitle.isNotBlank()) {
                trimmedTitle
            } else if (trimmedDesc.isNotBlank()) {
                trimmedDesc.take(40)
            } else if (subtasks.isNotEmpty()) {
                subtasks.firstOrNull { it.title.isNotBlank() }?.title ?: "Untitled Task"
            } else {
                "Untitled Task"
            }

            onSave(
                finalTitle,
                trimmedDesc,
                priority,
                if (reminderEnabled) reminderTime else null,
                if (reminderEnabled) reminderTime else null,
                reminderEnabled,
                isStarred,
                subtasks,
                selectedFolderIds.toList()
            )
        } else {
            onDismiss()
        }
    }

    // Android back button & gesture support
    BackHandler {
        performAutoSaveAndExit()
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            parentFolder = null,
            defaultFolderType = "TASK",
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name, folderType, colorIdx ->
                showCreateFolderDialog = false
                viewModel?.createFolder(name = name, folderType = folderType, colorIndex = colorIdx)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialTaskWithSubtasks == null) "New Task" else "Edit Task",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = performAutoSaveAndExit,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isStarred = !isStarred },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star task",
                            tint = if (isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Button(
                        onClick = performAutoSaveAndExit,
                        modifier = Modifier
                            .height(44.dp)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = title.isNotBlank() || description.isNotBlank() || subtasks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val titleFocusRequester = remember { FocusRequester() }
            val descFocusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(Unit) {
                delay(100)
                try {
                    titleFocusRequester.requestFocus()
                    keyboardController?.show()
                } catch (_: Exception) {
                }
            }

            // Task Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What needs to be done?", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        try {
                            descFocusRequester.requestFocus()
                            keyboardController?.show()
                        } catch (_: Exception) {
                        }
                    }
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester)
            )

            // Multi-Folder Assignment
            MultiFolderSelector(
                title = "Assign Task to Folders",
                availableFolders = taskFolders,
                selectedFolderIds = selectedFolderIds,
                onToggleFolder = { folderId ->
                    selectedFolderIds = if (selectedFolderIds.contains(folderId)) {
                        selectedFolderIds - folderId
                    } else {
                        selectedFolderIds + folderId
                    }
                },
                onAddNewFolder = { showCreateFolderDialog = true }
            )

            // Task Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Add details or notes for this task...", fontSize = 14.sp) },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descFocusRequester)
            )

            // Priority Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Priority Level",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val priorities = remember {
                        listOf(
                            Triple(0, "Low", Color(0xFF3B82F6)),
                            Triple(1, "Medium", Color(0xFFD97706)),
                            Triple(2, "High", Color(0xFFDC2626))
                        )
                    }
                    priorities.forEach { (pCode, pLabel, pColor) ->
                        val isSelected = priority == pCode
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = pCode },
                            label = { Text(pLabel, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pColor.copy(alpha = 0.2f),
                                selectedLabelColor = pColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Full Screen Reminder Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (reminderEnabled) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (reminderEnabled) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Full-Screen Reminder",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pop up full screen on lockscreen or other apps",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    if (reminderEnabled) {
                        // Formatted Reminder Time
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateTimeFormatter.format(Date(reminderTime)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Quick Presets
                        Text(
                            text = "Quick Presets:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val now = System.currentTimeMillis()
                            QuickTimeChip(label = "+10 mins") {
                                reminderTime = now + 10 * 60 * 1000L
                            }
                            QuickTimeChip(label = "+30 mins") {
                                reminderTime = now + 30 * 60 * 1000L
                            }
                            QuickTimeChip(label = "+1 hour") {
                                reminderTime = now + 60 * 60 * 1000L
                            }
                            QuickTimeChip(label = "Tomorrow 9 AM") {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                }
                                reminderTime = cal.timeInMillis
                            }
                        }

                        // Custom Date & Time Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = reminderTime }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(Calendar.YEAR, y)
                                            cal.set(Calendar.MONTH, m)
                                            cal.set(Calendar.DAY_OF_MONTH, d)
                                            reminderTime = cal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick Date", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = reminderTime }
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            cal.set(Calendar.MINUTE, minute)
                                            reminderTime = cal.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick Time", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Nested Subtasks Manager
            NestedSubtaskEditor(
                subtasks = subtasks,
                onAddSubtask = { subtaskTitle ->
                    val newSubtask = SubTaskEntity(
                        parentTaskId = task?.id ?: 0L,
                        title = subtaskTitle,
                        isCompleted = false,
                        orderIndex = subtasks.size
                    )
                    subtasks = subtasks + newSubtask
                },
                onToggleSubtask = { targetSubtask, isDone ->
                    subtasks = subtasks.map {
                        if (it == targetSubtask) it.copy(isCompleted = isDone) else it
                    }
                },
                onDeleteSubtask = { targetSubtask ->
                    subtasks = subtasks.filter { it != targetSubtask }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun QuickTimeChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}
