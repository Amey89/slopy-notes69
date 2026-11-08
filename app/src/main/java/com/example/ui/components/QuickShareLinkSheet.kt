package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FolderEntity
import com.example.data.model.TagEntity
import com.example.util.LinkMetadata
import com.example.util.LinkMetadataExtractor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickShareLinkSheet(
    sharedText: String,
    subject: String?,
    sourcePackage: String?,
    allTags: List<TagEntity>,
    onSaveAndContinueBrowsing: (
        resolvedMetadata: LinkMetadata?,
        customTitle: String,
        selectedTags: List<String>,
        targetSubfolderId: Long
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val instantPreview = remember(sharedText, subject, sourcePackage) {
        LinkMetadataExtractor.resolveInstantPreview(sharedText, subject, sourcePackage)
    }

    var resolvedMetadata by remember(sharedText) { mutableStateOf<LinkMetadata?>(null) }
    var isFetchingMetadata by remember(sharedText) { mutableStateOf(true) }
    var editableTitle by remember(sharedText) { mutableStateOf(instantPreview.title) }
    var userEditedTitle by remember(sharedText) { mutableStateOf(false) }
    var selectedSubfolderId by remember(sharedText) { mutableLongStateOf(instantPreview.subfolderId) }

    // Default ON: tapping a tag immediately assigns it, saves the link note, and returns to browsing!
    var autoSaveOnTagSelect by remember { mutableStateOf(true) }
    val selectedTags = remember(sharedText) {
        mutableStateListOf<String>().apply {
            addAll(instantPreview.suggestedTags)
        }
    }
    var customTagInput by remember { mutableStateOf("") }

    // Fetch full rich metadata immediately on open
    LaunchedEffect(sharedText, subject, sourcePackage) {
        isFetchingMetadata = true
        try {
            val fullMeta = LinkMetadataExtractor.resolve(sharedText, subject, sourcePackage)
            resolvedMetadata = fullMeta
            if (!userEditedTitle && fullMeta.title.isNotBlank()) {
                editableTitle = fullMeta.title
            }
            if (selectedSubfolderId == instantPreview.subfolderId) {
                selectedSubfolderId = fullMeta.subfolderId
            }
            fullMeta.suggestedTags.forEach { tag ->
                val clean = tag.trim().lowercase()
                if (clean.isNotBlank() && !selectedTags.contains(clean)) {
                    selectedTags.add(clean)
                }
            }
        } catch (_: Exception) {
            // Instant preview remains active
        } finally {
            isFetchingMetadata = false
        }
    }

    val activeMeta = resolvedMetadata ?: instantPreview
    val targetFolder = FolderEntity.getPermanentFolderById(selectedSubfolderId)
        ?: FolderEntity.PermanentChromeFolder

    // Combine user's existing tags with quick contextual tags for effortless 1-tap selection
    val availableQuickTags = remember(allTags, targetFolder.name) {
        val platformSlug = targetFolder.name.lowercase().replace(" (twitter)", "").replace(" ", "-")
        val baseNames = buildList {
            allTags.forEach { add(it.name.lowercase()) }
            add(platformSlug)
            add("watch-later")
            add("read-later")
            add("inspiration")
            add("bookmark")
            add("favorite")
            add("research")
            add("ideas")
        }.distinct()
        baseNames
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.testTag("quick_share_link_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2563EB).copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Quick Save Shared Link",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Shared Links ▸ ${targetFolder.name}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFetchingMetadata) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Live Metadata & Image Preview Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeMeta.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(activeMeta.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Link preview image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.06f)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    OutlinedTextField(
                        value = editableTitle,
                        onValueChange = {
                            editableTitle = it
                            userEditedTitle = true
                        },
                        label = { Text("Link Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (activeMeta.description.isNotBlank()) {
                        Text(
                            text = activeMeta.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = activeMeta.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2563EB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Target Permanent Subfolder Selector (Auto-detected under Shared Links)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SAVING TO SHARED LINKS SUBFOLDER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FolderEntity.PermanentSharedLinkSubfolders.forEach { subfolder ->
                        val isSelected = selectedSubfolderId == subfolder.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubfolderId = subfolder.id },
                            label = {
                                Text(
                                    text = subfolder.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 4. Tag Selection with 1-Tap Auto-Save & Continue Browsing
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Tap a Tag to Auto-Save & Continue Browsing",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (autoSaveOnTagSelect) {
                                        "Selecting any tag below immediately saves & returns you to your app"
                                    } else {
                                        "Multi-tag mode: select multiple tags, then tap Save below"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = autoSaveOnTagSelect,
                            onCheckedChange = { autoSaveOnTagSelect = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Selectable Tag Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableQuickTags.forEach { tagName ->
                            val isTagSelected = selectedTags.any { it.equals(tagName, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isTagSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isTagSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        val clean = tagName.trim().lowercase()
                                        if (autoSaveOnTagSelect) {
                                            if (!selectedTags.contains(clean)) {
                                                selectedTags.add(clean)
                                            }
                                            onSaveAndContinueBrowsing(
                                                resolvedMetadata,
                                                editableTitle,
                                                selectedTags.toList(),
                                                selectedSubfolderId
                                            )
                                        } else {
                                            if (selectedTags.contains(clean)) {
                                                selectedTags.remove(clean)
                                            } else {
                                                selectedTags.add(clean)
                                            }
                                        }
                                    }
                                    .testTag("quick_share_tag_$tagName")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isTagSelected) Icons.Default.Check else Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = if (isTagSelected) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "#$tagName",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isTagSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Inline custom tag input (pressing Done or Add also auto-saves if autoSaveOnTagSelect is ON)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customTagInput,
                            onValueChange = { customTagInput = it },
                            placeholder = { Text("Add custom #tag...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val clean = customTagInput.trim().removePrefix("#").lowercase()
                                    if (clean.isNotBlank()) {
                                        if (!selectedTags.contains(clean)) selectedTags.add(clean)
                                        customTagInput = ""
                                        if (autoSaveOnTagSelect) {
                                            onSaveAndContinueBrowsing(
                                                resolvedMetadata,
                                                editableTitle,
                                                selectedTags.toList(),
                                                selectedSubfolderId
                                            )
                                        }
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val clean = customTagInput.trim().removePrefix("#").lowercase()
                                if (clean.isNotBlank()) {
                                    if (!selectedTags.contains(clean)) selectedTags.add(clean)
                                    customTagInput = ""
                                    if (autoSaveOnTagSelect) {
                                        onSaveAndContinueBrowsing(
                                            resolvedMetadata,
                                            editableTitle,
                                            selectedTags.toList(),
                                            selectedSubfolderId
                                        )
                                    }
                                }
                            },
                            enabled = customTagInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (autoSaveOnTagSelect) "Tag & Save" else "Add", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 5. Primary Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onSaveAndContinueBrowsing(
                            resolvedMetadata,
                            editableTitle,
                            selectedTags.toList(),
                            selectedSubfolderId
                        )
                    },
                    modifier = Modifier
                        .weight(2f)
                        .testTag("quick_share_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Text(
                        text = "Save to ${targetFolder.name} & Continue",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
