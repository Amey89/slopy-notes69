package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity

val FolderColors = listOf(
    Color(0xFF3B82F6), // Blue
    Color(0xFF10B981), // Emerald
    Color(0xFF8B5CF6), // Purple
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444), // Rose
    Color(0xFF06B6D4)  // Cyan
)

@Composable
fun FolderBreadcrumbBar(
    currentFolder: FolderEntity,
    allFolders: List<FolderEntity>,
    onExitFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Find parent hierarchy if any
    val path = mutableListOf<FolderEntity>()
    var curr: FolderEntity? = currentFolder
    while (curr != null) {
        path.add(0, curr)
        curr = if (curr.parentFolderId != null) {
            allFolders.find { it.id == curr?.parentFolderId }
        } else null
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "All",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                path.forEach { f ->
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = f.name,
                        fontSize = 13.sp,
                        fontWeight = if (f.id == currentFolder.id) FontWeight.Bold else FontWeight.Normal,
                        color = if (f.id == currentFolder.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onExitFolder,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit folder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private val FolderCardShape = RoundedCornerShape(16.dp)
private val FolderIconShape = RoundedCornerShape(10.dp)
private val FolderBadgeShape = RoundedCornerShape(6.dp)
private val SubfolderPillShape = RoundedCornerShape(8.dp)

@Composable
fun FolderCard(
    folder: FolderEntity,
    subfolders: List<FolderEntity>,
    notesCount: Int,
    tasksCount: Int,
    onClick: () -> Unit,
    onAddSubfolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    performanceMode: Boolean = false
) {
    val isCompletedArchive = folder.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID
    val isSharedLinksFolder = folder.id == FolderEntity.SHARED_LINKS_FOLDER_ID
    val isSystemFolder = isCompletedArchive || isSharedLinksFolder
    var menuExpanded by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val folderColor = remember(isCompletedArchive, isSharedLinksFolder, folder.colorIndex, primaryColor) {
        when {
            isCompletedArchive -> Color(0xFF10B981)
            isSharedLinksFolder -> Color(0xFF2563EB)
            else -> FolderColors.getOrElse(folder.colorIndex) { primaryColor }
        }
    }
    val isTaskFolder = folder.folderType == "TASK" || isCompletedArchive

    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val cardBorder = remember(isCompletedArchive, isSharedLinksFolder, outlineVariantColor) {
        when {
            isCompletedArchive -> BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.6f))
            isSharedLinksFolder -> BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.6f))
            else -> BorderStroke(1.dp, outlineVariantColor.copy(alpha = 0.5f))
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = FolderCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(folderColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isCompletedArchive -> Icons.Default.CheckCircle
                                isSharedLinksFolder -> Icons.Default.Link
                                isTaskFolder -> Icons.Default.FolderOpen
                                else -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    isCompletedArchive -> Color(0xFF10B981).copy(alpha = 0.18f)
                                    isSharedLinksFolder -> Color(0xFF2563EB).copy(alpha = 0.18f)
                                    isTaskFolder -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        isCompletedArchive -> "Permanent Archive"
                                        isSharedLinksFolder -> "Shared Links"
                                        isTaskFolder -> "Tasks"
                                        else -> "Notes"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isCompletedArchive -> Color(0xFF047857)
                                        isSharedLinksFolder -> Color(0xFF1D4ED8)
                                        isTaskFolder -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                isCompletedArchive -> "$tasksCount completed tasks & reminders"
                                isSharedLinksFolder -> "$notesCount saved links"
                                isTaskFolder -> "$tasksCount tasks"
                                else -> "$notesCount notes" + if (subfolders.isNotEmpty()) " • ${subfolders.size} subfolders" else ""
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                if (!isSystemFolder) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Folder options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (menuExpanded) {
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add Subfolder") },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onAddSubfolder()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRename()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                }
            }
        }

            // Subfolders quick pills (Non-scrolling to prevent gesture fighting with LazyColumn)
            if (subfolders.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayedSubfolders = subfolders.take(3)
                    val remainingCount = subfolders.size - displayedSubfolders.size
                    displayedSubfolders.forEach { sub ->
                        val subColor = FolderColors.getOrElse(sub.colorIndex) { MaterialTheme.colorScheme.primary }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = subColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, subColor.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = subColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sub.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (remainingCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "+$remainingCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    parentFolder: FolderEntity?,
    defaultFolderType: String = "NOTE",
    onDismiss: () -> Unit,
    onConfirm: (name: String, folderType: String, colorIndex: Int) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var folderType by remember {
        mutableStateOf(parentFolder?.folderType ?: defaultFolderType)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (parentFolder == null) "New Folder" else "New Subfolder in '${parentFolder.name}'",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Folder Type Selection (only if not a subfolder)
                if (parentFolder == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Folder Type",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = folderType == "NOTE",
                                onClick = { folderType = "NOTE" },
                                label = { Text("Notes Folder") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = folderType == "TASK",
                                onClick = { folderType = "TASK" },
                                label = { Text("Tasks Folder") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g. Work, Personal, Project X") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Folder Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FolderColors.forEachIndexed { index, color ->
                        val isSelected = selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index },
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
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onConfirm(folderName.trim(), folderType, selectedColorIndex)
                    }
                },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameFolderDialog(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(folder.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Folder", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Folder Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName.trim())
                    }
                },
                enabled = newName.isNotBlank(),
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
