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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

import com.example.data.model.TagEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagManager(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    customTags: List<TagEntity> = emptyList(),
    onCreateCustomTag: ((name: String, colorIndex: Int) -> Unit)? = null
) {
    var newTagInput by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    val defaultSuggestions = listOf("Work", "Personal", "Ideas", "Project", "Urgent", "Study", "Finance")

    if (showCreateDialog && onCreateCustomTag != null) {
        CreateTagDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, colorIndex ->
                onCreateCustomTag(name, colorIndex)
                onAddTag(name)
                showCreateDialog = false
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Active assigned tags
        if (tags.isNotEmpty() || !readOnly) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.forEach { tag ->
                    TagChip(
                        tag = tag,
                        onRemove = if (!readOnly) { { onRemoveTag(tag) } } else null
                    )
                }
            }
        }

        if (!readOnly) {
            // Reusable Custom Tags from Database
            val availableTags = customTags.ifEmpty {
                defaultSuggestions.mapIndexed { idx, name -> TagEntity(name = name, colorIndex = idx % TagPaletteColors.size) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Permanent Tags",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onCreateCustomTag != null) {
                        Text(
                            text = "+ New Tag",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showCreateDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableTags.forEach { tagEntity ->
                        val isAssigned = tags.any { it.equals(tagEntity.name, ignoreCase = true) }
                        val tagColor = TagPaletteColors.getOrElse(tagEntity.colorIndex) { MaterialTheme.colorScheme.primary }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAssigned) tagColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (isAssigned) tagColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.clickable {
                                if (isAssigned) {
                                    val match = tags.firstOrNull { it.equals(tagEntity.name, ignoreCase = true) }
                                    if (match != null) onRemoveTag(match)
                                } else {
                                    onAddTag(tagEntity.name)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(tagColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAssigned) "✓ #${tagEntity.name}" else "#${tagEntity.name}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAssigned) tagColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Custom manual tag text field
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("Quick type tag...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmed = newTagInput.trim().removePrefix("#")
                            if (trimmed.isNotBlank() && !tags.contains(trimmed)) {
                                onAddTag(trimmed)
                                onCreateCustomTag?.invoke(trimmed, (tags.size) % TagPaletteColors.size)
                                newTagInput = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        val trimmed = newTagInput.trim().removePrefix("#")
                        if (trimmed.isNotBlank() && !tags.contains(trimmed)) {
                            onAddTag(trimmed)
                            onCreateCustomTag?.invoke(trimmed, (tags.size) % TagPaletteColors.size)
                            newTagInput = ""
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add tag",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private val TagChipShape = RoundedCornerShape(10.dp)
private val TagAccentPalette = listOf(
    Color(0xFF4F46E5), // Indigo
    Color(0xFF0D9488), // Teal
    Color(0xFFD97706), // Amber
    Color(0xFFE11D48), // Rose
    Color(0xFF7C3AED), // Purple
    Color(0xFF2563EB), // Blue
    Color(0xFF059669)  // Emerald
)

fun getTagAccentColor(tag: String): Color {
    val index = abs(tag.hashCode()) % TagAccentPalette.size
    return TagAccentPalette[index]
}

@Composable
fun TagChip(
    tag: String,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tagColor = remember(tag) { getTagAccentColor(tag) }

    Surface(
        shape = TagChipShape,
        color = tagColor.copy(alpha = 0.16f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(tagColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "#$tag",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = tagColor
            )
            if (onRemove != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onRemove() },
                    tint = tagColor
                )
            }
        }
    }
}
