package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.NotesAndTasksRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: NotesAndTasksRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NotesAndTasksRepository(db.noteDao(), db.taskDao(), db.folderDao(), db.tagDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun read_app_name_from_context() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Notes & Tasks", appName)
    }

    @Test
    fun test_permanent_tags_and_completed_tasks() = runBlocking {
        // Test permanent tag creation
        repository.createTag("Urgent", 2)
        val tags = db.tagDao().getAllTagsDirect()
        assertTrue(tags.any { it.name == "Urgent" })

        // Test task completion
        val task = TaskEntity(
            title = "Complete App Polish",
            isCompleted = false
        )
        val taskId = repository.saveTaskWithSubtasks(task, emptyList())
        assertTrue(taskId > 0)

        // Mark task complete
        repository.toggleTaskCompleted(taskId, true)
        val updated = repository.getTaskWithSubtasksDirect(taskId)
        assertNotNull(updated)
        assertTrue(updated!!.task.isCompleted)
    }

    @Test
    fun test_note_and_task_crud_and_export() = runBlocking {
        // Insert note
        val noteId = repository.insertNote(
            NoteEntity(
                title = "Meeting Notes",
                content = "# Architecture\n- [ ] Define API\n- [x] Room DB setup",
                tags = "Work,Tech",
                isStarred = true
            )
        )
        assertTrue(noteId > 0)

        // Insert task with subtasks
        val task = TaskEntity(
            title = "Launch Product",
            description = "Prepare launch checklist",
            priority = 2,
            isStarred = true
        )
        val subtasks = listOf(
            SubTaskEntity(parentTaskId = 0, title = "Design Mockup", isCompleted = true, orderIndex = 0),
            SubTaskEntity(parentTaskId = 0, title = "Implement Reminder", isCompleted = false, orderIndex = 1)
        )
        val taskId = repository.saveTaskWithSubtasks(task, subtasks)
        assertTrue(taskId > 0)

        // Test JSON Export
        val exportedJson = repository.exportDatabaseToJson()
        assertNotNull(exportedJson)
        assertTrue(exportedJson.contains("Meeting Notes"))
        assertTrue(exportedJson.contains("Launch Product"))
        assertTrue(exportedJson.contains("Implement Reminder"))

        // Test JSON Import
        val importResult = repository.importDatabaseFromJson(exportedJson)
        assertTrue(importResult.isSuccess)

        // Test Markdown ZIP Bundle Export
        val zipFile = repository.exportMarkdownZipBundle(context)
        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)

        // Test Markdown ZIP Bundle Import
        zipFile.inputStream().use { input ->
            val zipImportResult = repository.importMarkdownZipBundle(input)
            assertTrue(zipImportResult.isSuccess)
            val summary = zipImportResult.getOrThrow()
            assertTrue(summary.notesCount >= 1)
            assertTrue(summary.tasksCount >= 1)
        }
    }
}
