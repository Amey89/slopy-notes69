package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RichTextFormattingToolbar(
    textValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FormatIconButton(
                icon = Icons.Default.FormatBold,
                tooltip = "Bold",
                onClick = { onValueChange(applyWrapping(textValue, "**", "**")) }
            )
            FormatIconButton(
                icon = Icons.Default.FormatItalic,
                tooltip = "Italic",
                onClick = { onValueChange(applyWrapping(textValue, "*", "*")) }
            )
            FormatIconButton(
                icon = Icons.Default.FormatStrikethrough,
                tooltip = "Strikethrough",
                onClick = { onValueChange(applyWrapping(textValue, "~~", "~~")) }
            )
            FormatIconButton(
                icon = Icons.Default.Title,
                tooltip = "Heading 1",
                label = "H1",
                onClick = { onValueChange(applyLinePrefix(textValue, "# ")) }
            )
            FormatIconButton(
                icon = Icons.Default.Title,
                tooltip = "Heading 2",
                label = "H2",
                onClick = { onValueChange(applyLinePrefix(textValue, "## ")) }
            )
            FormatIconButton(
                icon = Icons.Default.FormatListBulleted,
                tooltip = "Bullet List",
                onClick = { onValueChange(applyLinePrefix(textValue, "• ")) }
            )
            FormatIconButton(
                icon = Icons.Default.CheckBox,
                tooltip = "Checkbox item",
                onClick = { onValueChange(applyLinePrefix(textValue, "- [ ] ")) }
            )
            FormatIconButton(
                icon = Icons.Default.FormatQuote,
                tooltip = "Quote",
                onClick = { onValueChange(applyLinePrefix(textValue, "> ")) }
            )
            FormatIconButton(
                icon = Icons.Default.Code,
                tooltip = "Inline Code",
                onClick = { onValueChange(applyWrapping(textValue, "`", "`")) }
            )
        }
    }
}

@Composable
private fun FormatIconButton(
    icon: ImageVector,
    tooltip: String,
    label: String? = null,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        if (label != null) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else {
            Icon(icon, contentDescription = tooltip, modifier = Modifier.size(20.dp))
        }
    }
}

private fun applyWrapping(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    return if (selection.start != selection.end) {
        val selectedText = text.substring(selection.start, selection.end)
        val newText = text.replaceRange(selection.start, selection.end, "$prefix$selectedText$suffix")
        TextFieldValue(
            text = newText,
            selection = TextRange(selection.start + prefix.length, selection.end + prefix.length)
        )
    } else {
        val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.start)
        TextFieldValue(
            text = newText,
            selection = TextRange(selection.start + prefix.length)
        )
    }
}

private fun applyLinePrefix(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val cursor = value.selection.start
    val startOfLine = text.lastIndexOf('\n', cursor - 1) + 1
    val newText = text.substring(0, startOfLine) + prefix + text.substring(startOfLine)
    return TextFieldValue(
        text = newText,
        selection = TextRange(cursor + prefix.length)
    )
}

/**
 * RichTextViewer renders formatted markdown notes gracefully with support for:
 * - H1, H2 Headings
 * - Interactive checklists (- [ ] / - [x])
 * - Bullet lists (• or -)
 * - Blockquotes
 * - Inline bold, italics, code
 */
@Composable
fun RichTextViewer(
    markdownContent: String,
    modifier: Modifier = Modifier,
    onChecklistToggle: ((updatedContent: String) -> Unit)? = null
) {
    val lines = markdownContent.lines()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEachIndexed { lineIndex, rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") -> {
                    val isChecked = trimmed.startsWith("- [x] ")
                    val itemText = trimmed.substring(6)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (onChecklistToggle != null) {
                                    val newPrefix = if (checked) "- [x] " else "- [ ] "
                                    val newLines = lines.toMutableList()
                                    newLines[lineIndex] = newPrefix + itemText
                                    onChecklistToggle(newLines.joinToString("\n"))
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(itemText),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                trimmed.startsWith("• ") || trimmed.startsWith("- ") -> {
                    val bulletText = if (trimmed.startsWith("• ")) trimmed.removePrefix("• ") else trimmed.removePrefix("- ")
                    Row(
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(bulletText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(trimmed.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                trimmed.isBlank() -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(rawLine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Basic markdown parser converting **bold**, *italic*, ~~strike~~, `code` into AnnotatedString
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        val boldText = text.substring(i + 2, end)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(boldText)
                        pop()
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        val strikeText = text.substring(i + 2, end)
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(strikeText)
                        pop()
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        val codeText = text.substring(i + 1, end)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x22808080)
                            )
                        )
                        append(codeText)
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        val italicText = text.substring(i + 1, end)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(italicText)
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
