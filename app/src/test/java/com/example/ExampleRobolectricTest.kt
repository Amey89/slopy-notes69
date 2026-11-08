package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.NotesAndTasksRepository
import com.example.util.LinkMetadataExtractor
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
    fun test_permanent_shared_link_subfolders_and_metadata_routing() {
        assertEquals(14, FolderEntity.PermanentSharedLinkSubfolders.size)
        FolderEntity.PermanentSharedLinkSubfolders.forEach { sub ->
            assertEquals(FolderEntity.SHARED_LINKS_FOLDER_ID, sub.parentFolderId)
            assertTrue(FolderEntity.isSharedLinksSubfolderId(sub.id))
            assertTrue(FolderEntity.isSharedLinksFamilyId(sub.id))
        }

        // Pinterest link routes to Pinterest subfolder (-911L)
        val pinFolder = FolderEntity.resolvePlatformSubfolder("https://www.pinterest.com/pin/123456789/")
        assertEquals(FolderEntity.PINTEREST_SUBFOLDER_ID, pinFolder.id)
        assertEquals("Pinterest", pinFolder.name)

        // Short Pinterest link (pin.it) routes to Pinterest subfolder
        val pinShort = FolderEntity.resolvePlatformSubfolder("https://pin.it/3xyzAbc")
        assertEquals(FolderEntity.PINTEREST_SUBFOLDER_ID, pinShort.id)

        // YouTube link routes to YouTube subfolder (-901L)
        val ytPreview = LinkMetadataExtractor.resolveInstantPreview(
            sharedText = "Check this out https://youtu.be/dQw4w9WgXcQ",
            hintSubject = "Never Gonna Give You Up"
        )
        assertNotNull(ytPreview)
        assertEquals(FolderEntity.YOUTUBE_SUBFOLDER_ID, ytPreview.subfolderId)
        assertEquals("YouTube", ytPreview.platformName)
        assertTrue(ytPreview.imageUrl.contains("dQw4w9WgXcQ"))

        // X (Twitter), TikTok, Twitch, Facebook, Instagram, Threads, Snapchat, Reddit, Discord, Porn, Firefox, Chrome
        assertEquals(FolderEntity.X_TWITTER_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://x.com/user/status/12345").id)
        assertEquals(FolderEntity.TIKTOK_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://www.tiktok.com/@user/video/123").id)
        assertEquals(FolderEntity.TWITCH_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://www.twitch.tv/shroud").id)
        assertEquals(FolderEntity.FACEBOOK_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://fb.watch/abc123/").id)
        assertEquals(FolderEntity.INSTAGRAM_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://www.instagram.com/reel/abc/").id)
        assertEquals(FolderEntity.THREADS_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://www.threads.net/@user/post/xyz").id)
        assertEquals(FolderEntity.SNAPCHAT_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://www.snapchat.com/spotlight/123").id)
        assertEquals(FolderEntity.REDDIT_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://www.reddit.com/r/androiddev/comments/123/test/").id)
        assertEquals(FolderEntity.DISCORD_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://discord.gg/invitecode").id)
        assertEquals(FolderEntity.PORN_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://www.pornhub.com/view_video.php?viewkey=123").id)
        assertEquals(FolderEntity.FIREFOX_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://example.org/article", sourcePackage = "org.mozilla.firefox").id)
        assertEquals(FolderEntity.CHROME_SUBFOLDER_ID, FolderEntity.resolvePlatformSubfolder("https://example.org/article", sourcePackage = "com.android.chrome").id)
    }

    @Test
    fun test_note_and_task_crud_and_export() = runBlocking {
        // Insert note inside Pinterest subfolder under Shared Links
        val noteId = repository.insertNote(
            NoteEntity(
                title = "Meeting Notes",
                content = "# Architecture\n- [ ] Define API\n- [x] Room DB setup",
                tags = "Work,Tech",
                folderId = FolderEntity.PINTEREST_SUBFOLDER_ID,
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
        assertTrue(exportedJson.contains("Pinterest"))
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
