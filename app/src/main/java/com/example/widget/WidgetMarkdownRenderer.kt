package com.example.widget

import android.text.Html
import android.text.Spanned
import com.example.data.model.SubTaskEntity

object WidgetMarkdownRenderer {

    fun renderMarkdownToSpanned(markdown: String): Spanned {
        if (markdown.isBlank()) {
            return Html.fromHtml("(Empty note)", Html.FROM_HTML_MODE_COMPACT)
        }

        val lines = markdown.lines()
        val formattedLines = lines.map { rawLine ->
            val trimmed = rawLine.trim()
            when {
                // Completed Checkbox: - [x], -[x], - [X], -[X]
                trimmed.startsWith("- [x] ", ignoreCase = true) ||
                trimmed.startsWith("-[x] ", ignoreCase = true) ||
                trimmed.startsWith("- [x]", ignoreCase = true) ||
                trimmed.startsWith("-[x]", ignoreCase = true) -> {
                    val closingBracketIndex = trimmed.indexOf(']')
                    val text = if (closingBracketIndex >= 0 && closingBracketIndex + 1 < trimmed.length) {
                        trimmed.substring(closingBracketIndex + 1).trim()
                    } else ""
                    "☑ <s>${Html.escapeHtml(text)}</s>"
                }

                // Incomplete Checkbox: - [ ], -[ ], - [ ], -[]
                trimmed.startsWith("- [ ] ") ||
                trimmed.startsWith("-[ ] ") ||
                trimmed.startsWith("- [ ]") ||
                trimmed.startsWith("-[ ]") ||
                trimmed.startsWith("-[] ") ||
                trimmed.startsWith("-[]") -> {
                    val closingBracketIndex = trimmed.indexOf(']')
                    val text = if (closingBracketIndex >= 0 && closingBracketIndex + 1 < trimmed.length) {
                        trimmed.substring(closingBracketIndex + 1).trim()
                    } else ""
                    "☐ ${Html.escapeHtml(text)}"
                }

                // Headings
                trimmed.startsWith("### ") -> "<b>${Html.escapeHtml(trimmed.removePrefix("### ").trim())}</b>"
                trimmed.startsWith("## ") -> "<b><big>${Html.escapeHtml(trimmed.removePrefix("## ").trim())}</big></b>"
                trimmed.startsWith("# ") -> "<b><big><big>${Html.escapeHtml(trimmed.removePrefix("# ").trim())}</big></big></b>"

                // Bullet Lists
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val text = trimmed.substring(2).trim()
                    "• ${formatInlineStyles(Html.escapeHtml(text))}"
                }

                else -> formatInlineStyles(Html.escapeHtml(rawLine))
            }
        }

        val fullHtml = formattedLines.joinToString("<br/>")
        return Html.fromHtml(fullHtml, Html.FROM_HTML_MODE_COMPACT)
    }

    private fun formatInlineStyles(htmlEscapedText: String): String {
        var result = htmlEscapedText
        // bold **text**
        result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
        // italic *text*
        result = result.replace(Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)"), "<i>$1</i>")
        // strikethrough ~~text~~
        result = result.replace(Regex("~~(.*?)~~"), "<s>$1</s>")
        return result
    }

    fun renderTaskContextToSpanned(
        description: String,
        subtasks: List<SubTaskEntity>
    ): Spanned {
        val parts = mutableListOf<String>()
        if (description.isNotBlank()) {
            parts.add(Html.escapeHtml(description))
        }
        if (subtasks.isNotEmpty()) {
            val subtaskListHtml = subtasks.joinToString("<br/>") { st ->
                val box = if (st.isCompleted) "☑ " else "☐ "
                val titleEscaped = Html.escapeHtml(st.title)
                val text = if (st.isCompleted) "<s>$titleEscaped</s>" else titleEscaped
                "$box$text"
            }
            parts.add("<b>Subtasks:</b><br/>$subtaskListHtml")
        }
        if (parts.isEmpty()) {
            parts.add("<i>No additional description or subtasks.</i>")
        }
        val fullHtml = parts.joinToString("<br/><br/>")
        return Html.fromHtml(fullHtml, Html.FROM_HTML_MODE_COMPACT)
    }
}
