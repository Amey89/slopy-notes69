package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.TaskWithSubtasks
import com.example.ui.components.AddLinkModalDialog
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.CreateTagDialog
import com.example.ui.components.FolderBreadcrumbBar
import com.example.ui.components.FolderColors
import com.example.ui.components.NoteCard
import com.example.ui.components.ObsidianVaultSidePanel
import com.example.ui.components.RenameFolderDialog
import com.example.ui.components.SearchFilter
import com.example.ui.components.TaskCard
import com.example.ui.components.UniversalSearchBar
import com.example.ui.model.NoteUiItem
import com.example.ui.model.TaskUiItem
import com.example.ui.theme.appBackground
import com.example.ui.viewmodel.NotesTasksViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NotesTasksViewModel,
    onOpenNote: (NoteEntity?) -> Unit,
    onOpenTask: (TaskWithSubtasks?) -> Unit,
    onOpenSettings: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()

    val notesList by viewModel.notesList.collectAsState()
    val tasksList by viewModel.tasksList.collectAsState()
    val noteUiItems by viewModel.noteUiItems.collectAsState()
    val taskUiItems by viewModel.taskUiItems.collectAsState()
    val isVaultLoaded by viewModel.isVaultLoaded.collectAsState()

    // Stable lambdas to prevent LazyColumn item recomposition churn
    val onOpenNoteStable: (NoteEntity) -> Unit = remember(onOpenNote) { { note -> onOpenNote(note) } }
    val onToggleNoteStarStable: (NoteEntity) -> Unit = remember(viewModel) { { note -> viewModel.toggleNoteStar(note) } }
    val onDeleteNoteStable: (Long) -> Unit = remember(viewModel) { { id -> viewModel.deleteNote(id) } }
    val onUpdateNoteContentStable: (NoteEntity, String) -> Unit = remember(viewModel) { { note, updated -> viewModel.updateNoteContent(note, updated) } }

    val onOpenTaskStable: (TaskWithSubtasks) -> Unit = remember(onOpenTask) { { task -> onOpenTask(task) } }
    val onToggleTaskCompletedStable: (Long, Boolean) -> Unit = remember(viewModel) { { id, isDone -> viewModel.toggleTaskCompleted(id, isDone) } }
    val onToggleTaskStarStable: (TaskWithSubtasks) -> Unit = remember(viewModel) { { task -> viewModel.toggleTaskStar(task) } }
    val onToggleSubtaskStable: (Long, Boolean) -> Unit = remember(viewModel) { { subtaskId, isDone -> viewModel.toggleSubtaskCompleted(subtaskId, isDone) } }
    val onDeleteTaskStable: (Long) -> Unit = remember(viewModel) { { id -> viewModel.deleteTask(id) } }

    val allNotes by viewModel.allNotes.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    val themePreset by viewModel.themePreset.collectAsState()
    val backgroundPattern by viewModel.backgroundPattern.collectAsState()

    val driveConnectionState by viewModel.driveConnectionState.collectAsState()
    val driveSyncState by viewModel.driveSyncState.collectAsState()
    
    val performanceMode by viewModel.performanceMode.collectAsState()

    // Memoized maps for high-performance fluid rendering without stutter (collected from ViewModel)
    val folderNotesCountMap by viewModel.folderNotesCountMap.collectAsState()
    val folderTasksCountMap by viewModel.folderTasksCountMap.collectAsState()
    val subfoldersMap by viewModel.subfoldersMap.collectAsState()

    var showActionSheet by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }

    // Folder Dialog States
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var createFolderParent by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToRename by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Android back gesture/button support for drawer, folders, and search
    BackHandler(enabled = drawerState.isOpen || selectedFolder != null || searchQuery.isNotBlank()) {
        when {
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            searchQuery.isNotBlank() -> viewModel.setSearchQuery("")
            selectedFolder != null -> viewModel.navigateUpFolder()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ObsidianVaultSidePanel(
                allFolders = allFolders,
                allNotes = allNotes,
                allTasks = allTasks,
                allTags = allTags,
                selectedFolder = selectedFolder,
                activeFilter = activeFilter,
                selectedTagFilter = selectedTagFilter,
                folderNotesCountMap = folderNotesCountMap,
                folderTasksCountMap = folderTasksCountMap,
                subfoldersMap = subfoldersMap,
                onSelectFolder = { folder -> viewModel.selectFolder(folder) },
                onSelectFilter = { filter -> viewModel.setActiveFilter(filter) },
                onSelectTag = { tag -> viewModel.setSelectedTagFilter(tag) },
                onCreateRootFolder = {
                    createFolderParent = null
                    showCreateFolderDialog = true
                },
                onCreateSubfolder = { parent ->
                    createFolderParent = parent
                    showCreateFolderDialog = true
                },
                onRenameFolder = { folder -> folderToRename = folder },
                onDeleteFolder = { folder -> folderToDelete = folder },
                onCreateNewNote = { onOpenNote(null) },
                onCreateNewTask = { onOpenTask(null) },
                onCreateNewTag = { showCreateTagDialog = true },
                onOpenNote = { note -> onOpenNote(note) },
                onOpenTask = { task -> onOpenTask(task) },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                },
                isDrawerOpen = drawerState.isOpen || drawerState.targetValue == DrawerValue.Open
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (backgroundPattern > 0) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        // Distraction-Free Interactive Vault / Folder Switcher Capsule Pill (single top-left button)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    coroutineScope.launch { drawerState.open() }
                                }
                                .testTag("topbar_vault_folder_switcher")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentFolder = selectedFolder
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            currentFolder == null -> Icons.Default.AccountTree
                                            currentFolder.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> Icons.Default.CheckCircle
                                            FolderEntity.isSharedLinksFamilyId(currentFolder.id) -> Icons.Default.Link
                                            else -> Icons.Default.FolderOpen
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        currentFolder != null -> currentFolder.name
                                        !selectedTagFilter.isNullOrBlank() -> "#$selectedTagFilter"
                                        activeFilter != SearchFilter.ALL -> activeFilter.label
                                        else -> "All Vault Items"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Switch folder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Surface(
                                onClick = { showAddLinkDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "Save Link with Metadata",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (driveConnectionState is com.example.data.sync.DriveConnectionState.Connected) {
                                Surface(
                                    onClick = onOpenSettings,
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val icon = when (driveSyncState) {
                                            is com.example.data.sync.DriveSyncState.Syncing -> Icons.Default.CloudSync
                                            else -> Icons.Default.CloudDone
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "Google Drive Sync",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = onOpenSettings,
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                // Material 3 Expressive Floating Action Toolbar + Squircle Primary FAB
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Quick Note Pill
                            Surface(
                                onClick = { onOpenNote(null) },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "New Note",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Note",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Quick Task Pill
                            Surface(
                                onClick = { onOpenTask(null) },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = "New Task",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Task",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Expressive Primary Squircle FAB
                    FloatingActionButton(
                        onClick = { showActionSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.size(62.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Item",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Pinned Universal Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    UniversalSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        activeFilter = activeFilter,
                        onFilterSelect = { viewModel.setActiveFilter(it) },
                        allFolders = allFolders,
                        allNotes = allNotes,
                        allTasks = allTasks,
                        allTags = allTags,
                        selectedTagFilter = selectedTagFilter,
                        onSelectTagFilter = { viewModel.setSelectedTagFilter(it) },
                        onOpenNote = { note -> onOpenNote(note) },
                        onOpenTask = { task -> onOpenTask(task) },
                        onSelectFolder = { folder -> viewModel.selectFolder(folder) },
                        performanceMode = performanceMode
                    )
                }

                val pendingTasksCount = remember(tasksList) { tasksList.count { !it.task.isCompleted } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Folder Breadcrumb Navigation (shown when navigating inside a folder)
                    if (selectedFolder != null) {
                        item(key = "breadcrumb_${selectedFolder?.id}", contentType = "breadcrumb") {
                            FolderBreadcrumbBar(
                                currentFolder = selectedFolder!!,
                                allFolders = allFolders,
                                onExitFolder = { viewModel.selectFolder(null) },
                                onSelectFolder = { viewModel.selectFolder(it) }
                            )
                        }
                    }

                    // 1b. Permanent Platform Subfolders Strip (when viewing Shared Links or any of its 14 app subfolders)
                    if (FolderEntity.isSharedLinksFamilyId(selectedFolder?.id)) {
                        item(key = "shared_links_platform_subfolders_strip", contentType = "subfolders_strip") {
                            val allSharedCount = folderNotesCountMap[FolderEntity.SHARED_LINKS_FOLDER_ID] ?: 0
                            val isRootSharedSelected = selectedFolder?.id == FolderEntity.SHARED_LINKS_FOLDER_ID
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { viewModel.selectFolder(FolderEntity.PermanentSharedLinksFolder) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isRootSharedSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        width = if (isRootSharedSelected) 1.5.dp else 1.dp,
                                        color = if (isRootSharedSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = if (isRootSharedSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "All Shared ($allSharedCount)",
                                            fontSize = 12.sp,
                                            fontWeight = if (isRootSharedSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isRootSharedSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                FolderEntity.PermanentSharedLinkSubfolders.forEach { subfolder ->
                                    val subCount = folderNotesCountMap[subfolder.id] ?: 0
                                    val isSubSelected = selectedFolder?.id == subfolder.id
                                    Surface(
                                        onClick = { viewModel.selectFolder(subfolder) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSubSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            width = if (isSubSelected) 1.5.dp else 1.dp,
                                            color = if (isSubSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSubSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = if (isSubSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (subCount > 0) "${subfolder.name} ($subCount)" else subfolder.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSubSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Tasks Section (Neutral Header Pill)
                    val showTasks = (activeFilter in listOf(SearchFilter.ALL, SearchFilter.TASKS, SearchFilter.REMINDERS, SearchFilter.STARRED, SearchFilter.FOLDERS))

                    if (showTasks && taskUiItems.isNotEmpty()) {
                        item(key = "tasks_header", contentType = "header") {
                            val isCompletedFolder = selectedFolder?.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID
                            val accentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = accentColor.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isCompletedFolder) Icons.Default.CheckCircle else Icons.Default.FormatListBulleted,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isCompletedFolder) "COMPLETED TASKS" else "TASKS",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.6.sp
                                            ),
                                            color = accentColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (isCompletedFolder) "${tasksList.size}" else "$pendingTasksCount",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = accentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = taskUiItems, 
                            key = { "task_${it.taskWithSubtasks.task.id}" },
                            contentType = { "task" }
                        ) { taskUiItem ->
                            TaskCard(
                                uiItem = taskUiItem,
                                onClick = onOpenTaskStable,
                                onToggleCompleted = onToggleTaskCompletedStable,
                                onToggleStar = onToggleTaskStarStable,
                                onToggleSubtask = onToggleSubtaskStable,
                                onDelete = onDeleteTaskStable,
                                performanceMode = performanceMode
                            )
                        }
                    }

                    // 3. Notes Section (Material 3 Expressive Header Pill)
                    val showNotes = (activeFilter in listOf(SearchFilter.ALL, SearchFilter.NOTES, SearchFilter.STARRED, SearchFilter.FOLDERS))

                    if (showNotes && noteUiItems.isNotEmpty()) {
                        item(key = "notes_header", contentType = "header") {
                            val isLinksFolder = FolderEntity.isSharedLinksFamilyId(selectedFolder?.id)
                            val accentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = accentColor.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isLinksFolder) Icons.Default.Link else Icons.Default.Description,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (selectedFolder != null && FolderEntity.isSharedLinksSubfolderId(selectedFolder?.id)) {
                                                "SHARED LINKS • ${selectedFolder?.name?.uppercase()}"
                                            } else if (isLinksFolder) {
                                                "SHARED LINKS"
                                            } else {
                                                "NOTES"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.6.sp
                                            ),
                                            color = accentColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "${notesList.size}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = accentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = noteUiItems, 
                            key = { "note_${it.note.id}" },
                            contentType = { "note" }
                        ) { noteUiItem ->
                            NoteCard(
                                uiItem = noteUiItem,
                                onClick = onOpenNoteStable,
                                onToggleStar = onToggleNoteStarStable,
                                onDelete = onDeleteNoteStable,
                                performanceMode = performanceMode,
                                onChecklistToggle = onUpdateNoteContentStable
                            )
                        }
                    }

                    // Empty State (Material 3 Expressive Card)
                    val isEmpty = isVaultLoaded && noteUiItems.isEmpty() && taskUiItems.isEmpty()
                    if (isEmpty) {
                        item(key = "empty_state", contentType = "empty_state") {
                            val currentSubfolders = subfoldersMap[selectedFolder?.id] ?: emptyList()
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 36.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                        modifier = Modifier.size(76.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when {
                                                    selectedFolder?.id == FolderEntity.SHARED_LINKS_FOLDER_ID -> Icons.Default.Link
                                                    selectedFolder?.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> Icons.Default.CheckCircle
                                                    selectedFolder != null -> Icons.Default.Folder
                                                    else -> Icons.Default.Description
                                                },
                                                contentDescription = null,
                                                tint = if (selectedFolder?.id == FolderEntity.SHARED_LINKS_FOLDER_ID) Color(0xFF2563EB) else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No results found"
                                        else if (selectedFolder?.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID) "No completed tasks yet"
                                        else if (selectedFolder?.id == FolderEntity.SHARED_LINKS_FOLDER_ID) "No shared links yet"
                                        else if (selectedFolder != null) "Folder '${selectedFolder?.name}' has no direct items"
                                        else "Your Vault is Ready",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Try changing search terms or switching filter in the Vault Side Panel."
                                        else if (selectedFolder?.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID) "Completed tasks and reminders automatically archive here."
                                        else if (selectedFolder?.id == FolderEntity.SHARED_LINKS_FOLDER_ID) "Share links from your browser, YouTube, or other apps to save rich previews here."
                                        else if (currentSubfolders.isNotEmpty()) "This folder has ${currentSubfolders.size} subfolder(s). Open the Vault Side Panel to browse nested folders."
                                        else "Use the floating toolbar below to create a note or task, or open the Vault Side Panel to organize folders.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { coroutineScope.launch { drawerState.open() } },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountTree,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Open Vault Folder Side Panel",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }


    // Modal Action Sheet for Big Comfortable Touch Targets (Material 3 Expressive)
    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (selectedFolder != null) "Create in '${selectedFolder?.name}'" else "Create in Vault",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )

                // 1. New Rich Note
                ActionSheetItem(
                    icon = Icons.Default.Description,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "New Note",
                    subtitle = "Markdown, rich formatting & attachments",
                    onClick = {
                        showActionSheet = false
                        onOpenNote(null)
                    }
                )

                // 2. New Task + Reminder
                ActionSheetItem(
                    icon = Icons.Default.FormatListBulleted,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "New Task + Reminder",
                    subtitle = "Subtasks, priorities & full-screen alerts",
                    onClick = {
                        showActionSheet = false
                        onOpenTask(null)
                    }
                )

                // 3. New Link (Metadata Extractor)
                ActionSheetItem(
                    icon = Icons.Default.Link,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Save Link with Metadata",
                    subtitle = "Extract image & preview from YouTube, Insta, Web",
                    onClick = {
                        showActionSheet = false
                        showAddLinkDialog = true
                    }
                )

                // 4. New Folder / Subfolder
                ActionSheetItem(
                    icon = Icons.Default.CreateNewFolder,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = if (selectedFolder != null) "New Subfolder" else "New Folder",
                    subtitle = "Organize notes and tasks hierarchically",
                    onClick = {
                        showActionSheet = false
                        createFolderParent = selectedFolder
                        showCreateFolderDialog = true
                    }
                )

                // 5. New Permanent Tag
                ActionSheetItem(
                    icon = Icons.Default.LocalOffer,
                    iconBgColor = Color(0xFFFDE68A).copy(alpha = 0.5f),
                    iconTint = Color(0xFFD97706),
                    title = "New Tag",
                    subtitle = "Create permanent tag with custom color",
                    onClick = {
                        showActionSheet = false
                        showCreateTagDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Dialog: Save Link with Metadata
    if (showAddLinkDialog) {
        AddLinkModalDialog(
            onDismiss = { showAddLinkDialog = false },
            onSaveLink = { url, title, description, imageUrl, tags, subfolderId ->
                viewModel.saveManualLinkWithMetadata(
                    url = url,
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    tags = tags,
                    subfolderId = subfolderId
                )
                showAddLinkDialog = false
            }
        )
    }

    // Dialog: Create Tag
    if (showCreateTagDialog) {
        CreateTagDialog(
            onDismiss = { showCreateTagDialog = false },
            onConfirm = { name, colorIdx ->
                viewModel.createTag(name, colorIdx)
                showCreateTagDialog = false
            }
        )
    }

    // Dialog: Create Folder
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            parentFolder = createFolderParent,
            defaultFolderType = if (activeFilter == SearchFilter.TASKS || activeFilter == SearchFilter.REMINDERS) "TASK" else "NOTE",
            onDismiss = {
                showCreateFolderDialog = false
                createFolderParent = null
            },
            onConfirm = { name, folderType, colorIdx ->
                viewModel.createFolder(
                    name = name,
                    folderType = folderType,
                    parentFolderId = createFolderParent?.id,
                    colorIndex = colorIdx
                )
                showCreateFolderDialog = false
                createFolderParent = null
            }
        )
    }

    // Dialog: Rename Folder
    if (folderToRename != null) {
        RenameFolderDialog(
            folder = folderToRename!!,
            onDismiss = { folderToRename = null },
            onConfirm = { newName ->
                viewModel.updateFolder(folderToRename!!.copy(name = newName))
                folderToRename = null
            }
        )
    }

    // Dialog: Delete Folder
    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete '${folderToDelete?.name}'? Notes and tasks inside will remain safe and be moved to root."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(folderToDelete!!.id)
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { folderToDelete = null },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconBgColor, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
