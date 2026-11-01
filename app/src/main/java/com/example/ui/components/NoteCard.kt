package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircleFilled
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

// Top-level static constants to prevent object allocations during 120Hz fast scrolling
private val NoteCardShape = RoundedCornerShape(26.dp)
private val SharedLinkBadgeShape = RoundedCornerShape(12.dp)
private val HeroImageShape = RoundedCornerShape(18.dp)
private val ThumbnailShape = RoundedCornerShape(14.dp)
private val UrlPillShape = RoundedCornerShape(50)
private val ActionPillShape = RoundedCornerShape(14.dp)
private val SharedLinkBorder = BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.55f))

private val CLEAN_MARKDOWN_REGEX = Regex("[#*`_~>\\[\\]]")

/**
 * 120Hz Smooth Scrolling Optimized NoteCard — Material 3 Expressive Edition.
 * Uses pre-computed [NoteUiItem] where all string splitting, regex parsing,
 * URL extraction, and date formatting are done off the main thread.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    uiItem: NoteUiItem,
    onClick: (NoteEntity) -> Unit,
    onToggleStar: (NoteEntity) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    performanceMode: Boolean = false
) {
    val note = uiItem.note
    val isDark = isSystemInDarkTheme()
    val palette = if (isDark) NoteAccentColorsDark else NoteAccentColorsLight
    val cardBg = palette.getOrElse(note.colorIndex) { MaterialTheme.colorScheme.surface }
    val context = LocalContext.current

    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardBorder = remember(uiItem.isSharedLinkNote, note.isStarred, outlineVariantColor, primaryColor) {
        when {
            uiItem.isSharedLinkNote -> SharedLinkBorder
            note.isStarred -> BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.55f))
            else -> BorderStroke(1.dp, outlineVariantColor.copy(alpha = 0.55f))
        }
    }

    val onCardClick = remember(onClick, note) { { onClick(note) } }
    val onStarClick = remember(onToggleStar, note) { { onToggleStar(note) } }
    val onDeleteClick = remember(onDelete, note.id) { { onDelete(note.id) } }

    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = NoteCardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (performanceMode) 0.dp else 1.dp),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Title (with Link badge if shared) + Expressive Tonal Star & Delete Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiItem.isSharedLinkNote) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.16f), SharedLinkBadgeShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = uiItem.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onStarClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (note.isStarred) Color(0xFFF59E0B).copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ActionPillShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (note.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (note.isStarred) "Unstar" else "Star",
                                tint = if (note.isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ActionPillShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Rich Shared Link Hero Thumbnail
            if (uiItem.isSharedLinkNote && uiItem.imageList.isNotEmpty()) {
                val heroUrl = uiItem.imageList.first()
                val heroImageRequest = remember(heroUrl, performanceMode, context) {
                    ImageRequest.Builder(context)
                        .data(if (heroUrl.startsWith("http")) heroUrl else File(heroUrl))
                        .crossfade(!performanceMode)
                        .build()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(HeroImageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    AsyncImage(
                        model = heroImageRequest,
                        contentDescription = "Link thumbnail preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (uiItem.tagsList.contains("video")) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Video",
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            // Note Content Snippet (Pre-computed in ViewModel)
            if (uiItem.cleanPreview.isNotBlank()) {
                Text(
                    text = uiItem.cleanPreview,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    ),
                    maxLines = if (uiItem.isSharedLinkNote && uiItem.imageList.isNotEmpty()) 2 else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Standard Note Image Thumbnails
            if (!uiItem.isSharedLinkNote && uiItem.imageList.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayedImages = remember(uiItem.imageList) { uiItem.imageList.take(3) }
                    displayedImages.forEach { imgPath ->
                        val imageRequest = remember(imgPath, context) {
                            val model = if (imgPath.startsWith("http")) imgPath else File(imgPath)
                            ImageRequest.Builder(context)
                                .data(model)
                                .size(168, 168)
                                .crossfade(false)
                                .build()
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(ThumbnailShape)
                        ) {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = "Attached thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    if (uiItem.imageList.size > 3) {
                        Surface(
                            shape = ThumbnailShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+${uiItem.imageList.size - 3}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Clickable URL Pill for Shared Links
            if (uiItem.extractedUrl != null && uiItem.displayUrl != null) {
                Surface(
                    shape = UrlPillShape,
                    color = Color(0xFF2563EB).copy(alpha = 0.12f),
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiItem.extractedUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = uiItem.displayUrl,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D4ED8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in browser",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Tags (Single-pass row layout to keep scrolling locked at 120 FPS)
            if (uiItem.tagsList.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayedTags = remember(uiItem.tagsList) { uiItem.tagsList.take(3) }
                    displayedTags.forEach { tag ->
                        TagChip(tag = tag)
                    }
                    if (uiItem.tagsList.size > 3) {
                        Text(
                            text = "+${uiItem.tagsList.size - 3}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Footer: Date (Pre-formatted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiItem.formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (uiItem.imageList.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${uiItem.imageList.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible overload for NoteCard directly taking [NoteEntity].
 * Wraps local computations in remember blocks to protect scrolling performance.
 */
@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    performanceMode: Boolean = false
) {
    val isSharedLink = note.folderId == FolderEntity.SHARED_LINKS_FOLDER_ID
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
            note.content.lineSequence().firstOrNull { it.trimStart().startsWith("http://") || it.trimStart().startsWith("https://") }?.trim()
        } else null
    }
    val displayUrl = remember(extractedUrl) {
        extractedUrl?.removePrefix("https://")?.removePrefix("http://")?.take(42)
    }
    val displayTitle = remember(note.title, isSharedLink) {
        note.title.ifBlank { if (isSharedLink) "Shared Link" else "Untitled Note" }
    }
    val cleanPreview = remember(note.content, isSharedLink, performanceMode) {
        if (note.content.isBlank()) ""
        else {
            var text = note.content
            if (isSharedLink) {
                val firstHttp = text.indexOf("http")
                if (firstHttp != -1) {
                    val endOfLine = text.indexOf('\n', firstHttp)
                    text = if (endOfLine != -1) text.substring(0, firstHttp) + text.substring(endOfLine + 1)
                    else text.substring(0, firstHttp)
                }
            }
            val snippet = if (text.length > 280) text.substring(0, 280) else text
            if (performanceMode) snippet.trim() else CLEAN_MARKDOWN_REGEX.replace(snippet, "").trim()
        }
    }
    val formattedDate = remember(note.updatedAt) {
        AppDateFormatter.format(note.updatedAt)
    }

    val uiItem = remember(note, displayTitle, formattedDate, cleanPreview, imageList, tagsList, extractedUrl, displayUrl, isSharedLink) {
        NoteUiItem(
            note = note,
            displayTitle = displayTitle,
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
        performanceMode = performanceMode
    )
}
