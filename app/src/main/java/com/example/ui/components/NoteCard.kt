package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.ui.model.NoteUiItem
import com.example.ui.theme.NoteAccentColorsDark
import com.example.ui.theme.NoteAccentColorsLight
import java.io.File

// Top-level static shapes to avoid runtime allocations on every LazyColumn item scroll
private val NoteCardShape = RoundedCornerShape(22.dp)
private val ActionButtonPillShape = RoundedCornerShape(12.dp)
private val TagPillShape = RoundedCornerShape(50)
private val LinkPillShape = RoundedCornerShape(12.dp)
private val HeroImageShape = RoundedCornerShape(16.dp)
private val ThumbImageShape = RoundedCornerShape(12.dp)

private val CLEAN_MARKDOWN_REGEX = Regex("[#*`_~>\\[\\]]")

/**
 * 120Hz Smooth Scrolling Optimized NoteCard — Distraction-Free Neutral Edition
 * with Interactive Markdown & Checkboxes directly inside the card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    uiItem: NoteUiItem,
    onClick: (NoteEntity) -> Unit,
    onToggleStar: (NoteEntity) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    performanceMode: Boolean = false,
    onChecklistToggle: ((NoteEntity, String) -> Unit)? = null
) {
    val note = uiItem.note
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val palette = if (isDark) NoteAccentColorsDark else NoteAccentColorsLight
    val cardBgColor = if (note.colorIndex == 0) {
        MaterialTheme.colorScheme.surface
    } else {
        palette.getOrElse(note.colorIndex) { MaterialTheme.colorScheme.surface }
    }

    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val outline = MaterialTheme.colorScheme.outline
    val cardBorder = remember(note.isStarred, outlineVariant, outline) {
        if (note.isStarred) {
            BorderStroke(1.2.dp, outline.copy(alpha = 0.8f))
        } else {
            BorderStroke(1.dp, outlineVariant.copy(alpha = 0.6f))
        }
    }

    val onCardClick = remember(onClick, note) { { onClick(note) } }
    val onStarClick = remember(onToggleStar, note) { { onToggleStar(note) } }
    val onDeleteClick = remember(onDelete, note.id) { { onDelete(note.id) } }

    val cleanTitle = remember(uiItem.displayTitle) {
        stripLeadingMarkdownPrefix(uiItem.displayTitle)
    }

    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = NoteCardShape,
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Title + Calm Neutral Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onStarClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (note.isStarred) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ActionButtonPillShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (note.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (note.isStarred) "Unstar note" else "Star note",
                                tint = if (note.isStarred) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ActionButtonPillShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Shared Link Hero Image Preview (disabled in High-Performance Mode for 10,000+ item speed)
            val imageList = uiItem.imageList
            val isSharedLinkNote = uiItem.isSharedLinkNote
            val extractedUrl = uiItem.extractedUrl

            if (!performanceMode && imageList.isNotEmpty() && isSharedLinkNote) {
                val heroPath = imageList.first()
                val modelData = remember(heroPath) {
                    if (heroPath.startsWith("http")) heroPath else File(heroPath)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(HeroImageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(modelData)
                            .crossfade(false)
                            .size(600, 320)
                            .build(),
                        contentDescription = "Link preview image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Open Shared Link Button Pill (Neutral Distraction-Free Style)
            if (extractedUrl != null && uiItem.displayUrl != null) {
                Surface(
                    shape = LinkPillShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LinkPillShape)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(extractedUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiItem.displayUrl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Rich Markdown & Interactive Checkboxes directly in NoteCard!
            if (note.content.isNotBlank()) {
                if (performanceMode) {
                    if (uiItem.cleanPreview.isNotBlank()) {
                        Text(
                            text = uiItem.cleanPreview,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            ),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    RichTextViewer(
                        markdownContent = note.content,
                        onChecklistToggle = if (onChecklistToggle != null) {
                            { updatedContent -> onChecklistToggle(note, updatedContent) }
                        } else null,
                        compactCardMode = true,
                        maxLines = 8,
                        skipUrls = isSharedLinkNote
                    )
                }
            }

            // Thumbnail Strip for Regular Notes (Non-Scrolling Row to avoid nested HorizontalScroll lag)
            if (!performanceMode && imageList.isNotEmpty() && !isSharedLinkNote) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayedThumbs = imageList.take(4)
                    val extraCount = imageList.size - displayedThumbs.size
                    displayedThumbs.forEach { path ->
                        val modelData = remember(path) {
                            if (path.startsWith("http")) path else File(path)
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(ThumbImageShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    ThumbImageShape
                                )
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(modelData)
                                    .crossfade(false)
                                    .size(160, 160)
                                    .build(),
                                contentDescription = "Note attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    if (extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(ThumbImageShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$extraCount",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Tags + Timestamp Footer Row
            val tagsList = uiItem.tagsList
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (tagsList.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val visibleTags = tagsList.take(3)
                        visibleTags.forEach { tag ->
                            Surface(
                                shape = TagPillShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Text(
                    text = uiItem.formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * Backward-compatible overload for NoteCard directly taking [NoteEntity].
 */
@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    performanceMode: Boolean = false,
    onChecklistToggle: ((String) -> Unit)? = null
) {
    val isSharedLink = FolderEntity.isSharedLinksFamilyId(note.folderId)
    val imageList = remember(note.imageUris) {
        if (note.imageUris.isBlank()) emptyList()
        else note.imageUris.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val tagsList = remember(note.tags) {
        if (note.tags.isBlank()) emptyList()
        else note.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val extractedUrl = remember(note.content, isSharedLink, tagsList) {
        if (isSharedLink || tagsList.contains("link")) {
            note.content.lineSequence().firstOrNull {
                val t = it.trim()
                t.startsWith("http://") || t.startsWith("https://")
            }?.trim()
        } else null
    }
    val displayUrl = remember(extractedUrl) {
        extractedUrl?.removePrefix("https://")?.removePrefix("http://")?.take(42)
    }
    val cleanPreview = remember(note.content, isSharedLink, extractedUrl, performanceMode) {
        if (note.content.isBlank()) ""
        else {
            val raw = if (isSharedLink && extractedUrl != null) {
                note.content.replaceFirst(extractedUrl, "").trim()
            } else note.content
            val snippet = raw.take(280)
            if (performanceMode) snippet.trim() else snippet.replace(CLEAN_MARKDOWN_REGEX, "").trim()
        }
    }
    val formattedDate = remember(note.updatedAt) {
        AppDateFormatter.format(note.updatedAt)
    }
    val uiItem = remember(note, imageList, tagsList, extractedUrl, displayUrl, cleanPreview, formattedDate, isSharedLink) {
        NoteUiItem(
            note = note,
            displayTitle = note.title.ifBlank { if (isSharedLink) "Shared Link" else "Untitled Note" },
            formattedDate = formattedDate,
            cleanPreview = cleanPreview,
            imageList = imageList,
            tagsList = tagsList,
            extractedUrl = extractedUrl,
            displayUrl = displayUrl,
            isSharedLinkNote = isSharedLink
        )
    }

    NoteCard(
        uiItem = uiItem,
        onClick = { onClick() },
        onToggleStar = { onToggleStar() },
        onDelete = { onDelete() },
        modifier = modifier,
        performanceMode = performanceMode,
        onChecklistToggle = if (onChecklistToggle != null) {
            { _, updated -> onChecklistToggle(updated) }
        } else null
    )
}
