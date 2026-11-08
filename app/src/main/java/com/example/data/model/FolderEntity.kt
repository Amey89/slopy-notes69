package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentFolderId"]),
        Index(value = ["name"]),
        Index(value = ["folderType"])
    ]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val folderType: String = "NOTE", // "NOTE" or "TASK"
    val parentFolderId: Long? = null, // null = root level folder; Long = subfolder under parent
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val COMPLETED_TASKS_FOLDER_ID = -999L
        const val SHARED_LINKS_FOLDER_ID = -998L

        // Permanent Platform Subfolders under Shared Links (-998L)
        const val YOUTUBE_SUBFOLDER_ID = -901L
        const val FIREFOX_SUBFOLDER_ID = -902L
        const val CHROME_SUBFOLDER_ID = -903L
        const val PORN_SUBFOLDER_ID = -904L
        const val TIKTOK_SUBFOLDER_ID = -905L
        const val TWITCH_SUBFOLDER_ID = -906L
        const val FACEBOOK_SUBFOLDER_ID = -907L
        const val INSTAGRAM_SUBFOLDER_ID = -908L
        const val X_TWITTER_SUBFOLDER_ID = -909L
        const val THREADS_SUBFOLDER_ID = -910L
        const val PINTEREST_SUBFOLDER_ID = -911L
        const val SNAPCHAT_SUBFOLDER_ID = -912L
        const val REDDIT_SUBFOLDER_ID = -913L
        const val DISCORD_SUBFOLDER_ID = -914L

        val PermanentCompletedFolder = FolderEntity(
            id = COMPLETED_TASKS_FOLDER_ID,
            name = "Completed Tasks",
            folderType = "TASK",
            colorIndex = 1 // Emerald green
        )

        val PermanentSharedLinksFolder = FolderEntity(
            id = SHARED_LINKS_FOLDER_ID,
            name = "Shared Links",
            folderType = "NOTE",
            colorIndex = 2 // Ocean Blue
        )

        val PermanentYouTubeFolder = FolderEntity(
            id = YOUTUBE_SUBFOLDER_ID,
            name = "YouTube",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 1,
            createdAt = 1001L
        )

        val PermanentFirefoxFolder = FolderEntity(
            id = FIREFOX_SUBFOLDER_ID,
            name = "Firefox",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 5,
            createdAt = 1002L
        )

        val PermanentChromeFolder = FolderEntity(
            id = CHROME_SUBFOLDER_ID,
            name = "Chrome",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 2,
            createdAt = 1003L
        )

        val PermanentPornFolder = FolderEntity(
            id = PORN_SUBFOLDER_ID,
            name = "Porn",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 4,
            createdAt = 1004L
        )

        val PermanentTikTokFolder = FolderEntity(
            id = TIKTOK_SUBFOLDER_ID,
            name = "TikTok",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 0,
            createdAt = 1005L
        )

        val PermanentTwitchFolder = FolderEntity(
            id = TWITCH_SUBFOLDER_ID,
            name = "Twitch",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 4,
            createdAt = 1006L
        )

        val PermanentFacebookFolder = FolderEntity(
            id = FACEBOOK_SUBFOLDER_ID,
            name = "Facebook",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 2,
            createdAt = 1007L
        )

        val PermanentInstagramFolder = FolderEntity(
            id = INSTAGRAM_SUBFOLDER_ID,
            name = "Instagram",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 1,
            createdAt = 1008L
        )

        val PermanentXTwitterFolder = FolderEntity(
            id = X_TWITTER_SUBFOLDER_ID,
            name = "X (Twitter)",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 0,
            createdAt = 1009L
        )

        val PermanentThreadsFolder = FolderEntity(
            id = THREADS_SUBFOLDER_ID,
            name = "Threads",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 0,
            createdAt = 1010L
        )

        val PermanentPinterestFolder = FolderEntity(
            id = PINTEREST_SUBFOLDER_ID,
            name = "Pinterest",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 1,
            createdAt = 1011L
        )

        val PermanentSnapchatFolder = FolderEntity(
            id = SNAPCHAT_SUBFOLDER_ID,
            name = "Snapchat",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 5,
            createdAt = 1012L
        )

        val PermanentRedditFolder = FolderEntity(
            id = REDDIT_SUBFOLDER_ID,
            name = "Reddit",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 5,
            createdAt = 1013L
        )

        val PermanentDiscordFolder = FolderEntity(
            id = DISCORD_SUBFOLDER_ID,
            name = "Discord",
            folderType = "NOTE",
            parentFolderId = SHARED_LINKS_FOLDER_ID,
            colorIndex = 4,
            createdAt = 1014L
        )

        val PermanentSharedLinkSubfolders: List<FolderEntity> = listOf(
            PermanentYouTubeFolder,
            PermanentFirefoxFolder,
            PermanentChromeFolder,
            PermanentPornFolder,
            PermanentTikTokFolder,
            PermanentTwitchFolder,
            PermanentFacebookFolder,
            PermanentInstagramFolder,
            PermanentXTwitterFolder,
            PermanentThreadsFolder,
            PermanentPinterestFolder,
            PermanentSnapchatFolder,
            PermanentRedditFolder,
            PermanentDiscordFolder
        )

        val AllPermanentFolders: List<FolderEntity> = listOf(
            PermanentCompletedFolder,
            PermanentSharedLinksFolder
        ) + PermanentSharedLinkSubfolders

        private val permanentByIdMap: Map<Long, FolderEntity> =
            AllPermanentFolders.associateBy { it.id }

        private val ADULT_DOMAINS = listOf(
            "pornhub.", "xvideos.", "xnxx.", "xhamster.", "redtube.", "youporn.",
            "spankbang.", "eporner.", "tube8.", "beeg.", "chaturbate.", "stripchat.",
            "onlyfans.", "fansly.", "brazzers.", "naughtyamerica.", "rule34.",
            "hanime.", "hentai", "erome.", "motherless.", "tnaflix.", "drtuber.",
            "txxx.", "hqporner.", "daftsex.", "sxyprn.", "cam4.", "bongacams.",
            "javhd.", "javmost.", "missav.", "vjav."
        )

        fun isPermanentFolderId(id: Long?): Boolean {
            if (id == null) return false
            return permanentByIdMap.containsKey(id)
        }

        fun isSharedLinksSubfolderId(id: Long?): Boolean {
            if (id == null) return false
            return id in DISCORD_SUBFOLDER_ID..YOUTUBE_SUBFOLDER_ID
        }

        fun isSharedLinksFamilyId(id: Long?): Boolean {
            if (id == null) return false
            return id == SHARED_LINKS_FOLDER_ID || isSharedLinksSubfolderId(id)
        }

        fun getPermanentFolderById(id: Long?): FolderEntity? {
            if (id == null) return null
            return permanentByIdMap[id]
        }

        fun matchPermanentSharedSubfolderByName(name: String): FolderEntity? {
            val clean = name.trim().lowercase()
            return when (clean) {
                "youtube", "yt" -> PermanentYouTubeFolder
                "firefox", "mozilla firefox" -> PermanentFirefoxFolder
                "chrome", "google chrome" -> PermanentChromeFolder
                "porn", "adult", "nsfw", "18+" -> PermanentPornFolder
                "tiktok", "tik tok" -> PermanentTikTokFolder
                "twitch" -> PermanentTwitchFolder
                "facebook", "fb" -> PermanentFacebookFolder
                "instagram", "insta", "ig" -> PermanentInstagramFolder
                "x (twitter)", "x", "twitter", "x / twitter" -> PermanentXTwitterFolder
                "threads" -> PermanentThreadsFolder
                "pinterest", "pintrest" -> PermanentPinterestFolder
                "snapchat", "snap" -> PermanentSnapchatFolder
                "reddit" -> PermanentRedditFolder
                "discord" -> PermanentDiscordFolder
                else -> null
            }
        }

        /**
         * Determines the appropriate permanent subfolder under Shared Links for a given URL,
         * shared text, and optional source package name (e.g., Firefox vs Chrome).
         */
        fun resolvePlatformSubfolder(
            url: String,
            sharedText: String = "",
            sourcePackage: String? = null
        ): FolderEntity {
            val lowerUrl = url.lowercase()
            val lowerText = sharedText.lowercase()
            val lowerPkg = sourcePackage?.lowercase() ?: ""

            return when {
                // 1. YouTube
                lowerUrl.contains("youtube.com") ||
                    lowerUrl.contains("youtu.be") ||
                    lowerPkg.contains("com.google.android.youtube") -> PermanentYouTubeFolder

                // 2. Porn / Adult video sites
                ADULT_DOMAINS.any { lowerUrl.contains(it) } ||
                    Regex("\\b(porn|xxx|nsfw|xvideos|pornhub|xnxx|xhamster|spankbang|redtube|youporn|eporner|onlyfans)\\b").containsMatchIn(lowerUrl) ||
                    Regex("\\b(porn|xxx|nsfw)\\b").containsMatchIn(lowerText) -> PermanentPornFolder

                // 3. TikTok
                lowerUrl.contains("tiktok.com") ||
                    lowerPkg.contains("musically") ||
                    lowerPkg.contains("trill") ||
                    lowerPkg.contains("tiktok") -> PermanentTikTokFolder

                // 4. Twitch
                lowerUrl.contains("twitch.tv") ||
                    lowerPkg.contains("tv.twitch") -> PermanentTwitchFolder

                // 5. Facebook
                lowerUrl.contains("facebook.com") ||
                    lowerUrl.contains("fb.watch") ||
                    lowerUrl.contains("fb.com") ||
                    lowerUrl.contains("fb.me") ||
                    lowerPkg.contains("com.facebook.katana") ||
                    lowerPkg.contains("com.facebook.lite") -> PermanentFacebookFolder

                // 6. Instagram
                lowerUrl.contains("instagram.com") ||
                    lowerUrl.contains("instagr.am") ||
                    lowerUrl.contains("ig.me") ||
                    lowerPkg.contains("com.instagram.android") -> PermanentInstagramFolder

                // 7. Threads
                lowerUrl.contains("threads.net") ||
                    lowerUrl.contains("threads.com") ||
                    lowerPkg.contains("com.instagram.barcelona") -> PermanentThreadsFolder

                // 8. X (Twitter)
                lowerUrl.contains("x.com") ||
                    lowerUrl.contains("twitter.com") ||
                    lowerUrl.contains("t.co/") ||
                    lowerUrl.contains("fxtwitter.com") ||
                    lowerUrl.contains("vxtwitter.com") ||
                    lowerUrl.contains("fixupx.com") ||
                    lowerPkg.contains("com.twitter.android") -> PermanentXTwitterFolder

                // 9. Pinterest
                lowerUrl.contains("pinterest.") ||
                    lowerUrl.contains("pin.it") ||
                    lowerPkg.contains("com.pinterest") -> PermanentPinterestFolder

                // 10. Snapchat
                lowerUrl.contains("snapchat.com") ||
                    lowerPkg.contains("com.snapchat.android") -> PermanentSnapchatFolder

                // 11. Reddit
                lowerUrl.contains("reddit.com") ||
                    lowerUrl.contains("redd.it") ||
                    lowerPkg.contains("com.reddit.frontpage") -> PermanentRedditFolder

                // 12. Discord
                lowerUrl.contains("discord.com") ||
                    lowerUrl.contains("discord.gg") ||
                    lowerUrl.contains("discordapp.com") ||
                    lowerUrl.contains("discord.media") ||
                    lowerPkg.contains("com.discord") -> PermanentDiscordFolder

                // 13. Firefox (when shared from Firefox browser or Mozilla domain)
                lowerPkg.contains("mozilla") ||
                    lowerPkg.contains("firefox") ||
                    lowerPkg.contains("fenix") ||
                    lowerUrl.contains("mozilla.org") ||
                    lowerUrl.contains("firefox.com") ||
                    lowerText.contains("shared via firefox") ||
                    lowerText.contains("from firefox") -> PermanentFirefoxFolder

                // 14. Chrome (default for general web links shared from Chrome or browsers)
                else -> PermanentChromeFolder
            }
        }
    }
}
