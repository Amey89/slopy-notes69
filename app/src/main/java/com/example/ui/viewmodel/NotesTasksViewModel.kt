package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NotesTasksApp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.SubTaskEntity
import com.example.data.model.TagEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithSubtasks
import com.example.data.sync.DriveConnectionState
import com.example.data.sync.DriveSyncState
import com.example.data.sync.GoogleDriveSyncManager
import com.example.data.sync.GoogleSignInOutcome
import com.example.data.sync.MarkdownImportSummary
import com.example.data.sync.SyncOutcome
import com.example.reminder.ReminderManager
import com.example.ui.components.SearchFilter
import com.example.widget.NotesTasksAppWidget
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import androidx.compose.ui.graphics.Color
import com.example.ui.components.AppDateFormatter
import com.example.ui.model.NoteUiItem
import com.example.ui.model.TaskUiItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import com.example.util.LinkMetadataExtractor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotesTasksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as NotesTasksApp).repository
    private val prefs = application.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    // Dark Mode: 0 = System, 1 = Light, 2 = Dark
    private val _darkModeSetting = MutableStateFlow(prefs.getInt("pref_dark_mode", 0))
    val darkModeSetting: StateFlow<Int> = _darkModeSetting.asStateFlow()

    fun setDarkMode(mode: Int) {
        _darkModeSetting.value = mode
        prefs.edit().putInt("pref_dark_mode", mode).apply()
    }

    // Theme Preset: 0 = Slate, 1 = Warm Amber, 2 = Midnight AMOLED, 3 = Emerald Forest, 4 = Twilight, 5 = Ocean
    private val _themePreset = MutableStateFlow(prefs.getInt("pref_theme_preset", 0))
    val themePreset: StateFlow<Int> = _themePreset.asStateFlow()

    fun setThemePreset(preset: Int) {
        _themePreset.value = preset
        prefs.edit().putInt("pref_theme_preset", preset).apply()
    }

    // Background Pattern: 0 = Clean, 1 = Dotted Journal, 2 = Graph Paper, 3 = Atmospheric Aura
    private val _backgroundPattern = MutableStateFlow(prefs.getInt("pref_background_pattern", 0))
    val backgroundPattern: StateFlow<Int> = _backgroundPattern.asStateFlow()

    fun setBackgroundPattern(pattern: Int) {
        _backgroundPattern.value = pattern
        prefs.edit().putInt("pref_background_pattern", pattern).apply()
    }

    // High-Performance / Organization Mode (Optimized for 10,000+ notes & tasks: removes animation delays, lightweight flat rendering, fast scroll recycling)
    private val _performanceMode = MutableStateFlow(prefs.getBoolean("pref_performance_mode", false))
    val performanceMode: StateFlow<Boolean> = _performanceMode.asStateFlow()

    fun setPerformanceMode(enabled: Boolean) {
        _performanceMode.value = enabled
        prefs.edit().putBoolean("pref_performance_mode", enabled).apply()
    }

    // Custom Tags
    val allTags: StateFlow<List<TagEntity>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTag(name: String, colorIndex: Int = 0) {
        viewModelScope.launch {
            repository.createTag(name, colorIndex)
        }
    }

    fun deleteTag(name: String) {
        viewModelScope.launch {
            repository.deleteTag(name)
        }
    }

    // Active Tag Filter in Search / Filter bar (defaults to null on launch so app opens on All Vault Items)
    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    fun selectTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun setSelectedTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    // Search & Filter (defaults to SearchFilter.ALL on launch so app opens on All Vault Items)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(SearchFilter.ALL)
    val activeFilter: StateFlow<SearchFilter> = _activeFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActiveFilter(filter: SearchFilter) {
        _activeFilter.value = filter
    }

    // Base all notes and all tasks (single shared SQLite flow per table)
    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskWithSubtasks>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Folders State
    val allFolders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootFolders: StateFlow<List<FolderEntity>> = allFolders.map { list ->
        list.filter { it.parentFolderId == null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // High-performance pre-computed maps (Offloaded from UI thread, reusing in-memory flows)
    val folderNotesCountMap: StateFlow<Map<Long?, Int>> = allNotes.map { notes ->
        val map = notes.groupBy { it.folderId }.mapValues { it.value.size }.toMutableMap()
        map[FolderEntity.SHARED_LINKS_FOLDER_ID] = notes.count { it.folderId == FolderEntity.SHARED_LINKS_FOLDER_ID }
        map
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val folderTasksCountMap: StateFlow<Map<Long?, Int>> = allTasks.map { tasks ->
        val map = tasks.filter { !it.task.isCompleted }.groupBy { it.task.folderId }.mapValues { it.value.size }.toMutableMap()
        map[FolderEntity.COMPLETED_TASKS_FOLDER_ID] = tasks.count { it.task.isCompleted }
        map
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subfoldersMap: StateFlow<Map<Long?, List<FolderEntity>>> = allFolders.map { folders ->
        folders.filter { it.parentFolderId != null }.groupBy { it.parentFolderId }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Always default to null (All Vault Items) when opening the app
    private val _selectedFolder = MutableStateFlow<FolderEntity?>(null)
    val selectedFolder: StateFlow<FolderEntity?> = _selectedFolder.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear any legacy saved folder/filter preferences so app always starts cleanly on All Vault Items
            prefs.edit()
                .remove("pref_selected_folder_id")
                .remove("pref_active_filter")
                .remove("pref_selected_tag_filter")
                .apply()
            repository.ensureDefaultTags()
        }
    }

    fun selectFolder(folder: FolderEntity?) {
        _selectedFolder.value = folder
    }

    val visibleFolders: StateFlow<List<FolderEntity>> = combine(allFolders, _selectedFolder, _activeFilter) { folders, selected, filter ->
        if (selected == null) {
            val baseFolders = when (filter) {
                SearchFilter.NOTES -> folders.filter { it.parentFolderId == null && it.folderType == "NOTE" }
                SearchFilter.TASKS, SearchFilter.REMINDERS -> folders.filter { it.parentFolderId == null && it.folderType == "TASK" }
                else -> folders.filter { it.parentFolderId == null }
            }
            val systemFolders = buildList {
                if (filter in listOf(SearchFilter.ALL, SearchFilter.TASKS, SearchFilter.REMINDERS, SearchFilter.FOLDERS)) {
                    add(FolderEntity.PermanentCompletedFolder)
                }
                if (filter in listOf(SearchFilter.ALL, SearchFilter.NOTES, SearchFilter.FOLDERS)) {
                    add(FolderEntity.PermanentSharedLinksFolder)
                }
            }
            systemFolders + baseFolders
        } else {
            folders.filter { it.parentFolderId == selected.id }
        }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val noteFolders: StateFlow<List<FolderEntity>> = allFolders.map { list ->
        list.filter { it.folderType == "NOTE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootNoteFolders: StateFlow<List<FolderEntity>> = allFolders.map { list ->
        list.filter { it.parentFolderId == null && it.folderType == "NOTE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskFolders: StateFlow<List<FolderEntity>> = allFolders.map { list ->
        list.filter { it.folderType == "TASK" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootTaskFolders: StateFlow<List<FolderEntity>> = allFolders.map { list ->
        list.filter { it.parentFolderId == null && it.folderType == "TASK" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateUpFolder() {
        val current = _selectedFolder.value ?: return
        val parentId = current.parentFolderId
        if (parentId == null) {
            _selectedFolder.value = null
        } else {
            _selectedFolder.value = allFolders.value.find { it.id == parentId }
        }
    }

    fun createFolder(name: String, folderType: String = "NOTE", parentFolderId: Long? = null, colorIndex: Int = 0) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createFolder(name, folderType, parentFolderId, colorIndex)
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder)
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            if (_selectedFolder.value?.id == folderId) {
                _selectedFolder.value = null
            }
            repository.deleteFolder(folderId)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    suspend fun getFolderIdsForNote(noteId: Long): List<Long> = repository.getFolderIdsForNote(noteId)
    suspend fun getFolderIdsForTask(taskId: Long): List<Long> = repository.getFolderIdsForTask(taskId)

    // Starred items derived in-memory from allNotes / allTasks
    val starredNotes: StateFlow<List<NoteEntity>> = allNotes.map { notes ->
        notes.filter { it.isStarred }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredTasks: StateFlow<List<TaskWithSubtasks>> = allTasks.map { tasks ->
        tasks.filter { it.task.isStarred && !it.task.isCompleted }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Notes (Fast in-memory filtering, zero stutter)
    val notesList: StateFlow<List<NoteEntity>> = combine(
        allNotes,
        _selectedFolder,
        _activeFilter,
        _searchQuery,
        _selectedTagFilter
    ) { notes, folder, filter, query, tagFilter ->
        val folderFiltered = if (folder != null) {
            when (folder.id) {
                FolderEntity.COMPLETED_TASKS_FOLDER_ID -> emptyList() // No notes in completed tasks folder
                FolderEntity.SHARED_LINKS_FOLDER_ID -> notes.filter { it.folderId == FolderEntity.SHARED_LINKS_FOLDER_ID }
                else -> notes.filter { it.folderId == folder.id }
            }
        } else {
            notes
        }
        val queryFiltered = if (query.isNotBlank()) {
            folderFiltered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        } else {
            folderFiltered
        }
        val tagFiltered = if (!tagFilter.isNullOrBlank()) {
            queryFiltered.filter { it.tags.contains(tagFilter, ignoreCase = true) }
        } else {
            queryFiltered
        }
        when (filter) {
            SearchFilter.ALL, SearchFilter.NOTES, SearchFilter.FOLDERS -> tagFiltered
            SearchFilter.STARRED -> tagFiltered.filter { it.isStarred }
            else -> emptyList()
        }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Tasks (Fast in-memory filtering, zero stutter)
    // Completed tasks are archived in COMPLETED_TASKS_FOLDER_ID and excluded from All Vault Items / normal folders
    val tasksList: StateFlow<List<TaskWithSubtasks>> = combine(
        allTasks,
        _selectedFolder,
        _activeFilter,
        _searchQuery,
        _selectedTagFilter
    ) { tasks, folder, filter, query, tagFilter ->
        val folderFiltered = if (folder != null) {
            when (folder.id) {
                FolderEntity.COMPLETED_TASKS_FOLDER_ID -> tasks.filter { it.task.isCompleted }
                FolderEntity.SHARED_LINKS_FOLDER_ID -> emptyList() // No tasks in shared links folder
                else -> tasks.filter { it.task.folderId == folder.id && !it.task.isCompleted }
            }
        } else {
            tasks.filter { !it.task.isCompleted }
        }
        val queryFiltered = if (query.isNotBlank()) {
            folderFiltered.filter { tws ->
                tws.task.title.contains(query, ignoreCase = true) ||
                tws.task.description.contains(query, ignoreCase = true) ||
                tws.task.tags.contains(query, ignoreCase = true) ||
                tws.subtasks.any { it.title.contains(query, ignoreCase = true) }
            }
        } else {
            folderFiltered
        }
        val tagFiltered = if (!tagFilter.isNullOrBlank()) {
            queryFiltered.filter { it.task.tags.contains(tagFilter, ignoreCase = true) }
        } else {
            queryFiltered
        }
        when (filter) {
            SearchFilter.ALL, SearchFilter.TASKS, SearchFilter.FOLDERS -> tagFiltered
            SearchFilter.STARRED -> tagFiltered.filter { it.task.isStarred }
            SearchFilter.REMINDERS -> tagFiltered.filter { it.hasReminders }
            else -> emptyList()
        }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 120Hz Ultra-Fluid Pre-Computed UI Models (Offloaded to Dispatchers.Default)
    val noteUiItems: StateFlow<List<NoteUiItem>> = combine(
        notesList,
        _performanceMode
    ) { notes, perfMode ->
        notes.map { it.toUiItem(perfMode) }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskUiItems: StateFlow<List<TaskUiItem>> = tasksList.map { tasks ->
        tasks.map { it.toUiItem() }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isVaultLoaded: StateFlow<Boolean> = combine(
        allNotes,
        allTasks
    ) { _, _ -> true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Direct access for widget / notification deep links
    suspend fun getNoteDirect(id: Long): NoteEntity? = repository.getNoteDirect(id)
    suspend fun getTaskDirect(id: Long): TaskWithSubtasks? = repository.getTaskWithSubtasksDirect(id)

    // Note operations
    fun saveNoteWithFolders(
        id: Long = 0,
        title: String,
        content: String,
        tags: String,
        imageUris: String,
        colorIndex: Int,
        isStarred: Boolean,
        folderIds: List<Long>
    ) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = id,
                title = title,
                content = content,
                tags = tags,
                imageUris = imageUris,
                colorIndex = colorIndex,
                isStarred = isStarred,
                folderId = folderIds.firstOrNull()
            )
            repository.saveNoteWithFolders(note, folderIds)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        tags: String,
        imageUris: String,
        colorIndex: Int,
        isStarred: Boolean,
        folderId: Long? = null
    ) {
        saveNoteWithFolders(
            id = id,
            title = title,
            content = content,
            tags = tags,
            imageUris = imageUris,
            colorIndex = colorIndex,
            isStarred = isStarred,
            folderIds = if (folderId != null) listOf(folderId) else emptyList()
        )
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    fun toggleNoteStar(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleNoteStar(note.id, note.isStarred)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    // Task & Subtask operations
    fun saveTaskWithFolders(
        id: Long = 0,
        title: String,
        description: String,
        priority: Int,
        dueDate: Long?,
        reminderTime: Long?,
        reminderEnabled: Boolean,
        isStarred: Boolean,
        subtasks: List<SubTaskEntity>,
        folderIds: List<Long>
    ) {
        viewModelScope.launch {
            val taskEntity = TaskEntity(
                id = id,
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                reminderTime = reminderTime,
                reminderEnabled = reminderEnabled,
                isStarred = isStarred,
                folderId = folderIds.firstOrNull()
            )

            val savedTaskId = repository.saveTaskWithFolders(taskEntity, subtasks, folderIds)

            // Update reminder in AlarmManager
            if (reminderEnabled && reminderTime != null && reminderTime > System.currentTimeMillis()) {
                ReminderManager.scheduleTaskReminder(
                    context = getApplication(),
                    taskId = savedTaskId,
                    title = title,
                    description = description,
                    priority = priority,
                    triggerAtMillis = reminderTime
                )
            } else {
                ReminderManager.cancelTaskReminder(getApplication(), savedTaskId)
            }
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    fun saveTask(
        id: Long = 0,
        title: String,
        description: String,
        priority: Int,
        dueDate: Long?,
        reminderTime: Long?,
        reminderEnabled: Boolean,
        isStarred: Boolean,
        subtasks: List<SubTaskEntity>,
        folderId: Long? = null
    ) {
        saveTaskWithFolders(
            id = id,
            title = title,
            description = description,
            priority = priority,
            dueDate = dueDate,
            reminderTime = reminderTime,
            reminderEnabled = reminderEnabled,
            isStarred = isStarred,
            subtasks = subtasks,
            folderIds = if (folderId != null) listOf(folderId) else emptyList()
        )
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            ReminderManager.cancelTaskReminder(getApplication(), taskId)
            repository.deleteTaskById(taskId)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    fun toggleTaskCompleted(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(taskId, isCompleted)
            if (isCompleted) {
                ReminderManager.cancelTaskReminder(getApplication(), taskId)
            }
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    fun toggleTaskStar(taskWithSubtasks: TaskWithSubtasks) {
        viewModelScope.launch {
            repository.toggleTaskStar(taskWithSubtasks.task.id, taskWithSubtasks.task.isStarred)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    fun toggleSubtaskCompleted(subtaskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleSubTaskCompleted(subtaskId, isCompleted)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    // Quick Add
    fun quickAddNote(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(
                    title = title,
                    content = "",
                    isStarred = true,
                    folderId = _selectedFolder.value?.id
                )
            )
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    fun quickAddTask(title: String, reminderTime: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val hasReminder = reminderTime != null
            val taskId = repository.insertTask(
                TaskEntity(
                    title = title,
                    reminderTime = reminderTime,
                    reminderEnabled = hasReminder,
                    isStarred = true,
                    folderId = _selectedFolder.value?.id
                )
            )
            if (hasReminder && reminderTime!! > System.currentTimeMillis()) {
                ReminderManager.scheduleTaskReminder(
                    context = getApplication(),
                    taskId = taskId,
                    title = title,
                    description = "",
                    priority = 1,
                    triggerAtMillis = reminderTime
                )
            }
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
    }

    // Backup & Restore
    suspend fun exportJsonBackup(): String {
        return repository.exportDatabaseToJson()
    }

    suspend fun importJsonBackup(jsonString: String): Result<Int> {
        val res = repository.importDatabaseFromJson(jsonString)
        if (res.isSuccess) {
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
        return res
    }

    suspend fun exportMarkdownZipBundle(context: Context): File {
        return repository.exportMarkdownZipBundle(context)
    }

    suspend fun importMarkdownZipBundle(inputStream: InputStream): Result<MarkdownImportSummary> {
        val res = repository.importMarkdownZipBundle(inputStream)
        if (res.isSuccess) {
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
        }
        return res
    }

    suspend fun exportMarkdownBundle(context: Context): File {
        return repository.exportMarkdownZipBundle(context)
    }

    // Google Drive Cloud Sync
    val googleDriveSyncManager = GoogleDriveSyncManager(application)

    val driveConnectionState: StateFlow<DriveConnectionState> = googleDriveSyncManager.connectionState
    val driveSyncState: StateFlow<DriveSyncState> = googleDriveSyncManager.syncState
    val driveAutoSyncOnOpen: StateFlow<Boolean> = googleDriveSyncManager.autoSyncOnOpen
    val driveLastSyncTime: StateFlow<Long> = googleDriveSyncManager.lastSyncTime
    val driveLastSyncSummary: StateFlow<String?> = googleDriveSyncManager.lastSyncSummary

    fun setDriveAutoSyncOnOpen(enabled: Boolean) {
        googleDriveSyncManager.setAutoSyncOnOpen(enabled)
    }

    fun getGoogleSignInClient(requestServerIdToken: Boolean = false): GoogleSignInClient {
        return googleDriveSyncManager.getGoogleSignInClient(requestServerIdToken)
    }

    suspend fun handleGoogleSignInResult(data: Intent?): GoogleSignInOutcome {
        return googleDriveSyncManager.handleSignInResult(data)
    }

    suspend fun fetchDriveTokenAfterConsent(email: String): Result<String> {
        return googleDriveSyncManager.fetchDriveTokenAfterConsent(email)
    }

    suspend fun verifyAndConnectDriveToken(token: String): Result<String> {
        val res = googleDriveSyncManager.verifyAccessToken(token)
        res.onSuccess { email ->
            googleDriveSyncManager.connectManually(email, token.trim())
        }
        return res
    }

    fun getActiveClientId(): String = googleDriveSyncManager.getActiveClientId()

    fun setActiveClientId(clientId: String) {
        googleDriveSyncManager.setActiveClientId(clientId)
    }

    fun resetActiveClientId() {
        googleDriveSyncManager.resetActiveClientId()
    }

    fun connectGoogleDriveManually(email: String, token: String) {
        googleDriveSyncManager.connectManually(email, token)
    }

    fun disconnectGoogleDrive() {
        googleDriveSyncManager.disconnect()
    }

    fun syncToGoogleDrive(onComplete: ((Result<SyncOutcome>) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val json = repository.exportDatabaseToJson()
                val result = googleDriveSyncManager.uploadBackupJson(json)
                onComplete?.invoke(result)
            } catch (e: Exception) {
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    fun restoreFromGoogleDrive(onComplete: ((Result<Int>) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val downloadRes = googleDriveSyncManager.downloadBackupJson()
                if (downloadRes.isFailure) {
                    onComplete?.invoke(Result.failure(downloadRes.exceptionOrNull() ?: Exception("Download failed")))
                    return@launch
                }
                val json = downloadRes.getOrThrow()
                val importRes = repository.importDatabaseFromJson(json)
                if (importRes.isSuccess) {
                    NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
                }
                onComplete?.invoke(importRes)
            } catch (e: Exception) {
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    fun getDriveAccountEmail(): String? = googleDriveSyncManager.getAccountEmail()

    // Custom Deletion Operations
    fun deleteNotesCustom(deleteAll: Boolean, unstarredOnly: Boolean, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteNotesCustom(deleteAll, unstarredOnly)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
            onComplete?.invoke()
        }
    }

    fun deleteTasksCustom(deleteAll: Boolean, completedOnly: Boolean, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteTasksCustom(deleteAll, completedOnly)
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
            onComplete?.invoke()
        }
    }

    fun deleteCustomFolders(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteCustomFolders()
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
            onComplete?.invoke()
        }
    }

    fun deleteCustomTags(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteCustomTags()
            onComplete?.invoke()
        }
    }

    fun clearEntireAppData(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.clearEntireAppData()
            _selectedFolder.value = null
            _searchQuery.value = ""
            NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
            onComplete?.invoke()
        }
    }

    fun checkAndPerformDriveAutoSync() {
        viewModelScope.launch {
            if (googleDriveSyncManager.isConnected() && googleDriveSyncManager.autoSyncOnOpen.value) {
                try {
                    val json = repository.exportDatabaseToJson()
                    googleDriveSyncManager.uploadBackupJson(json)
                } catch (_: Exception) {
                    // Silent background sync
                }
            }
        }
    }

    private val _shareToastEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val shareToastEvent = _shareToastEvent.asSharedFlow()

    fun handleIncomingShare(sharedText: String, subject: String? = null, onComplete: ((NoteEntity) -> Unit)? = null) {
        if (sharedText.isBlank()) return
        viewModelScope.launch {
            try {
                val metadata = LinkMetadataExtractor.resolve(sharedText, subject)
                val contentMarkdown = buildString {
                    appendLine(metadata.url)
                    if (metadata.description.isNotBlank()) {
                        appendLine()
                        appendLine(metadata.description)
                    }
                    val extraText = sharedText.replace(metadata.url, "").trim().trim('-', ':', '|')
                    if (extraText.isNotBlank()) {
                        appendLine()
                        appendLine()
                        appendLine("**Shared text:** $extraText")
                    }
                }.trim()

                val tagsList = buildList {
                    add("link")
                    if (metadata.domain.isNotBlank()) add(metadata.domain.lowercase())
                    if (metadata.isYouTube) add("video")
                }.distinct().joinToString(", ")

                val note = NoteEntity(
                    title = metadata.title.ifBlank { "Shared Link" },
                    content = contentMarkdown,
                    tags = tagsList,
                    imageUris = metadata.imageUrl,
                    isStarred = false,
                    colorIndex = 2, // Ocean Blue
                    folderId = FolderEntity.SHARED_LINKS_FOLDER_ID,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val noteId = repository.saveNoteWithFolders(note, listOf(FolderEntity.SHARED_LINKS_FOLDER_ID))
                val savedNote = note.copy(id = noteId)
                selectFolder(FolderEntity.PermanentSharedLinksFolder)
                _shareToastEvent.tryEmit("Saved to Shared Links: ${savedNote.title}")
                NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
                onComplete?.invoke(savedNote)
            } catch (e: Exception) {
                _shareToastEvent.tryEmit("Failed to save link: ${e.message}")
            }
        }
    }

    fun saveManualLinkWithMetadata(
        url: String,
        title: String,
        description: String,
        imageUrl: String,
        tags: String,
        onComplete: ((NoteEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val contentMarkdown = buildString {
                    appendLine(url)
                    if (description.isNotBlank()) {
                        appendLine()
                        appendLine(description)
                    }
                }.trim()

                val note = NoteEntity(
                    title = title.ifBlank { "Shared Link" },
                    content = contentMarkdown,
                    tags = tags.ifBlank { "links" },
                    imageUris = imageUrl,
                    isStarred = false,
                    colorIndex = 2, // Ocean Blue
                    folderId = FolderEntity.SHARED_LINKS_FOLDER_ID,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val noteId = repository.saveNoteWithFolders(note, listOf(FolderEntity.SHARED_LINKS_FOLDER_ID))
                val savedNote = note.copy(id = noteId)
                selectFolder(FolderEntity.PermanentSharedLinksFolder)
                _shareToastEvent.tryEmit("Saved to Shared Links: ${savedNote.title}")
                NotesTasksAppWidget.notifyWidgetUpdate(getApplication())
                onComplete?.invoke(savedNote)
            } catch (e: Exception) {
                _shareToastEvent.tryEmit("Failed to save link: ${e.message}")
            }
        }
    }
}

private val CLEAN_MARKDOWN_REGEX = Regex("[#*`_~>\\[\\]]")
private val HighPriorityColor = Color(0xFFDC2626)
private val MediumPriorityColor = Color(0xFFD97706)
private val LowPriorityColor = Color(0xFF3B82F6)

private fun NoteEntity.toUiItem(isPerformanceMode: Boolean): NoteUiItem {
    val isSharedLink = folderId == FolderEntity.SHARED_LINKS_FOLDER_ID
    val images = if (imageUris.isBlank()) emptyList()
    else imageUris.split(",").map { it.trim() }.filter { it.isNotBlank() }

    val tagList = if (tags.isBlank()) emptyList()
    else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }

    var url: String? = null
    if (isSharedLink || tagList.contains("link")) {
        url = content.lineSequence().firstOrNull { line ->
            val trimmed = line.trim()
            trimmed.startsWith("http://") || trimmed.startsWith("https://")
        }?.trim()
    }

    val displayUrl = url?.let {
        it.removePrefix("https://").removePrefix("http://").take(42)
    }

    val displayTitle = title.ifBlank { if (isSharedLink) "Shared Link" else "Untitled Note" }

    val cleanPreview = if (content.isNotBlank()) {
        var text = content
        if (isSharedLink && url != null) {
            val firstHttp = text.indexOf("http")
            if (firstHttp != -1) {
                val endOfLine = text.indexOf('\n', firstHttp)
                text = if (endOfLine != -1) {
                    text.substring(0, firstHttp) + text.substring(endOfLine + 1)
                } else {
                    text.substring(0, firstHttp)
                }
            }
        }
        val snippet = if (text.length > 280) text.substring(0, 280) else text
        if (isPerformanceMode) snippet.trim() else CLEAN_MARKDOWN_REGEX.replace(snippet, "").trim()
    } else ""

    val formattedDate = AppDateFormatter.format(updatedAt)

    return NoteUiItem(
        note = this,
        displayTitle = displayTitle,
        formattedDate = formattedDate,
        cleanPreview = cleanPreview,
        imageList = images,
        tagsList = tagList,
        extractedUrl = url,
        displayUrl = displayUrl,
        isSharedLinkNote = isSharedLink
    )
}

private fun TaskWithSubtasks.toUiItem(): TaskUiItem {
    val reminderStr = if (task.reminderEnabled && task.reminderTime != null) {
        AppDateFormatter.format(task.reminderTime)
    } else null

    val dueStr = if (task.dueDate != null && (task.reminderTime == null || task.dueDate != task.reminderTime)) {
        "Due: ${AppDateFormatter.format(task.dueDate)}"
    } else null

    val (pText, pColor) = when (task.priority) {
        2 -> "High" to HighPriorityColor
        0 -> "Low" to LowPriorityColor
        else -> "Medium" to MediumPriorityColor
    }

    val completedCount = completedSubtaskCount
    val totalCount = totalSubtaskCount
    val summary = "$completedCount/$totalCount subtasks"
    val progress = subtaskProgress

    return TaskUiItem(
        taskWithSubtasks = this,
        formattedReminder = reminderStr,
        formattedDueDate = dueStr,
        priorityText = pText,
        priorityColor = pColor,
        subtaskSummary = summary,
        subtaskProgress = progress
    )
}
