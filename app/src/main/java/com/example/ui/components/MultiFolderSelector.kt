package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity

@Composable
fun MultiFolderSelector(
    title: String = "Vault Folder Tree",
    availableFolders: List<FolderEntity>,
    selectedFolderIds: Set<Long>,
    onToggleFolder: (Long) -> Unit,
    onAddNewFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedMap = remember { mutableStateMapOf<Long, Boolean>() }

    val subfoldersByParent = remember(availableFolders) {
        availableFolders.filter { it.parentFolderId != null }.groupBy { it.parentFolderId }
    }

    val rootFolders = remember(availableFolders) {
        availableFolders.filter { it.parentFolderId == null }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedFolderIds.isEmpty()) "Unfiled (Vault Root)"
                            else "Filed in ${selectedFolderIds.size} folder${if (selectedFolderIds.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAddNewFolder,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New folder",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Folder", fontSize = 12.sp)
                }
            }

            if (availableFolders.isEmpty()) {
                Text(
                    text = "No folders created yet. Tap '+ New Folder' to create one.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    rootFolders.forEach { folder ->
                        SelectableFolderTreeNode(
                            folder = folder,
                            depth = 0,
                            selectedFolderIds = selectedFolderIds,
                            subfoldersByParent = subfoldersByParent,
                            expandedMap = expandedMap,
                            onToggleFolder = onToggleFolder
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableFolderTreeNode(
    folder: FolderEntity,
    depth: Int,
    selectedFolderIds: Set<Long>,
    subfoldersByParent: Map<Long?, List<FolderEntity>>,
    expandedMap: MutableMap<Long, Boolean>,
    onToggleFolder: (Long) -> Unit
) {
    val isSelected = selectedFolderIds.contains(folder.id)
    val children = subfoldersByParent[folder.id] ?: emptyList()
    val isExpanded = expandedMap[folder.id] ?: true
    val isPermanentLinks = folder.id == FolderEntity.SHARED_LINKS_FOLDER_ID
    val isPermanentCompleted = folder.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID

    val folderColor = when {
        isPermanentCompleted -> Color(0xFF10B981)
        isPermanentLinks -> Color(0xFF2563EB)
        else -> FolderColors.getOrElse(folder.colorIndex) { MaterialTheme.colorScheme.primary }
    }
    val guideColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) folderColor.copy(alpha = 0.14f)
                    else Color.Transparent
                )
                .clickable { onToggleFolder(folder.id) }
                .padding(end = 10.dp),
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
                            .background(guideColor)
                    )
                }
            }

            if (children.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expandedMap[folder.id] = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(10.dp))
            }

            Icon(
                imageVector = when {
                    isPermanentCompleted -> Icons.Default.CheckCircle
                    isPermanentLinks -> Icons.Default.Link
                    isSelected || isExpanded -> Icons.Default.FolderOpen
                    else -> Icons.Default.Folder
                },
                contentDescription = null,
                tint = folderColor,
                modifier = Modifier.size(17.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = folder.name,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) folderColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = folderColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isExpanded && children.isNotEmpty()) {
            children.forEach { sub ->
                SelectableFolderTreeNode(
                    folder = sub,
                    depth = depth + 1,
                    selectedFolderIds = selectedFolderIds,
                    subfoldersByParent = subfoldersByParent,
                    expandedMap = expandedMap,
                    onToggleFolder = onToggleFolder
                )
            }
        }
    }
}
