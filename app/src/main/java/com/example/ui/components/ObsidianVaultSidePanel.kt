package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Context
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.TagEntity
import com.example.data.model.TaskWithSubtasks

private enum class FolderSortOrder(val label: String) {
    NAME_ASC("A → Z"),
    NAME_DESC("Z → A"),
    ITEM_COUNT("Most Items")
}

@Composable
fun ObsidianVaultSidePanel(
    allFolders: List<FolderEntity>,
    allNotes: List<NoteEntity>,
    allTasks: List<TaskWithSubtasks>,
    allTags: List<TagEntity>,
    selectedFolder: FolderEntity?,
    activeFilter: SearchFilter,
    selectedTagFilter: String?,
    folderNotesCountMap: Map<Long?, Int>,
    folderTasksCountMap: Map<Long?, Int>,
    subfoldersMap: Map<Long?, List<FolderEntity>>,
    onSelectFolder: (FolderEntity?) -> Unit,
    onSelectFilter: (SearchFilter) -> Unit,
    onSelectTag: (String?) -> Unit,
    onCreateRootFolder: () -> Unit,
    onCreateSubfolder: (FolderEntity) -> Unit,
    onRenameFolder: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onCreateNewNote: () -> Unit,
    onCreateNewTask: () -> Unit,
    onCreateNewTag: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onOpenTask: (TaskWithSubtasks) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    isDrawerOpen: Boolean = true
) {
    val context = LocalContext.current
    val sidePanelPrefs = remember(context) {
        context.getSharedPreferences("obsidian_sidepanel_prefs", Context.MODE_PRIVATE)
    }

    // Track expanded state for folders by folder ID, persisted across app restarts
    val expandedFolders = remember(sidePanelPrefs) {
        mutableStateMapOf<Long, Boolean>().apply {
            val savedRaw = sidePanelPrefs.getString("expanded_folders_map", "") ?: ""
            if (savedRaw.isNotBlank()) {
                savedRaw.split(",").forEach { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val id = parts[0].toLongOrNull()
                        val exp = parts[1].toBooleanStrictOrNull()
                        if (id != null && exp != null) {
                            put(id, exp)
                        }
                    }
                }
            }
        }
    }

    val saveExpandedFolders: () -> Unit = {
        val serialized = expandedFolders.entries.joinToString(",") { "${it.key}:${it.value}" }
        sidePanelPrefs.edit().putString("expanded_folders_map", serialized).apply()
    }

    val setFolderExpanded: (Long, Boolean) -> Unit = { folderId, expanded ->
        expandedFolders[folderId] = expanded
        saveExpandedFolders()
    }

    var showFilesInTree by rememberSaveable {
        mutableStateOf(sidePanelPrefs.getBoolean("show_files_in_tree", true))
    }
    var sortOrder by rememberSaveable {
        val savedSort = sidePanelPrefs.getString("folder_sort_order", FolderSortOrder.NAME_ASC.name)
        mutableStateOf(FolderSortOrder.entries.find { it.name == savedSort } ?: FolderSortOrder.NAME_ASC)
    }
    var folderFilterQuery by rememberSaveable { mutableStateOf("") }
    var showFilterInput by rememberSaveable { mutableStateOf(false) }

    var viewsSectionExpanded by rememberSaveable {
        mutableStateOf(sidePanelPrefs.getBoolean("views_section_expanded", true))
    }
    var systemSectionExpanded by rememberSaveable {
        mutableStateOf(sidePanelPrefs.getBoolean("system_section_expanded", true))
    }
    var foldersSectionExpanded by rememberSaveable {
        mutableStateOf(sidePanelPrefs.getBoolean("folders_section_expanded", true))
    }
    var unfiledSectionExpanded by rememberSaveable {
        mutableStateOf(sidePanelPrefs.getBoolean("unfiled_section_expanded", false))
    }
    var tagsSectionExpanded by rememberSaveable {
        mutableStateOf(sidePanelPrefs.getBoolean("tags_section_expanded", true))
    }

    // Ensure ancestors of selectedFolder are expanded automatically only if not explicitly set by user
    LaunchedEffect(selectedFolder, allFolders) {
        var curr = selectedFolder
        var changed = false
        var guard = 0
        while (curr != null && guard < 12) {
            if (!expandedFolders.containsKey(curr.id)) {
                expandedFolders[curr.id] = true
                changed = true
            }
            val parentId = curr.parentFolderId
            curr = if (parentId != null) {
                FolderEntity.getPermanentFolderById(parentId) ?: allFolders.find { it.id == parentId }
            } else null
            guard++
        }
        if (changed) {
            saveExpandedFolders()
        }
    }

    val customFolders = remember(allFolders) {
        allFolders.filter { !FolderEntity.isPermanentFolderId(it.id) }
    }

    val rootFolders = remember(customFolders, folderFilterQuery, sortOrder, folderNotesCountMap, folderTasksCountMap) {
        val base = if (folderFilterQuery.isBlank()) {
            customFolders.filter { it.parentFolderId == null }
        } else {
            customFolders.filter { it.name.contains(folderFilterQuery.trim(), ignoreCase = true) }
        }
        when (sortOrder) {
            FolderSortOrder.NAME_ASC -> base.sortedBy { it.name.lowercase() }
            FolderSortOrder.NAME_DESC -> base.sortedByDescending { it.name.lowercase() }
            FolderSortOrder.ITEM_COUNT -> base.sortedByDescending {
                (folderNotesCountMap[it.id] ?: 0) + (folderTasksCountMap[it.id] ?: 0)
            }
        }
    }

    val notesByFolder = remember(allNotes, isDrawerOpen, showFilesInTree) {
        if (!isDrawerOpen || !showFilesInTree) emptyMap() else allNotes.groupBy { it.folderId }
    }

    val tasksByFolder = remember(allTasks, isDrawerOpen, showFilesInTree) {
        if (!isDrawerOpen || !showFilesInTree) emptyMap() else allTasks.filter { !it.task.isCompleted }.groupBy { it.task.folderId }
    }

    val unfiledNotes = remember(notesByFolder) {
        notesByFolder[null] ?: emptyList()
    }
    val unfiledTasks = remember(tasksByFolder) {
        tasksByFolder[null] ?: emptyList()
    }

    val regularNotesCount = remember(allNotes) {
        allNotes.count { !FolderEntity.isSharedLinksFamilyId(it.folderId) }
    }
    val sharedLinksCount = remember(allNotes) {
        allNotes.count { FolderEntity.isSharedLinksFamilyId(it.folderId) }
    }
    val activeTasksCount = remember(allTasks) {
        allTasks.count { !it.task.isCompleted }
    }
    val starredCount = remember(allNotes, allTasks) {
        allNotes.count { it.isStarred } + allTasks.count { it.task.isStarred }
    }
    val remindersCount = remember(allTasks) {
        allTasks.count { it.hasReminders }
    }

    val allExpanded = remember(customFolders, expandedFolders.size, expandedFolders.values.toList()) {
        customFolders.isNotEmpty() && customFolders.all { expandedFolders[it.id] == true }
    }

    ModalDrawerSheet(
        modifier = modifier
            .width(324.dp)
            .fillMaxHeight()
            .testTag("obsidian_side_panel"),
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 3.dp,
        windowInsets = WindowInsets.safeDrawing
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            // 1. Vault Top Header (Obsidian Vault Switcher Style — M3 Expressive)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Workspace Vault",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${customFolders.size} folders • $regularNotesCount notes • $sharedLinksCount links • ${allTasks.size} tasks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_side_panel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close folder side panel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Obsidian File Explorer Action Toolbar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // New Folder
                    ExplorerToolbarButton(
                        icon = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = MaterialTheme.colorScheme.primary,
                        testTag = "sidebar_new_folder_btn",
                        onClick = {
                            if (selectedFolder != null &&
                                selectedFolder.id != FolderEntity.COMPLETED_TASKS_FOLDER_ID &&
                                selectedFolder.id != FolderEntity.SHARED_LINKS_FOLDER_ID
                            ) {
                                onCreateSubfolder(selectedFolder)
                            } else {
                                onCreateRootFolder()
                            }
                        }
                    )

                    // New Note
                    ExplorerToolbarButton(
                        icon = Icons.Default.Description,
                        contentDescription = "New Note",
                        tint = MaterialTheme.colorScheme.secondary,
                        testTag = "sidebar_new_note_btn",
                        onClick = {
                            onCloseDrawer()
                            onCreateNewNote()
                        }
                    )

                    // New Task
                    ExplorerToolbarButton(
                        icon = Icons.Default.FormatListBulleted,
                        contentDescription = "New Task",
                        tint = MaterialTheme.colorScheme.tertiary,
                        testTag = "sidebar_new_task_btn",
                        onClick = {
                            onCloseDrawer()
                            onCreateNewTask()
                        }
                    )

                    // Filter Folders Toggle
                    ExplorerToolbarButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Filter Folder Tree",
                        tint = if (showFilterInput || folderFilterQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        testTag = "sidebar_search_folders_btn",
                        onClick = {
                            showFilterInput = !showFilterInput
                            if (!showFilterInput) folderFilterQuery = ""
                        }
                    )

                    // Sort Order Cycle
                    ExplorerToolbarButton(
                        icon = Icons.Default.SortByAlpha,
                        contentDescription = "Sort Folders (${sortOrder.label})",
                        tint = if (sortOrder != FolderSortOrder.NAME_ASC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        testTag = "sidebar_sort_folders_btn",
                        onClick = {
                            val nextSort = when (sortOrder) {
                                FolderSortOrder.NAME_ASC -> FolderSortOrder.NAME_DESC
                                FolderSortOrder.NAME_DESC -> FolderSortOrder.ITEM_COUNT
                                FolderSortOrder.ITEM_COUNT -> FolderSortOrder.NAME_ASC
                            }
                            sortOrder = nextSort
                            sidePanelPrefs.edit().putString("folder_sort_order", nextSort.name).apply()
                        }
                    )

                    // Toggle Files in Tree
                    ExplorerToolbarButton(
                        icon = Icons.Default.AccountTree,
                        contentDescription = if (showFilesInTree) "Hide notes & tasks in tree" else "Show notes & tasks in tree",
                        tint = if (showFilesInTree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        testTag = "sidebar_toggle_files_tree_btn",
                        onClick = {
                            val nextVal = !showFilesInTree
                            showFilesInTree = nextVal
                            sidePanelPrefs.edit().putBoolean("show_files_in_tree", nextVal).apply()
                        }
                    )

                    // Expand / Collapse All
                    ExplorerToolbarButton(
                        icon = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = if (allExpanded) "Collapse all folders" else "Expand all folders",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        testTag = "sidebar_expand_collapse_all_btn",
                        onClick = {
                            val target = !allExpanded
                            customFolders.forEach { f ->
                                expandedFolders[f.id] = target
                            }
                            unfiledSectionExpanded = target
                            saveExpandedFolders()
                            sidePanelPrefs.edit().putBoolean("unfiled_section_expanded", target).apply()
                        }
                    )
                }
            }

            // Optional Quick Filter Input inside Side Panel
            AnimatedVisibility(
                visible = showFilterInput,
                enter = expandVertically(tween(150)) + fadeIn(tween(120)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(120))
            ) {
                OutlinedTextField(
                    value = folderFilterQuery,
                    onValueChange = { folderFilterQuery = it },
                    placeholder = { Text("Filter folders...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (folderFilterQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { folderFilterQuery = "" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear folder filter",
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            // 3. Scrollable Obsidian Explorer Tree
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // SECTION A: VAULT VIEWS (replaces square filter pills under search bar)
                item(key = "section_views_header") {
                    ObsidianSectionHeader(
                        title = "VAULT VIEWS",
                        expanded = viewsSectionExpanded,
                        onToggle = {
                            val nextVal = !viewsSectionExpanded
                            viewsSectionExpanded = nextVal
                            sidePanelPrefs.edit().putBoolean("views_section_expanded", nextVal).apply()
                        }
                    )
                }

                if (viewsSectionExpanded) {
                    item(key = "view_all") {
                        val isSelected = selectedFolder == null && activeFilter == SearchFilter.ALL && selectedTagFilter == null
                        ObsidianNavRow(
                            icon = Icons.Default.FolderOpen,
                            iconTint = MaterialTheme.colorScheme.primary,
                            label = "All Vault Items",
                            count = allNotes.size + activeTasksCount,
                            isSelected = isSelected,
                            testTag = "nav_view_all",
                            onClick = {
                                onSelectFolder(null)
                                onSelectFilter(SearchFilter.ALL)
                                onSelectTag(null)
                                onCloseDrawer()
                            }
                        )
                    }

                    item(key = "view_notes") {
                        val isSelected = selectedFolder == null && activeFilter == SearchFilter.NOTES
                        ObsidianNavRow(
                            icon = Icons.Default.Description,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            label = "All Notes",
                            count = regularNotesCount,
                            isSelected = isSelected,
                            testTag = "nav_view_notes",
                            onClick = {
                                onSelectFolder(null)
                                onSelectFilter(SearchFilter.NOTES)
                                onCloseDrawer()
                            }
                        )
                    }

                    item(key = "view_links") {
                        val isSelected = selectedFolder == null && activeFilter == SearchFilter.LINKS
                        ObsidianNavRow(
                            icon = Icons.Default.Link,
                            iconTint = MaterialTheme.colorScheme.primary,
                            label = "All Shared Links",
                            count = sharedLinksCount,
                            isSelected = isSelected,
                            testTag = "nav_view_links",
                            onClick = {
                                onSelectFolder(null)
                                onSelectFilter(SearchFilter.LINKS)
                                onCloseDrawer()
                            }
                        )
                    }

                    item(key = "view_tasks") {
                        val isSelected = selectedFolder == null && activeFilter == SearchFilter.TASKS
                        ObsidianNavRow(
                            icon = Icons.Default.FormatListBulleted,
                            iconTint = MaterialTheme.colorScheme.primary,
                            label = "All Tasks",
                            count = activeTasksCount,
                            isSelected = isSelected,
                            testTag = "nav_view_tasks",
                            onClick = {
                                onSelectFolder(null)
                                onSelectFilter(SearchFilter.TASKS)
                                onCloseDrawer()
                            }
                        )
                    }

                    item(key = "view_starred") {
                        val isSelected = selectedFolder == null && activeFilter == SearchFilter.STARRED
                        ObsidianNavRow(
                            icon = Icons.Default.Star,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "Starred",
                            count = starredCount,
                            isSelected = isSelected,
                            testTag = "nav_view_starred",
                            onClick = {
                                onSelectFolder(null)
                                onSelectFilter(SearchFilter.STARRED)
                                onCloseDrawer()
                            }
                        )
                    }

                    item(key = "view_reminders") {
                        val isSelected = selectedFolder == null && activeFilter == SearchFilter.REMINDERS
                        ObsidianNavRow(
                            icon = Icons.Default.Alarm,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "Reminders",
                            count = remindersCount,
                            isSelected = isSelected,
                            testTag = "nav_view_reminders",
                            onClick = {
                                onSelectFolder(null)
                                onSelectFilter(SearchFilter.REMINDERS)
                                onCloseDrawer()
                            }
                        )
                    }
                }

                // SECTION B: SYSTEM / PERMANENT FOLDERS
                item(key = "section_system_header") {
                    Spacer(modifier = Modifier.height(6.dp))
                    ObsidianSectionHeader(
                        title = "SYSTEM FOLDERS",
                        expanded = systemSectionExpanded,
                        onToggle = {
                            val nextVal = !systemSectionExpanded
                            systemSectionExpanded = nextVal
                            sidePanelPrefs.edit().putBoolean("system_section_expanded", nextVal).apply()
                        }
                    )
                }

                if (systemSectionExpanded) {
                    item(key = "system_shared_links") {
                        val linksCount = folderNotesCountMap[FolderEntity.SHARED_LINKS_FOLDER_ID] ?: 0
                        val isParentSelected = selectedFolder?.id == FolderEntity.SHARED_LINKS_FOLDER_ID
                        val isSharedLinksExpanded = expandedFolders[FolderEntity.SHARED_LINKS_FOLDER_ID]
                            ?: FolderEntity.isSharedLinksSubfolderId(selectedFolder?.id)
                        val guideLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isParentSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        onSelectFolder(FolderEntity.PermanentSharedLinksFolder)
                                        onSelectFilter(SearchFilter.ALL)
                                        onCloseDrawer()
                                    }
                                    .padding(start = 4.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
                                    .testTag("nav_folder_shared_links"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                setFolderExpanded(
                                                    FolderEntity.SHARED_LINKS_FOLDER_ID,
                                                    !isSharedLinksExpanded
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSharedLinksExpanded) Icons.Default.KeyboardArrowDown
                                            else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = if (isSharedLinksExpanded) "Collapse Shared Links" else "Expand Shared Links",
                                            tint = if (isParentSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Text(
                                        text = "Shared Links",
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isParentSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isParentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "14 apps",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isParentSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ) {
                                    Text(
                                        text = linksCount.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isParentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (isSharedLinksExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp, bottom = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    FolderEntity.PermanentSharedLinkSubfolders.forEach { subfolder ->
                                        val subCount = folderNotesCountMap[subfolder.id] ?: 0
                                        val isSubSelected = selectedFolder?.id == subfolder.id
                                        val subColor = FolderColors.getOrElse(subfolder.colorIndex) { Color(0xFF2563EB) }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSubSelected) subColor.copy(alpha = 0.16f)
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    onSelectFolder(subfolder)
                                                    onSelectFilter(SearchFilter.ALL)
                                                    onCloseDrawer()
                                                }
                                                .padding(end = 10.dp)
                                                .testTag("nav_folder_shared_sub_${subfolder.id}"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(22.dp)
                                                        .fillMaxHeight(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.dp)
                                                            .fillMaxHeight()
                                                            .background(guideLineColor)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = if (isSubSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = subColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = subfolder.name,
                                                    fontSize = 12.8.sp,
                                                    fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSubSelected) subColor else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                            }

                                            if (subCount > 0 || isSubSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSubSelected) subColor.copy(alpha = 0.22f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                                ) {
                                                    Text(
                                                        text = subCount.toString(),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isSubSelected) subColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "system_completed_tasks") {
                        val completedCount = folderTasksCountMap[FolderEntity.COMPLETED_TASKS_FOLDER_ID] ?: 0
                        val isSelected = selectedFolder?.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID
                        ObsidianNavRow(
                            icon = Icons.Default.CheckCircle,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "Completed Tasks",
                            count = completedCount,
                            isSelected = isSelected,
                            testTag = "nav_folder_completed_tasks",
                            onClick = {
                                onSelectFolder(FolderEntity.PermanentCompletedFolder)
                                onSelectFilter(SearchFilter.ALL)
                                onCloseDrawer()
                            }
                        )
                    }
                }

                // SECTION C: HIERARCHICAL FOLDER EXPLORER TREE
                item(key = "section_folders_header") {
                    Spacer(modifier = Modifier.height(6.dp))
                    ObsidianSectionHeader(
                        title = "FOLDERS (${customFolders.size})",
                        subtitle = sortOrder.label,
                        expanded = foldersSectionExpanded,
                        onToggle = {
                            val nextVal = !foldersSectionExpanded
                            foldersSectionExpanded = nextVal
                            sidePanelPrefs.edit().putBoolean("folders_section_expanded", nextVal).apply()
                        },
                        onActionClick = onCreateRootFolder,
                        actionIcon = Icons.Default.Add,
                        actionDescription = "Create root folder"
                    )
                }

                if (foldersSectionExpanded) {
                    if (rootFolders.isEmpty()) {
                        item(key = "empty_folders_tree") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable(onClick = onCreateRootFolder)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (folderFilterQuery.isNotBlank()) "No matching folders"
                                        else "No folders yet — tap to create one",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "recursive_folder_tree") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                rootFolders.forEach { folder ->
                                    ObsidianFolderTreeNode(
                                        folder = folder,
                                        depth = 0,
                                        selectedFolderId = selectedFolder?.id,
                                        expandedFolders = expandedFolders,
                                        onSetFolderExpanded = setFolderExpanded,
                                        subfoldersMap = subfoldersMap,
                                        notesByFolder = notesByFolder,
                                        tasksByFolder = tasksByFolder,
                                        folderNotesCountMap = folderNotesCountMap,
                                        folderTasksCountMap = folderTasksCountMap,
                                        showFilesInTree = showFilesInTree,
                                        sortOrder = sortOrder,
                                        onSelectFolder = { clicked ->
                                            onSelectFolder(clicked)
                                            onSelectFilter(SearchFilter.ALL)
                                            onCloseDrawer()
                                        },
                                        onCreateSubfolder = onCreateSubfolder,
                                        onRenameFolder = onRenameFolder,
                                        onDeleteFolder = onDeleteFolder,
                                        onOpenNote = { note ->
                                            onCloseDrawer()
                                            onOpenNote(note)
                                        },
                                        onOpenTask = { task ->
                                            onCloseDrawer()
                                            onOpenTask(task)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Unfiled / Root items node when showFilesInTree is enabled
                    if (showFilesInTree && (unfiledNotes.isNotEmpty() || unfiledTasks.isNotEmpty())) {
                        item(key = "unfiled_root_items") {
                            Spacer(modifier = Modifier.height(4.dp))
                            ObsidianUnfiledTreeNode(
                                unfiledNotes = unfiledNotes,
                                unfiledTasks = unfiledTasks,
                                expanded = unfiledSectionExpanded,
                                onToggleExpand = {
                                    val nextVal = !unfiledSectionExpanded
                                    unfiledSectionExpanded = nextVal
                                    sidePanelPrefs.edit().putBoolean("unfiled_section_expanded", nextVal).apply()
                                },
                                onOpenNote = { note ->
                                    onCloseDrawer()
                                    onOpenNote(note)
                                },
                                onOpenTask = { task ->
                                    onCloseDrawer()
                                    onOpenTask(task)
                                }
                            )
                        }
                    }
                }

                // SECTION D: TAGS EXPLORER PANE
                item(key = "section_tags_header") {
                    Spacer(modifier = Modifier.height(8.dp))
                    ObsidianSectionHeader(
                        title = "TAGS (${allTags.size})",
                        expanded = tagsSectionExpanded,
                        onToggle = {
                            val nextVal = !tagsSectionExpanded
                            tagsSectionExpanded = nextVal
                            sidePanelPrefs.edit().putBoolean("tags_section_expanded", nextVal).apply()
                        },
                        onActionClick = onCreateNewTag,
                        actionIcon = Icons.Default.Add,
                        actionDescription = "Create new tag"
                    )
                }

                if (tagsSectionExpanded) {
                    if (allTags.isEmpty()) {
                        item(key = "empty_tags") {
                            Text(
                                text = "No tags yet",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        item(key = "tags_tree_list") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                allTags.forEach { tagEntity ->
                                    val isTagSelected = selectedTagFilter.equals(tagEntity.name, ignoreCase = true)
                                    val tagColor = TagPaletteColors.getOrElse(tagEntity.colorIndex) { MaterialTheme.colorScheme.primary }
                                    val tagUsageCount = remember(allNotes, allTasks, tagEntity.name) {
                                        allNotes.count { it.tags.contains(tagEntity.name, ignoreCase = true) } +
                                            allTasks.count { it.task.tags.contains(tagEntity.name, ignoreCase = true) }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isTagSelected) tagColor.copy(alpha = 0.16f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                onSelectTag(if (isTagSelected) null else tagEntity.name)
                                                onCloseDrawer()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(tagColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "#${tagEntity.name}",
                                                fontSize = 13.sp,
                                                fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isTagSelected) tagColor else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isTagSelected) tagColor.copy(alpha = 0.24f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = tagUsageCount.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isTagSelected) tagColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "sidebar_bottom_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ExplorerToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun ObsidianSectionHeader(
    title: String,
    subtitle: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) "Collapse section" else "Expand section",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• $subtitle",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (onActionClick != null && actionIcon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionDescription,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ObsidianNavRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    count: Int,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ObsidianFolderTreeNode(
    folder: FolderEntity,
    depth: Int,
    selectedFolderId: Long?,
    expandedFolders: MutableMap<Long, Boolean>,
    onSetFolderExpanded: (Long, Boolean) -> Unit,
    subfoldersMap: Map<Long?, List<FolderEntity>>,
    notesByFolder: Map<Long?, List<NoteEntity>>,
    tasksByFolder: Map<Long?, List<TaskWithSubtasks>>,
    folderNotesCountMap: Map<Long?, Int>,
    folderTasksCountMap: Map<Long?, Int>,
    showFilesInTree: Boolean,
    sortOrder: FolderSortOrder,
    onSelectFolder: (FolderEntity) -> Unit,
    onCreateSubfolder: (FolderEntity) -> Unit,
    onRenameFolder: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onOpenTask: (TaskWithSubtasks) -> Unit
) {
    val rawSubfolders = subfoldersMap[folder.id] ?: emptyList()
    val subfolders = remember(rawSubfolders, sortOrder, folderNotesCountMap, folderTasksCountMap) {
        when (sortOrder) {
            FolderSortOrder.NAME_ASC -> rawSubfolders.sortedBy { it.name.lowercase() }
            FolderSortOrder.NAME_DESC -> rawSubfolders.sortedByDescending { it.name.lowercase() }
            FolderSortOrder.ITEM_COUNT -> rawSubfolders.sortedByDescending {
                (folderNotesCountMap[it.id] ?: 0) + (folderTasksCountMap[it.id] ?: 0)
            }
        }
    }

    val directNotes = notesByFolder[folder.id] ?: emptyList()
    val directTasks = tasksByFolder[folder.id] ?: emptyList()
    val directCount = (folderNotesCountMap[folder.id] ?: 0) + (folderTasksCountMap[folder.id] ?: 0)

    val hasChildren = subfolders.isNotEmpty() || (showFilesInTree && (directNotes.isNotEmpty() || directTasks.isNotEmpty()))
    val isExpanded = expandedFolders[folder.id] == true
    val isSelected = selectedFolderId == folder.id
    var menuExpanded by remember { mutableStateOf(false) }

    val folderColor = FolderColors.getOrElse(folder.colorIndex) { MaterialTheme.colorScheme.primary }
    val guideLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) folderColor.copy(alpha = 0.15f)
                    else Color.Transparent
                )
                .clickable {
                    onSetFolderExpanded(folder.id, true)
                    onSelectFolder(folder)
                }
                .testTag("obsidian_folder_node_${folder.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Obsidian Vertical Indentation Guide Lines
            repeat(depth) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(guideLineColor)
                    )
                }
            }

            // Expand / Collapse Chevron Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        onSetFolderExpanded(folder.id, !isExpanded)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (hasChildren) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse folder" else "Expand folder",
                        tint = if (isSelected) folderColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
                    )
                }
            }

            // Colored Folder Icon
            Icon(
                imageVector = if (isExpanded || isSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = folderColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Folder Title + Type indicator
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = folder.name,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) folderColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Subtle Folder Type Badge (N / T)
                val isTaskFolder = folder.folderType == "TASK"
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = folderColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isTaskFolder) "T" else "N",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = folderColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Item Count Pill
            if (directCount > 0 || subfolders.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) folderColor.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    Text(
                        text = directCount.toString(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) folderColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            // Quick Add Subfolder '+' Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        onSetFolderExpanded(folder.id, true)
                        onCreateSubfolder(folder)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Subfolder in ${folder.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Context Menu (Rename / Add Subfolder / Delete)
            Box {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { menuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Folder actions for ${folder.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("New Subfolder") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSetFolderExpanded(folder.id, true)
                            onCreateSubfolder(folder)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Folder") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRenameFolder(folder)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteFolder(folder)
                        }
                    )
                }
            }
        }

        // Recursive Children (Subfolders + Notes & Tasks inside Folder)
        if (isExpanded && hasChildren) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // 1. Subfolders
                subfolders.forEach { childFolder ->
                    ObsidianFolderTreeNode(
                        folder = childFolder,
                        depth = depth + 1,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onSetFolderExpanded = onSetFolderExpanded,
                        subfoldersMap = subfoldersMap,
                        notesByFolder = notesByFolder,
                        tasksByFolder = tasksByFolder,
                        folderNotesCountMap = folderNotesCountMap,
                        folderTasksCountMap = folderTasksCountMap,
                        showFilesInTree = showFilesInTree,
                        sortOrder = sortOrder,
                        onSelectFolder = onSelectFolder,
                        onCreateSubfolder = onCreateSubfolder,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder,
                        onOpenNote = onOpenNote,
                        onOpenTask = onOpenTask
                    )
                }

                // 2. Files (Notes & Tasks) in this folder when showFilesInTree is active
                if (showFilesInTree) {
                    directNotes.forEach { note ->
                        ObsidianFileTreeLeafRow(
                            title = note.title.ifBlank { "Untitled Note" },
                            icon = Icons.Default.Description,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            depth = depth + 1,
                            isCompleted = false,
                            isStarred = note.isStarred,
                            guideLineColor = guideLineColor,
                            onClick = { onOpenNote(note) }
                        )
                    }
                    directTasks.forEach { tws ->
                        ObsidianFileTreeLeafRow(
                            title = tws.task.title.ifBlank { "Untitled Task" },
                            icon = if (tws.task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            iconTint = if (tws.task.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            depth = depth + 1,
                            isCompleted = tws.task.isCompleted,
                            isStarred = tws.task.isStarred,
                            guideLineColor = guideLineColor,
                            onClick = { onOpenTask(tws) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObsidianUnfiledTreeNode(
    unfiledNotes: List<NoteEntity>,
    unfiledTasks: List<TaskWithSubtasks>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onOpenTask: (TaskWithSubtasks) -> Unit
) {
    val guideLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val totalUnfiled = unfiledNotes.size + unfiledTasks.size

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggleExpand)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unfiled (Root)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Text(
                    text = totalUnfiled.toString(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                unfiledNotes.forEach { note ->
                    ObsidianFileTreeLeafRow(
                        title = note.title.ifBlank { "Untitled Note" },
                        icon = Icons.Default.Description,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        depth = 1,
                        isCompleted = false,
                        isStarred = note.isStarred,
                        guideLineColor = guideLineColor,
                        onClick = { onOpenNote(note) }
                    )
                }
                unfiledTasks.forEach { tws ->
                    ObsidianFileTreeLeafRow(
                        title = tws.task.title.ifBlank { "Untitled Task" },
                        icon = if (tws.task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        iconTint = if (tws.task.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        depth = 1,
                        isCompleted = tws.task.isCompleted,
                        isStarred = tws.task.isStarred,
                        guideLineColor = guideLineColor,
                        onClick = { onOpenTask(tws) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ObsidianFileTreeLeafRow(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    depth: Int,
    isCompleted: Boolean,
    isStarred: Boolean,
    guideLineColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(depth) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(guideLineColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint.copy(alpha = 0.85f),
            modifier = Modifier.size(14.dp)
        )

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = title,
            fontSize = 12.5.sp,
            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
        )

        if (isStarred) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(12.dp)
            )
        }
    }
}
