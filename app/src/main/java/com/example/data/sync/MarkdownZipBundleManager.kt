package com.example.data.sync

import android.content.Context
import com.example.data.db.FolderDao
import com.example.data.db.NoteDao
import com.example.data.db.TaskDao
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithSubtasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class MarkdownImportSummary(
    val notesCount: Int,
    val tasksCount: Int,
    val foldersCount: Int,
    val subtasksCount: Int
)

object MarkdownZipBundleManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Exports all notes and tasks into their individual .md files,
     * segregated hierarchically into folders, packed in a single ZIP file.
     */
    suspend fun exportZipBundle(
        context: Context,
        folders: List<FolderEntity>,
        notes: List<NoteEntity>,
        tasks: List<TaskWithSubtasks>
    ): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val zipFile = File(context.cacheDir, "Notes_Tasks_Backup_$timestamp.zip")

        // 1. Build directory path mapping for each folder
        val allFolders = (listOf(FolderEntity.PermanentCompletedFolder, FolderEntity.PermanentSharedLinksFolder) + folders).distinctBy { it.id }
        val folderById = allFolders.associateBy { it.id }
        val folderPathMap = mutableMapOf<Long, String>()
        folderPathMap[FolderEntity.COMPLETED_TASKS_FOLDER_ID] = "Completed Tasks/"
        folderPathMap[FolderEntity.SHARED_LINKS_FOLDER_ID] = "Shared Links/"

        fun resolveFolderPath(folder: FolderEntity): String {
            folderPathMap[folder.id]?.let { return it }
            if (folder.id == FolderEntity.COMPLETED_TASKS_FOLDER_ID) {
                folderPathMap[folder.id] = "Completed Tasks/"
                return "Completed Tasks/"
            }
            if (folder.id == FolderEntity.SHARED_LINKS_FOLDER_ID) {
                folderPathMap[folder.id] = "Shared Links/"
                return "Shared Links/"
            }
            val parentPath = folder.parentFolderId?.let { parentId ->
                folderById[parentId]?.let { resolveFolderPath(it) }
            } ?: ""
            val safeName = sanitizeDirectoryName(folder.name)
            val fullPath = if (parentPath.isEmpty()) "$safeName/" else "$parentPath$safeName/"
            folderPathMap[folder.id] = fullPath
            return fullPath
        }

        for (folder in allFolders) {
            resolveFolderPath(folder)
        }

        // 2. Stream all segregated notes and tasks into ZipOutputStream
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // Ensure empty folders (including permanent system folders) are represented in the zip
            for (folder in allFolders) {
                val dirPath = folderPathMap[folder.id] ?: continue
                try {
                    val dirEntry = ZipEntry(dirPath)
                    zos.putNextEntry(dirEntry)
                    zos.closeEntry()
                } catch (_: Exception) {
                    // Entry may already exist, safe to ignore
                }
            }

            // Write each Note as an individual .md file
            val usedNoteFileNames = mutableSetOf<String>()
            notes.forEachIndexed { idx, note ->
                val folderPath = when {
                    note.folderId == FolderEntity.SHARED_LINKS_FOLDER_ID -> "Shared Links/"
                    note.folderId != null -> folderPathMap[note.folderId] ?: ""
                    else -> ""
                }
                val baseTitle = sanitizeFileName(note.title.ifBlank { "Untitled_Note_${idx + 1}" })
                var uniqueName = "$folderPath$baseTitle.md"
                var duplicateCounter = 1
                while (usedNoteFileNames.contains(uniqueName)) {
                    uniqueName = "$folderPath${baseTitle}_$duplicateCounter.md"
                    duplicateCounter++
                }
                usedNoteFileNames.add(uniqueName)

                val noteMd = buildNoteMarkdown(note)
                val entry = ZipEntry(uniqueName)
                entry.time = note.updatedAt
                zos.putNextEntry(entry)
                zos.write(noteMd.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            // Write each Task as an individual .task.md file (with checkbox subtasks)
            val usedTaskFileNames = mutableSetOf<String>()
            tasks.forEachIndexed { idx, tws ->
                val folderPath = when {
                    tws.task.folderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> "Completed Tasks/"
                    tws.task.isCompleted && tws.task.folderId == null -> "Completed Tasks/"
                    tws.task.folderId != null -> folderPathMap[tws.task.folderId] ?: ""
                    else -> ""
                }
                val baseTitle = sanitizeFileName(tws.task.title.ifBlank { "Untitled_Task_${idx + 1}" })
                var uniqueName = "$folderPath$baseTitle.task.md"
                var duplicateCounter = 1
                while (usedTaskFileNames.contains(uniqueName)) {
                    uniqueName = "$folderPath${baseTitle}_$duplicateCounter.task.md"
                    duplicateCounter++
                }
                usedTaskFileNames.add(uniqueName)

                val taskMd = buildTaskMarkdown(tws)
                val entry = ZipEntry(uniqueName)
                entry.time = tws.task.updatedAt
                zos.putNextEntry(entry)
                zos.write(taskMd.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        zipFile
    }

    /**
     * Imports a ZIP bundle containing segregated folders and markdown files.
     * Recreates the folder tree and imports individual notes and tasks.
     */
    suspend fun importZipBundle(
        inputStream: InputStream,
        folderDao: FolderDao,
        noteDao: NoteDao,
        taskDao: TaskDao
    ): Result<MarkdownImportSummary> = withContext(Dispatchers.IO) {
        try {
            var importedNotes = 0
            var importedTasks = 0
            var importedFolders = 0
            var importedSubtasks = 0

            // Cache of resolved folder paths to DB folder IDs
            // e.g. "Work" -> 101, "Work/Projects" -> 102
            val folderCache = mutableMapOf<String, Long>()

            // Pre-populate cache with permanent folders to prevent creating duplicate DB rows
            folderCache["Completed Tasks"] = FolderEntity.COMPLETED_TASKS_FOLDER_ID
            folderCache["Permanent Archive"] = FolderEntity.COMPLETED_TASKS_FOLDER_ID
            folderCache["Shared Links"] = FolderEntity.SHARED_LINKS_FOLDER_ID

            // Load existing folders into cache to avoid duplicate folders if re-importing
            val existingFolders = folderDao.getAllFoldersDirect()
            val existingFolderById = existingFolders.associateBy { it.id }

            fun getExistingPath(folder: FolderEntity): String {
                val parentPath = folder.parentFolderId?.let { pId ->
                    existingFolderById[pId]?.let { getExistingPath(it) }
                } ?: ""
                val safeName = sanitizeDirectoryName(folder.name)
                return if (parentPath.isEmpty()) safeName else "$parentPath/$safeName"
            }

            for (f in existingFolders) {
                val p = getExistingPath(f)
                folderCache[p] = f.id
            }

            // Helper to get or create folder by relative path: e.g. "Work/Projects"
            suspend fun getOrCreateFolderId(path: String): Long? {
                val cleanPath = path.trim('/').trim()
                if (cleanPath.isEmpty()) return null

                // Check permanent folders first
                if (cleanPath.equals("Completed Tasks", ignoreCase = true) || cleanPath.equals("Permanent Archive", ignoreCase = true)) {
                    return FolderEntity.COMPLETED_TASKS_FOLDER_ID
                }
                if (cleanPath.equals("Shared Links", ignoreCase = true)) {
                    return FolderEntity.SHARED_LINKS_FOLDER_ID
                }

                folderCache[cleanPath]?.let { return it }

                val segments = cleanPath.split('/').filter { it.isNotBlank() }
                var currentParentId: Long? = null
                var runningPath = ""

                for (segment in segments) {
                    val safeSegment = sanitizeDirectoryName(segment)
                    runningPath = if (runningPath.isEmpty()) safeSegment else "$runningPath/$safeSegment"

                    var folderId = folderCache[runningPath]
                    if (folderId == null) {
                        // Check DB for matching folder name + parentFolderId
                        val existing = folderDao.getAllFoldersDirect().firstOrNull {
                            it.name.equals(segment, ignoreCase = true) && it.parentFolderId == currentParentId
                        }
                        if (existing != null) {
                            folderId = existing.id
                        } else {
                            folderId = folderDao.insertFolder(
                                FolderEntity(
                                    name = segment,
                                    parentFolderId = currentParentId,
                                    colorIndex = 0,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            importedFolders++
                        }
                        folderCache[runningPath] = folderId
                    }
                    currentParentId = folderId
                }

                return currentParentId
            }

            // Parse ZIP stream
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val rawName = entry.name.replace('\\', '/')
                    if (!entry.isDirectory && isMarkdownFile(rawName)) {
                        val content = zis.readBytes().toString(Charsets.UTF_8)
                        val lastSlash = rawName.lastIndexOf('/')
                        val folderPath = if (lastSlash != -1) rawName.substring(0, lastSlash) else ""
                        val fileName = if (lastSlash != -1) rawName.substring(lastSlash + 1) else rawName

                        val targetFolderId = if (folderPath.isNotBlank()) {
                            getOrCreateFolderId(folderPath)
                        } else null

                        // Determine if Task or Note
                        val isTask = isTaskFile(fileName, content)

                        if (isTask) {
                            val parsedTask = parseTaskMarkdown(content, fileName, targetFolderId)
                            val finalFolderId = when {
                                targetFolderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
                                parsedTask.task.folderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
                                parsedTask.task.isCompleted && targetFolderId == null -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
                                else -> targetFolderId ?: parsedTask.task.folderId
                            }
                            val taskToInsert = parsedTask.task.copy(
                                folderId = finalFolderId,
                                isCompleted = parsedTask.task.isCompleted || (finalFolderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID)
                            )
                            val taskId = taskDao.insertTask(taskToInsert)
                            if (finalFolderId != null) {
                                folderDao.insertTaskFolderCrossRef(com.example.data.model.TaskFolderCrossRef(taskId, finalFolderId))
                            }
                            if (parsedTask.subtasks.isNotEmpty()) {
                                val subtasksToInsert = parsedTask.subtasks.map { st ->
                                    st.copy(parentTaskId = taskId)
                                }
                                taskDao.insertSubTasks(subtasksToInsert)
                                importedSubtasks += subtasksToInsert.size
                            }
                            importedTasks++
                        } else {
                            val parsedNote = parseNoteMarkdown(content, fileName, targetFolderId)
                            val finalFolderId = when {
                                targetFolderId == FolderEntity.SHARED_LINKS_FOLDER_ID -> FolderEntity.SHARED_LINKS_FOLDER_ID
                                parsedNote.folderId == FolderEntity.SHARED_LINKS_FOLDER_ID -> FolderEntity.SHARED_LINKS_FOLDER_ID
                                else -> targetFolderId ?: parsedNote.folderId
                            }
                            val noteToInsert = parsedNote.copy(folderId = finalFolderId)
                            val noteId = noteDao.insert(noteToInsert)
                            if (finalFolderId != null) {
                                folderDao.insertNoteFolderCrossRef(com.example.data.model.NoteFolderCrossRef(noteId, finalFolderId))
                            }
                            importedNotes++
                        }
                    } else if (entry.isDirectory) {
                        // Ensure directory path is registered
                        getOrCreateFolderId(rawName)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            Result.success(
                MarkdownImportSummary(
                    notesCount = importedNotes,
                    tasksCount = importedTasks,
                    foldersCount = importedFolders,
                    subtasksCount = importedSubtasks
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- MARKDOWN BUILDERS ---

    private fun buildNoteMarkdown(note: NoteEntity): String = buildString {
        appendLine("---")
        appendLine("type: note")
        appendLine("title: \"${note.title.replace("\"", "\\\"")}\"")
        appendLine("starred: ${note.isStarred}")
        appendLine("colorIndex: ${note.colorIndex}")
        if (note.folderId == FolderEntity.SHARED_LINKS_FOLDER_ID) {
            appendLine("folderId: ${FolderEntity.SHARED_LINKS_FOLDER_ID}")
            appendLine("permanentFolder: \"Shared Links\"")
        }
        if (note.tags.isNotBlank()) appendLine("tags: [${note.tags}]")
        if (note.imageUris.isNotBlank()) appendLine("imageUris: \"${note.imageUris.replace("\"", "\\\"")}\"")
        appendLine("createdAt: ${note.createdAt}")
        appendLine("updatedAt: ${note.updatedAt}")
        appendLine("date: ${dateFormat.format(Date(note.createdAt))}")
        appendLine("---")
        appendLine()
        if (note.title.isNotBlank()) {
            appendLine("# ${note.title}")
            appendLine()
        }
        append(note.content)
    }

    private fun buildTaskMarkdown(tws: TaskWithSubtasks): String = buildString {
        val task = tws.task
        appendLine("---")
        appendLine("type: task")
        appendLine("title: \"${task.title.replace("\"", "\\\"")}\"")
        appendLine("completed: ${task.isCompleted}")
        appendLine("starred: ${task.isStarred}")
        appendLine("priority: ${task.priority}")
        if (task.folderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID || task.isCompleted) {
            appendLine("folderId: ${FolderEntity.COMPLETED_TASKS_FOLDER_ID}")
            appendLine("permanentFolder: \"Completed Tasks\"")
        }
        if (task.dueDate != null) appendLine("dueDate: ${task.dueDate}")
        if (task.reminderTime != null) {
            appendLine("reminderTime: ${task.reminderTime}")
            appendLine("reminderEnabled: ${task.reminderEnabled}")
        }
        if (task.tags.isNotBlank()) appendLine("tags: [${task.tags}]")
        appendLine("createdAt: ${task.createdAt}")
        appendLine("updatedAt: ${task.updatedAt}")
        appendLine("date: ${dateFormat.format(Date(task.createdAt))}")
        appendLine("---")
        appendLine()
        if (task.title.isNotBlank()) {
            appendLine("# ${task.title}")
            appendLine()
        }
        if (task.description.isNotBlank()) {
            appendLine(task.description)
            appendLine()
        }

        if (tws.subtasks.isNotEmpty()) {
            appendLine("## Subtasks")
            tws.subtasks.sortedBy { it.orderIndex }.forEach { st ->
                val check = if (st.isCompleted) "x" else " "
                appendLine("- [$check] ${st.title}")
            }
        }
    }

    // --- MARKDOWN PARSERS ---

    private data class ParsedTaskResult(
        val task: TaskEntity,
        val subtasks: List<SubTaskEntity>
    )

    private fun isMarkdownFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt")
    }

    private fun isTaskFile(fileName: String, content: String): Boolean {
        if (content.contains("type: note") || content.contains("type: \"note\"")) return false
        if (fileName.endsWith(".task.md", ignoreCase = true)) return true
        if (content.contains("type: task") || content.contains("type: \"task\"")) return true
        if (content.contains("## Subtasks", ignoreCase = true)) return true
        return false
    }

    private fun parseTaskMarkdown(content: String, fileName: String, folderId: Long?): ParsedTaskResult {
        val (frontmatter, body) = splitFrontmatter(content)
        val defaultTitle = fileName
            .removeSuffix(".task.md")
            .removeSuffix(".md")
            .removeSuffix(".markdown")
            .removeSuffix(".txt")

        var title = frontmatter["title"] ?: extractFirstHeading(body) ?: defaultTitle
        title = title.trim('"', ' ', '\t')

        val rawFolderId = frontmatter["folderId"]?.toLongOrNull()
        val permFolder = frontmatter["permanentFolder"]
        val effectiveFolderId = when {
            folderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
            rawFolderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
            permFolder?.contains("Completed", ignoreCase = true) == true -> FolderEntity.COMPLETED_TASKS_FOLDER_ID
            folderId != null -> folderId
            else -> rawFolderId
        }

        val completed = frontmatter["completed"]?.toBooleanStrictOrNull() ?: (effectiveFolderId == FolderEntity.COMPLETED_TASKS_FOLDER_ID)
        val starred = frontmatter["starred"]?.toBooleanStrictOrNull() ?: false
        val priority = frontmatter["priority"]?.toIntOrNull() ?: 1
        val dueDate = frontmatter["dueDate"]?.toLongOrNull()
        val reminderTime = frontmatter["reminderTime"]?.toLongOrNull()
        val reminderEnabled = frontmatter["reminderEnabled"]?.toBooleanStrictOrNull() ?: (reminderTime != null)
        val tags = frontmatter["tags"]?.trim('[', ']') ?: ""
        val createdAt = frontmatter["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis()
        val updatedAt = frontmatter["updatedAt"]?.toLongOrNull() ?: System.currentTimeMillis()

        // Extract subtasks & description from body
        val lines = body.lines()
        val subtasks = mutableListOf<SubTaskEntity>()
        val descriptionLines = mutableListOf<String>()
        var inSubtasksSection = false
        val checkRegex = "^\\s*-\\s*\\[([ xX])\\]\\s*(.*)$".toRegex()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# ") && trimmed.substring(2).trim().equals(title, ignoreCase = true)) {
                // Skip the top title header if duplicated
                continue
            }
            if (trimmed.startsWith("## Subtasks", ignoreCase = true)) {
                inSubtasksSection = true
                continue
            }

            val match = checkRegex.find(line)
            if (match != null) {
                val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
                val subtaskTitle = match.groupValues[2].trim()
                if (subtaskTitle.isNotBlank()) {
                    subtasks.add(
                        SubTaskEntity(
                            parentTaskId = 0L,
                            title = subtaskTitle,
                            isCompleted = isChecked,
                            orderIndex = subtasks.size
                        )
                    )
                }
            } else if (!inSubtasksSection) {
                descriptionLines.add(line)
            }
        }

        val description = descriptionLines.joinToString("\n").trim()

        val task = TaskEntity(
            title = title,
            description = description,
            isCompleted = completed,
            isStarred = starred,
            dueDate = dueDate,
            reminderTime = reminderTime,
            reminderEnabled = reminderEnabled,
            priority = priority,
            tags = tags,
            folderId = effectiveFolderId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        return ParsedTaskResult(task, subtasks)
    }

    private fun parseNoteMarkdown(content: String, fileName: String, folderId: Long?): NoteEntity {
        val (frontmatter, body) = splitFrontmatter(content)
        val defaultTitle = fileName
            .removeSuffix(".md")
            .removeSuffix(".markdown")
            .removeSuffix(".txt")

        var title = frontmatter["title"] ?: extractFirstHeading(body) ?: defaultTitle
        title = title.trim('"', ' ', '\t')

        val rawFolderId = frontmatter["folderId"]?.toLongOrNull()
        val permFolder = frontmatter["permanentFolder"]
        val effectiveFolderId = when {
            folderId == FolderEntity.SHARED_LINKS_FOLDER_ID -> FolderEntity.SHARED_LINKS_FOLDER_ID
            rawFolderId == FolderEntity.SHARED_LINKS_FOLDER_ID -> FolderEntity.SHARED_LINKS_FOLDER_ID
            permFolder?.contains("Shared", ignoreCase = true) == true -> FolderEntity.SHARED_LINKS_FOLDER_ID
            folderId != null -> folderId
            else -> rawFolderId
        }

        val starred = frontmatter["starred"]?.toBooleanStrictOrNull() ?: false
        val colorIndex = frontmatter["colorIndex"]?.toIntOrNull() ?: 0
        val tags = frontmatter["tags"]?.trim('[', ']') ?: ""
        val imageUris = frontmatter["imageUris"]?.trim('"', ' ') ?: ""
        val createdAt = frontmatter["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis()
        val updatedAt = frontmatter["updatedAt"]?.toLongOrNull() ?: System.currentTimeMillis()

        // Strip the duplicate top title header if it exists at start of body
        val cleanBody = if (body.trimStart().startsWith("# ") && extractFirstHeading(body)?.equals(title, ignoreCase = true) == true) {
            val lines = body.lines()
            val firstNonEmptyIdx = lines.indexOfFirst { it.trim().startsWith("# ") }
            if (firstNonEmptyIdx != -1) {
                lines.drop(firstNonEmptyIdx + 1).joinToString("\n").trim()
            } else body.trim()
        } else {
            body.trim()
        }

        return NoteEntity(
            title = title,
            content = cleanBody,
            tags = tags,
            imageUris = imageUris,
            isStarred = starred,
            colorIndex = colorIndex,
            folderId = effectiveFolderId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun splitFrontmatter(content: String): Pair<Map<String, String>, String> {
        val lines = content.lines()
        if (lines.isEmpty() || lines[0].trim() != "---") {
            return Pair(emptyMap(), content)
        }

        val endIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (endIdx == -1) {
            return Pair(emptyMap(), content)
        }

        val frontmatterLines = lines.subList(1, endIdx + 1)
        val bodyLines = lines.subList(endIdx + 2, lines.size)

        val map = mutableMapOf<String, String>()
        for (fLine in frontmatterLines) {
            val colonIdx = fLine.indexOf(':')
            if (colonIdx != -1) {
                val key = fLine.substring(0, colonIdx).trim()
                val value = fLine.substring(colonIdx + 1).trim()
                map[key] = value
            }
        }

        return Pair(map, bodyLines.joinToString("\n"))
    }

    private fun extractFirstHeading(text: String): String? {
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim()
            }
        }
        return null
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            .trim()
            .take(60)
            .ifBlank { "Item" }
    }

    private fun sanitizeDirectoryName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            .trim()
            .take(50)
            .ifBlank { "Folder" }
    }
}
