package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.util.LinkMetadata
import com.example.util.LinkMetadataExtractor
import kotlinx.coroutines.launch

@Composable
fun AddLinkModalDialog(
    onDismiss: () -> Unit,
    onSaveLink: (url: String, title: String, description: String, imageUrl: String, tags: String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf("") }
    var extractedTitle by remember { mutableStateOf("") }
    var extractedDescription by remember { mutableStateOf("") }
    var extractedImageUrl by remember { mutableStateOf("") }
    var extractedTags by remember { mutableStateOf("links") }

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

        scope.launch {
            try {
                val metadata = LinkMetadataExtractor.resolve(cleanUrl)
                detectedMetadata = metadata
                if (extractedTitle.isBlank()) extractedTitle = metadata.title
                if (extractedDescription.isBlank()) extractedDescription = metadata.description
                if (extractedImageUrl.isBlank()) extractedImageUrl = metadata.imageUrl

                val autoTags = buildList {
                    add("links")
                    if (metadata.domain.isNotBlank()) add(metadata.domain.lowercase())
                    if (metadata.isYouTube) add("video")
                    if (metadata.domain.contains("instagram")) add("insta")
                    if (metadata.domain.contains("pinterest") || metadata.domain.contains("pin.it")) add("pinterest")
                    if (metadata.domain.contains("facebook")) add("facebook")
                }.distinct().joinToString(", ")
                extractedTags = autoTags
            } catch (e: Exception) {
                errorMessage = "Could not fetch remote details (${e.localizedMessage ?: "network issue"}). You can still save manually."
            } finally {
                isFetching = false
            }
        }
    }

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
                Text(
                    text = "Save Link with Metadata",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
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
                    text = "Extracts high-resolution images, video thumbnails, and titles from YouTube Shorts, Instagram, Facebook, Pinterest & web pages.",
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "METADATA & IMAGE PREVIEW",
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
                            extractedTitle.ifBlank { "Shared Link" },
                            extractedDescription,
                            extractedImageUrl,
                            extractedTags.ifBlank { "links" }
                        )
                    }
                },
                enabled = inputUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save to Shared Links", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
