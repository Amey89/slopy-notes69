package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FolderEntity
import com.example.util.LinkMetadata
import com.example.util.LinkMetadataExtractor
import kotlinx.coroutines.launch

@Composable
fun AddLinkModalDialog(
    onDismiss: () -> Unit,
    onSaveLink: (url: String, title: String, description: String, imageUrl: String, tags: String, subfolderId: Long) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf("") }
    var extractedTitle by remember { mutableStateOf("") }
    var extractedDescription by remember { mutableStateOf("") }
    var extractedImageUrl by remember { mutableStateOf("") }
    var extractedTags by remember { mutableStateOf("link") }
    var selectedSubfolderId by remember { mutableLongStateOf(FolderEntity.CHROME_SUBFOLDER_ID) }

    var isFetching by remember { mutableStateOf(false) }
    var detectedMetadata by remember { mutableStateOf<LinkMetadata?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun runExtraction(urlToExtract: String) {
        val cleanUrl = LinkMetadataExtractor.extractUrl(urlToExtract) ?: urlToExtract.trim()
        if (cleanUrl.isBlank()) {
            errorMessage = "Please enter a valid web link or text."
            return
        }

        errorMessage = null
        isFetching = true

        // Instant subfolder & preview detection
        val instant = LinkMetadataExtractor.resolveInstantPreview(urlToExtract)
        selectedSubfolderId = instant.subfolderId

        scope.launch {
            try {
                val metadata = LinkMetadataExtractor.resolve(urlToExtract)
                detectedMetadata = metadata
                selectedSubfolderId = metadata.subfolderId
                if (extractedTitle.isBlank()) extractedTitle = metadata.title
                if (extractedDescription.isBlank()) extractedDescription = metadata.description
                if (extractedImageUrl.isBlank()) extractedImageUrl = metadata.imageUrl
                extractedTags = metadata.suggestedTags.joinToString(", ")
            } catch (e: Exception) {
                errorMessage = "Could not fetch remote details (${e.localizedMessage ?: "network issue"}). You can still save manually."
            } finally {
                isFetching = false
            }
        }
    }

    val targetSubfolder = FolderEntity.getPermanentFolderById(selectedSubfolderId)
        ?: FolderEntity.PermanentChromeFolder

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Save Link with Metadata",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Shared Links ▸ ${targetSubfolder.name}",
                        fontSize = 12.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Supports YouTube, Firefox, Chrome, Porn, TikTok, Twitch, Facebook, Instagram, X (Twitter), Threads, Pinterest, Snapchat, Reddit & Discord.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // URL Input & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = {
                            inputUrl = it
                            errorMessage = null
                            if (it.isNotBlank()) {
                                val autoFolder = FolderEntity.resolvePlatformSubfolder(it, it)
                                selectedSubfolderId = autoFolder.id
                            }
                        },
                        label = { Text("Link URL or text") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            cursorColor = Color(0xFF2563EB)
                        )
                    )

                    Button(
                        onClick = { runExtraction(inputUrl) },
                        enabled = !isFetching && inputUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Fetch", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Quick Paste button if clipboard has text
                val clipText = clipboardManager.getText()?.text
                if (!clipText.isNullOrBlank() && clipText != inputUrl && (clipText.startsWith("http") || clipText.contains("http"))) {
                    OutlinedButton(
                        onClick = {
                            inputUrl = clipText
                            runExtraction(clipText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📋 Paste from Clipboard & Fetch", fontSize = 12.sp)
                    }
                }

                // Platform Subfolder Selector
                Text(
                    text = "Shared Links Subfolder (Auto-detected):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FolderEntity.PermanentSharedLinkSubfolders.forEach { sub ->
                        val isSelected = selectedSubfolderId == sub.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubfolderId = sub.id },
                            label = { Text(sub.name, fontSize = 11.sp) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Preview Card
                if (detectedMetadata != null || extractedImageUrl.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "METADATA & IMAGE PREVIEW (${targetSubfolder.name.uppercase()})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (extractedImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(extractedImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Thumbnail preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.05f)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (extractedTitle.isNotBlank()) {
                                Text(
                                    text = extractedTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (extractedDescription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = extractedDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Editable Title
                OutlinedTextField(
                    value = extractedTitle,
                    onValueChange = { extractedTitle = it },
                    label = { Text("Title (Auto-filled or Custom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Editable Notes / Description
                OutlinedTextField(
                    value = extractedDescription,
                    onValueChange = { extractedDescription = it },
                    label = { Text("Notes / Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Editable Image URL
                OutlinedTextField(
                    value = extractedImageUrl,
                    onValueChange = { extractedImageUrl = it },
                    label = { Text("Thumbnail Image URL") },
                    placeholder = { Text("https://... image URL") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags
                OutlinedTextField(
                    value = extractedTags,
                    onValueChange = { extractedTags = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUrl = LinkMetadataExtractor.extractUrl(inputUrl) ?: inputUrl.trim()
                    if (finalUrl.isNotBlank()) {
                        onSaveLink(
                            finalUrl,
                            extractedTitle.ifBlank { "${targetSubfolder.name} Link" },
                            extractedDescription,
                            extractedImageUrl,
                            extractedTags.ifBlank { "link" },
                            selectedSubfolderId
                        )
                    }
                },
                enabled = inputUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save to ${targetSubfolder.name}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
