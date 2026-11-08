package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.FolderColors
import com.example.ui.components.ImageAttachmentStrip
import com.example.ui.components.MultiFolderSelector
import com.example.ui.components.RichTextFormattingToolbar
import com.example.ui.components.RichTextViewer
import com.example.ui.components.TagManager
import com.example.ui.components.stripLeadingMarkdownPrefix
import com.example.ui.theme.NoteAccentColorsDark
import com.example.ui.theme.NoteAccentColorsLight
import com.example.ui.viewmodel.NotesTasksViewModel
import kotlinx.coroutines.delay

enum class NoteToolSection {
    NONE, FORMAT, COLOR, TAGS, IMAGES, FOLDER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    initialNote: NoteEntity?,
    allFolders: List<FolderEntity> = emptyList(),
    defaultFolderId: Long? = null,
    viewModel: NotesTasksViewModel? = null,
    onSave: (
        title: String,
        content: String,
        tags: String,
        imageUris: String,
        colorIndex: Int,
        isStarred: Boolean,
        folderIds: List<Long>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(TextFieldValue(initialNote?.content ?: "")) }
    var isStarred by remember { mutableStateOf(initialNote?.isStarred ?: false) }
    var colorIndex by remember { mutableStateOf(initialNote?.colorIndex ?: 0) }

    var selectedFolderIds by remember {
        mutableStateOf(
            if (initialNote?.folderId != null) setOf(initialNote.folderId)
            else if (defaultFolderId != null) setOf(defaultFolderId)
            else emptySet()
        )
    }

    LaunchedEffect(initialNote?.id) {
        if (initialNote != null && viewModel != null) {
            val crossRefIds = viewModel.getFolderIdsForNote(initialNote.id)
            if (crossRefIds.isNotEmpty()) {
                selectedFolderIds = (selectedFolderIds + crossRefIds).toSet()
            }
        }
    }

    var tags by remember {
        mutableStateOf(
            initialNote?.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        )
    }
    var imagePaths by remember {
        mutableStateOf(
            initialNote?.imageUris?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    var isPreviewMode by remember { mutableStateOf(false) }
    var activeToolSection by remember { mutableStateOf(NoteToolSection.NONE) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val palette = if (isDark) NoteAccentColorsDark else NoteAccentColorsLight
    val selectedColor = palette.getOrElse(colorIndex) { MaterialTheme.colorScheme.surface }

    val allCustomTags by if (viewModel != null) {
        viewModel.allTags.collectAsState()
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    // Filter available folders to note folders & general folders (including permanent Shared Links)
    val noteFolders = remember(allFolders) {
        listOf(FolderEntity.PermanentSharedLinksFolder) + allFolders.filter { it.folderType == "NOTE" || it.folderType == "ALL" }
    }
    val selectedFolderEntities = remember(noteFolders, selectedFolderIds) {
        noteFolders.filter { selectedFolderIds.contains(it.id) }
    }

    // Auto-save logic on back gesture, back button, or save button
    val performAutoSaveAndExit = {
        val trimmedTitle = title.trim()
        val trimmedContent = content.text.trim()
        val hasContent = trimmedTitle.isNotBlank() || trimmedContent.isNotBlank() || imagePaths.isNotEmpty() || tags.isNotEmpty()

        if (hasContent || initialNote != null) {
            val finalTitle = if (trimmedTitle.isNotBlank()) {
                trimmedTitle
            } else if (trimmedContent.isNotBlank()) {
                val firstLine = trimmedContent.lineSequence().firstOrNull { it.isNotBlank() } ?: "Untitled Note"
                stripLeadingMarkdownPrefix(firstLine).take(40).trim().ifBlank { "Untitled Note" }
            } else {
                "Untitled Note"
            }

            onSave(
                finalTitle,
                trimmedContent,
                tags.joinToString(","),
                imagePaths.joinToString(","),
                colorIndex,
                isStarred,
                selectedFolderIds.toList()
            )
        } else {
            onDismiss()
        }
    }

    // Android back button & gesture support
    BackHandler {
        if (activeToolSection != NoteToolSection.NONE) {
            activeToolSection = NoteToolSection.NONE
        } else {
            performAutoSaveAndExit()
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            parentFolder = null,
            defaultFolderType = "NOTE",
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name, folderType, colorIdx ->
                showCreateFolderDialog = false
                viewModel?.createFolder(name = name, folderType = folderType, colorIndex = colorIdx)
            }
        )
    }

    Scaffold(
        containerColor = selectedColor,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialNote == null) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeToolSection != NoteToolSection.NONE) {
                                activeToolSection = NoteToolSection.NONE
                            } else {
                                performAutoSaveAndExit()
                            }
                        },
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
                    // Toggle Preview Mode
                    IconButton(
                        onClick = { isPreviewMode = !isPreviewMode },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Preview,
                            contentDescription = if (isPreviewMode) "Edit Mode" else "Preview Mode",
                            tint = if (isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Star button
                    IconButton(
                        onClick = { isStarred = !isStarred },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isStarred) "Unstar note" else "Star note",
                            tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Large comfortable Save Button
                    Button(
                        onClick = {
                            performAutoSaveAndExit()
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .padding(end = 6.dp),
                        shape = RoundedCornerShape(12.dp),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = selectedColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Scrollable Content Area: fills space above docked toolbar
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Folder & Tag Badges
                if (selectedFolderEntities.isNotEmpty() || tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        selectedFolderEntities.forEach { f ->
                            val fColor = FolderColors.getOrElse(f.colorIndex) { MaterialTheme.colorScheme.primary }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = fColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, fColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = fColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = f.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                val titleFocusRequester = remember { FocusRequester() }
                val contentFocusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current

                LaunchedEffect(isPreviewMode) {
                    if (!isPreviewMode) {
                        delay(100)
                        try {
                            contentFocusRequester.requestFocus()
                            keyboardController?.show()
                        } catch (_: Exception) {
                        }
                    }
                }

                // Note Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = "Note Title...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            try {
                                contentFocusRequester.requestFocus()
                                keyboardController?.show()
                            } catch (_: Exception) {
                            }
                        }
                    ),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                )

                // Fluid Note Content Editor or Rich Preview
                if (!isPreviewMode) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = {
                            Text(
                                text = "Start writing your note... Markdown is supported (# heading, **bold**, • bullet, - [ ] checkbox)",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 280.dp)
                            .focusRequester(contentFocusRequester)
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Rich Markdown Preview",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            RichTextViewer(
                                markdownContent = content.text.ifBlank { "*No note content written yet.*" },
                                onChecklistToggle = { updated ->
                                    content = TextFieldValue(updated)
                                }
                            )
                        }
                    }
                }

                // Generous bottom spacer so cursor and bottom content always scroll above the docked toolbar
                Spacer(modifier = Modifier.height(120.dp))
            }

            // Docked Accessory Toolbar directly above navigation bar / keyboard
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Expandable Tool Panels
                    AnimatedVisibility(
                        visible = activeToolSection != NoteToolSection.NONE,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (activeToolSection) {
                                            NoteToolSection.FORMAT -> "Formatting & Checklist"
                                            NoteToolSection.COLOR -> "Note Color Theme"
                                            NoteToolSection.TAGS -> "Tags"
                                            NoteToolSection.IMAGES -> "Image Attachments"
                                            NoteToolSection.FOLDER -> "Assign Note to Folders"
                                            else -> ""
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { activeToolSection = NoteToolSection.NONE },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tool",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                when (activeToolSection) {
                                    NoteToolSection.FORMAT -> {
                                        RichTextFormattingToolbar(
                                            textValue = content,
                                            onValueChange = { content = it }
                                        )
                                    }

                                    NoteToolSection.COLOR -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            palette.forEachIndexed { idx, color ->
                                                val isSelected = colorIndex == idx
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .background(color, CircleShape)
                                                        .border(
                                                            width = if (isSelected) 3.dp else 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                            shape = CircleShape
                                                        )
                                                        .clickable { colorIndex = idx },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    NoteToolSection.TAGS -> {
                                        TagManager(
                                            tags = tags,
                                            onAddTag = { tag -> if (!tags.contains(tag)) tags = tags + tag },
                                            onRemoveTag = { tag -> tags = tags - tag },
                                            customTags = allCustomTags,
                                            onCreateCustomTag = { name, colorIdx ->
                                                viewModel?.createTag(name, colorIdx)
                                            }
                                        )
                                    }

                                    NoteToolSection.IMAGES -> {
                                        ImageAttachmentStrip(
                                            imagePaths = imagePaths,
                                            onAddImages = { newPaths -> imagePaths = imagePaths + newPaths },
                                            onRemoveImage = { path -> imagePaths = imagePaths - path }
                                        )
                                    }

                                    NoteToolSection.FOLDER -> {
                                        MultiFolderSelector(
                                            title = "Save Note in Multiple Folders",
                                            availableFolders = noteFolders,
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
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }

                    // Main Docked Tool Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Format Tool
                        IconButton(
                            onClick = {
                                activeToolSection = if (activeToolSection == NoteToolSection.FORMAT) NoteToolSection.NONE else NoteToolSection.FORMAT
                            },
                            modifier = Modifier.size(48.dp),
                            colors = if (activeToolSection == NoteToolSection.FORMAT) {
                                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = "Formatting Toolbar",
                                tint = if (activeToolSection == NoteToolSection.FORMAT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 2. Color Palette Tool
                        IconButton(
                            onClick = {
                                activeToolSection = if (activeToolSection == NoteToolSection.COLOR) NoteToolSection.NONE else NoteToolSection.COLOR
                            },
                            modifier = Modifier.size(48.dp),
                            colors = if (activeToolSection == NoteToolSection.COLOR) {
                                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Note Color",
                                tint = if (activeToolSection == NoteToolSection.COLOR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 3. Tag Tool
                        IconButton(
                            onClick = {
                                activeToolSection = if (activeToolSection == NoteToolSection.TAGS) NoteToolSection.NONE else NoteToolSection.TAGS
                            },
                            modifier = Modifier.size(48.dp),
                            colors = if (activeToolSection == NoteToolSection.TAGS) {
                                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = "Tags",
                                tint = if (activeToolSection == NoteToolSection.TAGS || tags.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 4. Image Attachment Tool
                        IconButton(
                            onClick = {
                                activeToolSection = if (activeToolSection == NoteToolSection.IMAGES) NoteToolSection.NONE else NoteToolSection.IMAGES
                            },
                            modifier = Modifier.size(48.dp),
                            colors = if (activeToolSection == NoteToolSection.IMAGES) {
                                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Images",
                                tint = if (activeToolSection == NoteToolSection.IMAGES || imagePaths.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 5. Folder Tool (with badge count of selected folders)
                        IconButton(
                            onClick = {
                                activeToolSection = if (activeToolSection == NoteToolSection.FOLDER) NoteToolSection.NONE else NoteToolSection.FOLDER
                            },
                            modifier = Modifier.size(48.dp),
                            colors = if (activeToolSection == NoteToolSection.FOLDER) {
                                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else IconButtonDefaults.iconButtonColors()
                        ) {
                            BadgedBox(
                                badge = {
                                    if (selectedFolderIds.isNotEmpty()) {
                                        Badge { Text(selectedFolderIds.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folders",
                                    tint = if (activeToolSection == NoteToolSection.FOLDER || selectedFolderIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
