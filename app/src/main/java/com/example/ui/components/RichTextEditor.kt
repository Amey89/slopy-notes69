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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Matches all markdown checklist variations:
 * - [ ] task
 * - [] task
 * - []task
 * - [x] task / - [X] task
 * * [ ] task / • [ ] task
 * [ ] task / [] task / [x] task
 */
val CHECKLIST_LINE_REGEX = Regex("^(\\s*(?:[-*•]\\s*)?)\\[([ xX]?)\\]\\s*(.*)$")

private val LEADING_MARKDOWN_PREFIX_REGEX = Regex("^(\\s*(?:[-*•]\\s*\\[[ xX]?\\]\\s*|\\[[ xX]?\\]\\s*|#{1,6}\\s+|[-*•]\\s+|1\\.\\s+))+")

fun stripLeadingMarkdownPrefix(raw: String): String {
    val cleaned = LEADING_MARKDOWN_PREFIX_REGEX.replace(raw.trim(), "").trim()
    return cleaned.ifBlank { raw.trim() }
}

/**
 * Compact, single-row horizontally scrollable formatting bar.
 */
@Composable
fun RichTextFormattingToolbar(
    textValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatActionChip(
                icon = Icons.Default.Title,
                label = "H1",
                onClick = { onValueChange(insertLinePrefix(textValue, "# ")) }
            )
            FormatActionChip(
                icon = Icons.Default.Title,
                label = "H2",
                onClick = { onValueChange(insertLinePrefix(textValue, "## ")) }
            )
            FormatActionChip(
                icon = Icons.Default.FormatBold,
                label = "Bold",
                onClick = { onValueChange(wrapSelection(textValue, "**", "**", "bold")) }
            )
            FormatActionChip(
                icon = Icons.Default.FormatItalic,
                label = "Italic",
                onClick = { onValueChange(wrapSelection(textValue, "*", "*", "italic")) }
            )
            FormatActionChip(
                icon = Icons.Default.FormatStrikethrough,
                label = "Strike",
                onClick = { onValueChange(wrapSelection(textValue, "~~", "~~", "strike")) }
            )
            FormatActionChip(
                icon = Icons.Default.FormatListBulleted,
                label = "Bullet",
                onClick = { onValueChange(insertLinePrefix(textValue, "• ")) }
            )
            FormatActionChip(
                icon = Icons.Default.CheckBox,
                label = "Check",
                onClick = { onValueChange(insertLinePrefix(textValue, "- [ ] ")) }
            )
            FormatActionChip(
                icon = Icons.Default.FormatQuote,
                label = "Quote",
                onClick = { onValueChange(insertLinePrefix(textValue, "> ")) }
            )
            FormatActionChip(
                icon = Icons.Default.Code,
                label = "Code",
                onClick = { onValueChange(wrapSelection(textValue, "`", "`", "code")) }
            )
        }
    }
}

@Composable
private fun FormatActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun wrapSelection(
    tfv: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String
): TextFieldValue {
    val text = tfv.text
    val start = tfv.selection.min.coerceIn(0, text.length)
    val end = tfv.selection.max.coerceIn(0, text.length)

    return if (start != end) {
        val selected = text.substring(start, end)
        val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
        TextFieldValue(
            text = newText,
            selection = TextRange(start + prefix.length, end + prefix.length)
        )
    } else {
        val insert = prefix + placeholder + suffix
        val newText = text.substring(0, start) + insert + text.substring(end)
        TextFieldValue(
            text = newText,
            selection = TextRange(start + prefix.length, start + prefix.length + placeholder.length)
        )
    }
}

private fun insertLinePrefix(tfv: TextFieldValue, prefix: String): TextFieldValue {
    val text = tfv.text
    val cursor = tfv.selection.min.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }

    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return TextFieldValue(
        text = newText,
        selection = TextRange(cursor + prefix.length)
    )
}

/**
 * Interactive Rich Markdown Renderer for both full Note Preview and Note Cards on the Home screen.
 * Supports H1/H2/H3, Bold, Italic, Strikethrough, Inline Code, Quotes, Bullets, and Interactive Checklists
 * (including `- [ ]`, `- []`, `- [x]`, `[ ]`, `[x]`).
 */
@Composable
fun RichTextViewer(
    markdownContent: String,
    onChecklistToggle: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    compactCardMode: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    skipUrls: Boolean = false
) {
    val allLines = markdownContent.split("\n")
    val indexedVisibleLines = allLines.mapIndexedNotNull { index, line ->
        val trimmed = line.trim()
        if (skipUrls && (trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            null
        } else if (compactCardMode && trimmed.isBlank()) {
            null
        } else {
            index to line
        }
    }

    val displayedLines = if (maxLines < Int.MAX_VALUE) {
        indexedVisibleLines.take(maxLines)
    } else {
        indexedVisibleLines
    }
    val hiddenCount = (indexedVisibleLines.size - displayedLines.size).coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compactCardMode) 5.dp else 6.dp)
    ) {
        displayedLines.forEach { (originalIndex, line) ->
            val trimmed = line.trim()
            val checklistMatch = CHECKLIST_LINE_REGEX.matchEntire(trimmed)
            when {
                checklistMatch != null -> {
                    val isChecked = checklistMatch.groupValues[2].equals("x", ignoreCase = true)
                    val itemText = checklistMatch.groupValues[3].trim()

                    val toggleAction = {
                        if (onChecklistToggle != null) {
                            val newLines = allLines.toMutableList()
                            newLines[originalIndex] = if (!isChecked) "- [x] $itemText" else "- [ ] $itemText"
                            onChecklistToggle(newLines.joinToString("\n"))
                        }
                    }

                    if (compactCardMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = onChecklistToggle != null) { toggleAction() }
                                .padding(vertical = 3.dp, horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        color = if (isChecked) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = if (isChecked) 0.dp else 1.5.dp,
                                        color = if (isChecked) Color.Transparent
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Checked",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(itemText),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = onChecklistToggle != null) { toggleAction() }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = if (onChecklistToggle != null) {
                                    { toggleAction() }
                                } else null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = parseInlineMarkdown(itemText),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ")),
                        style = if (compactCardMode) {
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ")),
                        style = if (compactCardMode) {
                            MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val bulletText = trimmed.drop(2).trimStart()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(bulletText),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = if (compactCardMode) 2 else Int.MAX_VALUE,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                trimmed.startsWith("> ") -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = parseInlineMarkdown(trimmed.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = if (compactCardMode) 2 else Int.MAX_VALUE,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (compactCardMode) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        ),
                        maxLines = if (compactCardMode) 3 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (compactCardMode && hiddenCount > 0) {
            Text(
                text = "+$hiddenCount more line${if (hiddenCount > 1) "s" else ""}…",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun parseInlineMarkdown(input: String): AnnotatedString {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    return buildAnnotatedString {
        var i = 0
        val n = input.length
        while (i < n) {
            when {
                input.startsWith("**", i) -> {
                    val close = input.indexOf("**", i + 2)
                    if (close != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(input.substring(i + 2, close))
                        pop()
                        i = close + 2
                    } else {
                        append(input[i])
                        i++
                    }
                }
                input.startsWith("~~", i) -> {
                    val close = input.indexOf("~~", i + 2)
                    if (close != -1) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(input.substring(i + 2, close))
                        pop()
                        i = close + 2
                    } else {
                        append(input[i])
                        i++
                    }
                }
                input.startsWith("`", i) -> {
                    val close = input.indexOf("`", i + 1)
                    if (close != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBg,
                                color = primaryColor
                            )
                        )
                        append(" " + input.substring(i + 1, close) + " ")
                        pop()
                        i = close + 1
                    } else {
                        append(input[i])
                        i++
                    }
                }
                input.startsWith("*", i) -> {
                    val close = input.indexOf("*", i + 1)
                    if (close != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(input.substring(i + 1, close))
                        pop()
                        i = close + 1
                    } else {
                        append(input[i])
                        i++
                    }
                }
                else -> {
                    append(input[i])
                    i++
                }
            }
        }
    }
}
