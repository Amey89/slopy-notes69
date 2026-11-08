package com.example.util

import android.util.Patterns
import com.example.data.model.FolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LinkMetadata(
    val url: String,
    val title: String,
    val description: String = "",
    val imageUrl: String = "",
    val domain: String = "",
    val isYouTube: Boolean = false,
    val platformName: String = "Chrome",
    val subfolderId: Long = FolderEntity.CHROME_SUBFOLDER_ID,
    val suggestedTags: List<String> = listOf("link")
)

object LinkMetadataExtractor {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val YOUTUBE_REGEX = Regex(
        "(?:youtu\\.be/|youtube\\.com/(?:embed/|v/|shorts/|live/|watch\\?v=|watch\\?.+&v=))([\\w-]{11})",
        RegexOption.IGNORE_CASE
    )

    private val TWITTER_STATUS_REGEX = Regex(
        "(?:twitter\\.com|x\\.com|fxtwitter\\.com|vxtwitter\\.com)/([A-Za-z0-9_]+)/status/(\\d+)",
        RegexOption.IGNORE_CASE
    )

    private val DISCORD_INVITE_REGEX = Regex(
        "(?:discord\\.gg|discord(?:app)?\\.com/invite)/([A-Za-z0-9-]+)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extracts the first valid HTTP/HTTPS URL from any shared text.
     */
    fun extractUrl(text: String): String? {
        val httpRegex = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE)
        val directMatch = httpRegex.find(text)
        if (directMatch != null) {
            return directMatch.value.trimEnd('.', ',', ')', ']', ';')
        }
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            var candidate = matcher.group().trimEnd('.', ',', ')', ']', ';')
            if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
                candidate = "https://$candidate"
            }
            return candidate
        }
        return null
    }

    /**
     * Extracts domain host from URL (e.g. "youtube.com").
     */
    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: ""
            host.removePrefix("www.").removePrefix("m.").removePrefix("mobile.")
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Extracts YouTube Video ID if present.
     */
    fun extractYouTubeId(url: String): String? {
        val match = YOUTUBE_REGEX.find(url)
        return match?.groupValues?.getOrNull(1)
    }

    /**
     * Builds an instant, zero-latency preview before network resolution completes.
     */
    fun resolveInstantPreview(
        sharedText: String,
        hintSubject: String? = null,
        sourcePackage: String? = null
    ): LinkMetadata {
        val extractedUrl = extractUrl(sharedText) ?: sharedText.trim()
        val domain = extractDomain(extractedUrl).ifBlank { "link" }
        val ytVideoId = extractYouTubeId(extractedUrl)
        val platformFolder = FolderEntity.resolvePlatformSubfolder(extractedUrl, sharedText, sourcePackage)
        val platformTag = platformFolder.name.lowercase()
            .replace(" (twitter)", "")
            .replace(" ", "-")

        val instantImage = when {
            ytVideoId != null -> "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg"
            domain.contains("instagram.com") -> {
                val cleanMedia = extractedUrl.substringBefore("?").trimEnd('/')
                "$cleanMedia/media/?size=l"
            }
            else -> ""
        }

        val fallbackTitle = when {
            !hintSubject.isNullOrBlank() -> hintSubject.trim()
            else -> generateFallbackTitle(sharedText, extractedUrl, domain, platformFolder.name)
        }

        val defaultTags = buildList {
            add("link")
            add(platformTag)
            if (ytVideoId != null || platformFolder.id in listOf(
                    FolderEntity.YOUTUBE_SUBFOLDER_ID,
                    FolderEntity.TIKTOK_SUBFOLDER_ID,
                    FolderEntity.TWITCH_SUBFOLDER_ID,
                    FolderEntity.PORN_SUBFOLDER_ID
                )
            ) {
                add("video")
            }
        }.distinct()

        return LinkMetadata(
            url = extractedUrl,
            title = fallbackTitle,
            description = "",
            imageUrl = instantImage,
            domain = domain,
            isYouTube = ytVideoId != null,
            platformName = platformFolder.name,
            subfolderId = platformFolder.id,
            suggestedTags = defaultTags
        )
    }

    /**
     * Resolves comprehensive metadata for the given URL or shared text across:
     * YouTube, Firefox, Chrome, Porn/Adult video platforms, TikTok, Twitch,
     * Facebook, Instagram, X (Twitter), Threads, Pinterest, Snapchat, Reddit, Discord.
     */
    suspend fun resolve(
        sharedText: String,
        hintSubject: String? = null,
        sourcePackage: String? = null
    ): LinkMetadata = withContext(Dispatchers.IO) {
        val initialUrl = extractUrl(sharedText) ?: sharedText.trim()
        var resolvedUrl = initialUrl
        var preFetchedHtml: String? = null

        // If it's a known shortlink or redirect domain, or we fetch HTML, capture final redirected URL
        val initialDomain = extractDomain(initialUrl)
        val isShortLink = initialDomain in listOf(
            "pin.it", "vm.tiktok.com", "vt.tiktok.com", "t.co", "redd.it",
            "fb.watch", "fb.me", "instagr.am", "ig.me", "t.snapchat.com",
            "bit.ly", "tinyurl.com", "ow.ly"
        )

        if (isShortLink) {
            try {
                val req = Request.Builder()
                    .url(initialUrl)
                    .header("User-Agent", SOCIAL_CRAWLER_USER_AGENT)
                    .build()
                client.newCall(req).execute().use { resp ->
                    resolvedUrl = resp.request.url.toString()
                    if (resp.isSuccessful) {
                        preFetchedHtml = resp.body?.string()
                    }
                }
            } catch (_: Exception) {
                // Keep initialUrl if offline
            }
        }

        val domain = extractDomain(resolvedUrl).ifBlank { initialDomain.ifBlank { "link" } }
        val platformFolder = FolderEntity.resolvePlatformSubfolder(resolvedUrl, sharedText, sourcePackage)
        val platformTag = platformFolder.name.lowercase()
            .replace(" (twitter)", "")
            .replace(" ", "-")

        val ytVideoId = extractYouTubeId(resolvedUrl) ?: extractYouTubeId(initialUrl)
        val ytThumbnail = if (ytVideoId != null) {
            "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg"
        } else ""

        val fallbackTitle = when {
            !hintSubject.isNullOrBlank() -> hintSubject.trim()
            else -> generateFallbackTitle(sharedText, resolvedUrl, domain, platformFolder.name)
        }

        val baseTags = buildList {
            add("link")
            add(platformTag)
            if (domain.isNotBlank() && !domain.equals(platformTag, ignoreCase = true)) {
                add(domain.substringBefore('.').lowercase())
            }
            if (ytVideoId != null || platformFolder.id in listOf(
                    FolderEntity.YOUTUBE_SUBFOLDER_ID,
                    FolderEntity.TIKTOK_SUBFOLDER_ID,
                    FolderEntity.TWITCH_SUBFOLDER_ID,
                    FolderEntity.PORN_SUBFOLDER_ID
                )
            ) {
                add("video")
            }
        }.distinct()

        // 1. YouTube & YouTube Shorts (Fast oEmbed + HQ thumbnail)
        if (ytVideoId != null) {
            try {
                val oEmbedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$ytVideoId&format=json"
                val request = Request.Builder()
                    .url(oEmbedUrl)
                    .header("User-Agent", DESKTOP_CHROME_USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val title = json.optString("title", fallbackTitle)
                            val author = json.optString("author_name", "")
                            val thumb = json.optString("thumbnail_url", ytThumbnail)
                            return@withContext LinkMetadata(
                                url = resolvedUrl,
                                title = title.ifBlank { fallbackTitle },
                                description = if (author.isNotBlank()) "YouTube • $author" else "Shared from YouTube",
                                imageUrl = thumb.ifBlank { ytThumbnail },
                                domain = "youtube.com",
                                isYouTube = true,
                                platformName = FolderEntity.PermanentYouTubeFolder.name,
                                subfolderId = FolderEntity.YOUTUBE_SUBFOLDER_ID,
                                suggestedTags = baseTags
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Offline fallback uses deterministic ytThumbnail
            }
            return@withContext LinkMetadata(
                url = resolvedUrl,
                title = fallbackTitle,
                description = "Shared from YouTube",
                imageUrl = ytThumbnail,
                domain = "youtube.com",
                isYouTube = true,
                platformName = FolderEntity.PermanentYouTubeFolder.name,
                subfolderId = FolderEntity.YOUTUBE_SUBFOLDER_ID,
                suggestedTags = baseTags
            )
        }

        // 2. X (Twitter) — FxTwitter API + Twitter oEmbed for reliable tweet text, author & media
        val twitterMatch = TWITTER_STATUS_REGEX.find(resolvedUrl)
        if (twitterMatch != null || platformFolder.id == FolderEntity.X_TWITTER_SUBFOLDER_ID) {
            if (twitterMatch != null) {
                val screenName = twitterMatch.groupValues[1]
                val statusId = twitterMatch.groupValues[2]
                try {
                    val fxUrl = "https://api.fxtwitter.com/$screenName/status/$statusId"
                    val req = Request.Builder()
                        .url(fxUrl)
                        .header("User-Agent", "NotesTasksApp/1.0")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (!body.isNullOrBlank()) {
                                val tweet = JSONObject(body).optJSONObject("tweet")
                                if (tweet != null) {
                                    val text = tweet.optString("text", "")
                                    val authorObj = tweet.optJSONObject("author")
                                    val authorName = authorObj?.optString("name", screenName) ?: screenName
                                    val authorHandle = authorObj?.optString("screen_name", screenName) ?: screenName
                                    val mediaObj = tweet.optJSONObject("media")
                                    val photos = mediaObj?.optJSONArray("photos")
                                    val videos = mediaObj?.optJSONArray("videos")
                                    val thumbUrl = when {
                                        photos != null && photos.length() > 0 -> photos.optJSONObject(0)?.optString("url", "") ?: ""
                                        videos != null && videos.length() > 0 -> videos.optJSONObject(0)?.optString("thumbnail_url", "") ?: ""
                                        else -> authorObj?.optString("avatar_url", "") ?: ""
                                    }
                                    val cleanTitle = if (text.isNotBlank()) {
                                        "$authorName (@$authorHandle): ${text.lineSequence().first().take(72)}"
                                    } else {
                                        "$authorName (@$authorHandle) on X"
                                    }
                                    return@withContext LinkMetadata(
                                        url = resolvedUrl,
                                        title = cleanTitle,
                                        description = text.ifBlank { "Shared from X (@$authorHandle)" },
                                        imageUrl = thumbUrl,
                                        domain = "x.com",
                                        isYouTube = false,
                                        platformName = platformFolder.name,
                                        subfolderId = platformFolder.id,
                                        suggestedTags = baseTags
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. TikTok oEmbed API (returns caption, @creator, and cover thumbnail)
        if (platformFolder.id == FolderEntity.TIKTOK_SUBFOLDER_ID) {
            try {
                val encoded = URLEncoder.encode(resolvedUrl, "UTF-8")
                val oembedUrl = "https://www.tiktok.com/oembed?url=$encoded"
                val req = Request.Builder()
                    .url(oembedUrl)
                    .header("User-Agent", DESKTOP_CHROME_USER_AGENT)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val title = json.optString("title", "")
                            val author = json.optString("author_name", "")
                            val thumb = json.optString("thumbnail_url", "")
                            if (title.isNotBlank() || thumb.isNotBlank()) {
                                val displayTitle = when {
                                    title.isNotBlank() && author.isNotBlank() -> "$author: ${title.take(80)}"
                                    title.isNotBlank() -> title.take(85)
                                    author.isNotBlank() -> "TikTok by @$author"
                                    else -> fallbackTitle
                                }
                                return@withContext LinkMetadata(
                                    url = resolvedUrl,
                                    title = displayTitle,
                                    description = if (title.isNotBlank()) title else "TikTok video by @$author",
                                    imageUrl = thumb,
                                    domain = "tiktok.com",
                                    isYouTube = false,
                                    platformName = platformFolder.name,
                                    subfolderId = platformFolder.id,
                                    suggestedTags = baseTags
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Reddit oEmbed API
        if (platformFolder.id == FolderEntity.REDDIT_SUBFOLDER_ID) {
            try {
                val encoded = URLEncoder.encode(resolvedUrl, "UTF-8")
                val oembedUrl = "https://www.reddit.com/oembed?url=$encoded"
                val req = Request.Builder()
                    .url(oembedUrl)
                    .header("User-Agent", DESKTOP_CHROME_USER_AGENT)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val title = json.optString("title", "")
                            val author = json.optString("author_name", "")
                            val thumb = json.optString("thumbnail_url", "")
                            if (title.isNotBlank() && thumb.isNotBlank()) {
                                return@withContext LinkMetadata(
                                    url = resolvedUrl,
                                    title = cleanHtmlEntities(title),
                                    description = if (author.isNotBlank()) "Posted by u/$author on Reddit" else "Shared from Reddit",
                                    imageUrl = thumb,
                                    domain = "reddit.com",
                                    isYouTube = false,
                                    platformName = platformFolder.name,
                                    subfolderId = platformFolder.id,
                                    suggestedTags = baseTags
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 5. Discord Invite Metadata API (for discord.gg / discord.com/invite links)
        val discordInviteMatch = DISCORD_INVITE_REGEX.find(resolvedUrl)
        if (discordInviteMatch != null) {
            val code = discordInviteMatch.groupValues[1]
            try {
                val apiUrl = "https://discord.com/api/v9/invites/$code?with_counts=true"
                val req = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", DESKTOP_CHROME_USER_AGENT)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val guild = json.optJSONObject("guild")
                            if (guild != null) {
                                val guildId = guild.optString("id", "")
                                val guildName = guild.optString("name", "Discord Server")
                                val guildDesc = guild.optString("description", "")
                                val iconHash = guild.optString("icon", "")
                                val splashHash = guild.optString("splash", "")
                                val memberCount = json.optInt("approximate_member_count", 0)
                                val iconUrl = when {
                                    splashHash.isNotBlank() && guildId.isNotBlank() ->
                                        "https://cdn.discordapp.com/splashes/$guildId/$splashHash.png?size=512"
                                    iconHash.isNotBlank() && guildId.isNotBlank() ->
                                        "https://cdn.discordapp.com/icons/$guildId/$iconHash.png?size=256"
                                    else -> ""
                                }
                                val desc = buildString {
                                    if (guildDesc.isNotBlank()) append(guildDesc)
                                    if (memberCount > 0) {
                                        if (isNotEmpty()) append(" • ")
                                        append("$memberCount members")
                                    }
                                }.ifBlank { "Discord Server Invite ($resolvedUrl)" }
                                return@withContext LinkMetadata(
                                    url = resolvedUrl,
                                    title = "Discord: $guildName",
                                    description = desc,
                                    imageUrl = iconUrl,
                                    domain = "discord.com",
                                    isYouTube = false,
                                    platformName = platformFolder.name,
                                    subfolderId = platformFolder.id,
                                    suggestedTags = baseTags
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 6. Platform-specific defaults before HTML OpenGraph & JSON-LD extraction
        var platformImage = ""
        var platformDesc = "Shared from ${platformFolder.name}: $resolvedUrl"
        var platformTitle = fallbackTitle

        when (platformFolder.id) {
            FolderEntity.INSTAGRAM_SUBFOLDER_ID -> {
                val isReel = resolvedUrl.contains("/reel/") || resolvedUrl.contains("/reels/")
                if (platformTitle == fallbackTitle) {
                    platformTitle = if (isReel) "Instagram Reel" else "Instagram Post"
                }
                val cleanMedia = resolvedUrl.substringBefore("?").trimEnd('/')
                platformImage = "$cleanMedia/media/?size=l"
            }
            FolderEntity.PINTEREST_SUBFOLDER_ID -> {
                val pinMatch = Regex("/pin/([\\w-]+)").find(resolvedUrl)
                if (pinMatch != null) {
                    platformDesc = "Pinterest Pin #${pinMatch.groupValues[1]}"
                }
            }
            FolderEntity.FACEBOOK_SUBFOLDER_ID -> {
                val isWatch = resolvedUrl.contains("watch") || resolvedUrl.contains("/reel") || resolvedUrl.contains("/videos/")
                if (platformTitle == fallbackTitle) {
                    platformTitle = if (isWatch) "Facebook Video" else "Facebook Post"
                }
            }
            FolderEntity.THREADS_SUBFOLDER_ID -> {
                val handleMatch = Regex("threads\\.(?:net|com)/@([A-Za-z0-9_.]+)").find(resolvedUrl)
                if (handleMatch != null && platformTitle == fallbackTitle) {
                    platformTitle = "Threads Post by @${handleMatch.groupValues[1]}"
                }
            }
            FolderEntity.TWITCH_SUBFOLDER_ID -> {
                val channelMatch = Regex("twitch\\.tv/([A-Za-z0-9_]+)").find(resolvedUrl)
                if (channelMatch != null && platformTitle == fallbackTitle) {
                    platformTitle = "Twitch • ${channelMatch.groupValues[1]}"
                }
            }
            FolderEntity.SNAPCHAT_SUBFOLDER_ID -> {
                if (platformTitle == fallbackTitle) {
                    platformTitle = if (resolvedUrl.contains("/spotlight/")) "Snapchat Spotlight" else "Snapchat Story / Link"
                }
            }
            FolderEntity.PORN_SUBFOLDER_ID -> {
                platformDesc = "Saved video link from $domain"
            }
        }

        // 7. Direct HTML OpenGraph + Twitter Card + JSON-LD extraction
        // Social platforms (Instagram, Facebook, Threads, Snapchat, Twitch, Discord, Pinterest, Reddit)
        // serve rich server-side OG meta tags when queried with TelegramBot/crawler User-Agent.
        val useCrawlerUa = platformFolder.id in listOf(
            FolderEntity.INSTAGRAM_SUBFOLDER_ID,
            FolderEntity.FACEBOOK_SUBFOLDER_ID,
            FolderEntity.THREADS_SUBFOLDER_ID,
            FolderEntity.SNAPCHAT_SUBFOLDER_ID,
            FolderEntity.TWITCH_SUBFOLDER_ID,
            FolderEntity.DISCORD_SUBFOLDER_ID,
            FolderEntity.PINTEREST_SUBFOLDER_ID,
            FolderEntity.REDDIT_SUBFOLDER_ID,
            FolderEntity.X_TWITTER_SUBFOLDER_ID
        )

        var parsedMeta: LinkMetadata? = null
        try {
            val htmlBody = preFetchedHtml ?: run {
                val request = Request.Builder()
                    .url(resolvedUrl)
                    .header("User-Agent", if (useCrawlerUa) SOCIAL_CRAWLER_USER_AGENT else DESKTOP_CHROME_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()

                client.newCall(request).execute().use { response ->
                    resolvedUrl = response.request.url.toString()
                    if (response.isSuccessful) response.body?.string() else null
                }
            }

            if (!htmlBody.isNullOrBlank()) {
                parsedMeta = parseHtmlMetadata(
                    html = htmlBody,
                    url = resolvedUrl,
                    domain = domain,
                    fallbackTitle = platformTitle,
                    platformFolder = platformFolder,
                    suggestedTags = baseTags
                )
            }
        } catch (_: Exception) {
            // Continue to fallback
        }

        val currentImage = parsedMeta?.imageUrl?.ifBlank { platformImage } ?: platformImage
        val currentTitle = parsedMeta?.title?.takeIf { it.isNotBlank() && !isGenericWallTitle(it) } ?: platformTitle
        val currentDesc = parsedMeta?.description?.ifBlank { platformDesc } ?: platformDesc

        // 8. If image or title is still missing/generic, call Microlink Public Metadata API
        if (currentImage.isBlank() || currentTitle == fallbackTitle || isGenericWallTitle(currentTitle)) {
            try {
                val encodedUrl = URLEncoder.encode(resolvedUrl, "UTF-8")
                val apiUrl = "https://api.microlink.io?url=$encodedUrl"
                val apiRequest = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "NotesTasksApp/1.0")
                    .build()

                client.newCall(apiRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            if (json.optString("status") == "success") {
                                val data = json.optJSONObject("data")
                                if (data != null) {
                                    val apiTitle = data.optString("title", "").takeIf { !isGenericWallTitle(it) } ?: ""
                                    val apiDesc = data.optString("description", "")
                                    val apiImgObj = data.optJSONObject("image") ?: data.optJSONObject("logo")
                                    val apiImgUrl = apiImgObj?.optString("url", "") ?: ""

                                    val finalImage = apiImgUrl.ifBlank { currentImage }
                                        .ifBlank { highResFavicon(domain) }

                                    return@withContext LinkMetadata(
                                        url = resolvedUrl,
                                        title = if (apiTitle.isNotBlank()) apiTitle else currentTitle,
                                        description = if (apiDesc.isNotBlank()) apiDesc else currentDesc,
                                        imageUrl = finalImage,
                                        domain = domain,
                                        isYouTube = false,
                                        platformName = platformFolder.name,
                                        subfolderId = platformFolder.id,
                                        suggestedTags = baseTags
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore API failure, proceed with resolved fields
            }
        }

        LinkMetadata(
            url = resolvedUrl,
            title = currentTitle,
            description = currentDesc,
            imageUrl = currentImage.ifBlank { highResFavicon(domain) },
            domain = domain,
            isYouTube = false,
            platformName = platformFolder.name,
            subfolderId = platformFolder.id,
            suggestedTags = baseTags
        )
    }

    private const val DESKTOP_CHROME_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private const val SOCIAL_CRAWLER_USER_AGENT =
        "Mozilla/5.0 (compatible; TelegramBot/1.0; +https://core.telegram.org/bots)"

    private fun highResFavicon(domain: String): String {
        val clean = domain.trim()
        if (clean.isBlank() || clean == "link" || !clean.contains(".")) return ""
        return "https://www.google.com/s2/favicons?domain=$clean&sz=128"
    }

    private fun isGenericWallTitle(title: String): Boolean {
        val lower = title.trim().lowercase()
        return lower in listOf(
            "instagram", "login • instagram", "facebook", "log in or sign up to view",
            "x", "x.com", "twitter", "reddit - dive into anything", "pinterest",
            "tiktok - make your day", "snapchat", "threads", "discord", "just a moment..."
        )
    }

    private fun parseHtmlMetadata(
        html: String,
        url: String,
        domain: String,
        fallbackTitle: String,
        platformFolder: FolderEntity,
        suggestedTags: List<String>
    ): LinkMetadata {
        // 1. OpenGraph & Twitter Cards
        var ogTitle = findMetaContent(html, "property", "og:title")
            ?: findMetaContent(html, "name", "twitter:title")
            ?: findMetaContent(html, "property", "twitter:title")

        var ogDesc = findMetaContent(html, "property", "og:description")
            ?: findMetaContent(html, "name", "twitter:description")
            ?: findMetaContent(html, "name", "description")

        var ogImage = findMetaContent(html, "property", "og:image")
            ?: findMetaContent(html, "property", "og:image:secure_url")
            ?: findMetaContent(html, "property", "og:image:url")
            ?: findMetaContent(html, "name", "twitter:image")
            ?: findMetaContent(html, "name", "twitter:image:src")
            ?: findMetaContent(html, "itemprop", "image")
            ?: findMetaContent(html, "itemprop", "thumbnailUrl")

        // 2. JSON-LD (<script type="application/ld+json">) for VideoObject, Article, Pin, etc.
        if (ogTitle.isNullOrBlank() || ogImage.isNullOrBlank()) {
            val ldResult = extractJsonLdMetadata(html)
            if (ogTitle.isNullOrBlank() && !ldResult.first.isNullOrBlank()) {
                ogTitle = ldResult.first
            }
            if (ogDesc.isNullOrBlank() && !ldResult.second.isNullOrBlank()) {
                ogDesc = ldResult.second
            }
            if (ogImage.isNullOrBlank() && !ldResult.third.isNullOrBlank()) {
                ogImage = ldResult.third
            }
        }

        if (ogTitle.isNullOrBlank()) {
            ogTitle = findTagContent(html, "title")
        }

        if (ogImage.isNullOrBlank()) {
            ogImage = findLinkHref(html, "image_src")
                ?: findLinkHref(html, "apple-touch-icon")
                ?: findLinkHref(html, "icon")
        }

        if (ogImage != null && !ogImage.startsWith("http://") && !ogImage.startsWith("https://")) {
            ogImage = resolveRelativeUrl(url, ogImage)
        }

        val cleanedTitle = cleanHtmlEntities(ogTitle?.trim() ?: fallbackTitle)
        val cleanedDesc = cleanHtmlEntities(ogDesc?.trim() ?: "")

        return LinkMetadata(
            url = url,
            title = if (cleanedTitle.isNotBlank() && !isGenericWallTitle(cleanedTitle)) cleanedTitle else fallbackTitle,
            description = cleanedDesc,
            imageUrl = cleanHtmlEntities(ogImage?.trim() ?: ""),
            domain = domain,
            isYouTube = false,
            platformName = platformFolder.name,
            subfolderId = platformFolder.id,
            suggestedTags = suggestedTags
        )
    }

    private fun extractJsonLdMetadata(html: String): Triple<String?, String?, String?> {
        try {
            val pattern = Pattern.compile(
                "<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>([\\s\\S]*?)</script>",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = pattern.matcher(html)
            while (matcher.find()) {
                val rawJson = matcher.group(1)?.trim() ?: continue
                val obj = when {
                    rawJson.startsWith("{") -> JSONObject(rawJson)
                    rawJson.startsWith("[") -> JSONArray(rawJson).optJSONObject(0)
                    else -> null
                } ?: continue

                val title = obj.optString("headline").ifBlank { obj.optString("name") }
                val desc = obj.optString("description")
                var image = obj.optString("thumbnailUrl")
                if (image.isBlank()) {
                    val imgField = obj.opt("image")
                    image = when (imgField) {
                        is String -> imgField
                        is JSONObject -> imgField.optString("url")
                        is JSONArray -> {
                            val first = imgField.opt(0)
                            if (first is String) first else (first as? JSONObject)?.optString("url") ?: ""
                        }
                        else -> ""
                    }
                }
                if (title.isNotBlank() || image.isNotBlank()) {
                    return Triple(title.ifBlank { null }, desc.ifBlank { null }, image.ifBlank { null })
                }
            }
        } catch (_: Exception) {}
        return Triple(null, null, null)
    }

    private fun findMetaContent(html: String, attrName: String, attrValue: String): String? {
        val pattern = Pattern.compile(
            "<meta\\s+[^>]*$attrName=[\"']${Pattern.quote(attrValue)}[\"'][^>]*content=[\"']([^\"']*)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)
        }
        val patternRev = Pattern.compile(
            "<meta\\s+[^>]*content=[\"']([^\"']*)[\"'][^>]*$attrName=[\"']${Pattern.quote(attrValue)}[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        )
        val matcherRev = patternRev.matcher(html)
        if (matcherRev.find()) {
            return matcherRev.group(1)
        }
        return null
    }

    private fun findTagContent(html: String, tagName: String): String? {
        val pattern = Pattern.compile("<$tagName[^>]*>([^<]*)</$tagName>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun findLinkHref(html: String, relValue: String): String? {
        val pattern = Pattern.compile(
            "<link\\s+[^>]*rel=[\"']${Pattern.quote(relValue)}[\"'][^>]*href=[\"']([^\"']*)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)
        }
        val patternRev = Pattern.compile(
            "<link\\s+[^>]*href=[\"']([^\"']*)[\"'][^>]*rel=[\"']${Pattern.quote(relValue)}[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        )
        val matcherRev = patternRev.matcher(html)
        if (matcherRev.find()) {
            return matcherRev.group(1)
        }
        return null
    }

    private fun resolveRelativeUrl(baseUrl: String, relativePath: String): String {
        return try {
            val base = URI(baseUrl)
            base.resolve(relativePath).toString()
        } catch (_: Exception) {
            relativePath
        }
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
            .replace("&#x3A;", ":")
            .replace("&nbsp;", " ")
            .trim()
    }

    private fun generateFallbackTitle(
        sharedText: String,
        url: String,
        domain: String,
        platformName: String
    ): String {
        val remaining = sharedText.replace(url, "").trim().trim('-', ':', '|', '•')
        if (remaining.isNotBlank() && remaining.length > 3) {
            return remaining.lineSequence().first().trim().take(85)
        }
        return try {
            val uri = URI(url)
            val segments = (uri.path ?: "")
                .split('/')
                .map { it.trim() }
                .filter { it.isNotBlank() && it !in listOf("p", "reel", "reels", "shorts", "watch", "pin", "status", "video", "v", "r", "comments", "view_video.php") }

            val meaningfulSegment = segments.lastOrNull { seg ->
                seg.length > 2 && !seg.all { it.isDigit() }
            } ?: segments.lastOrNull()

            if (!meaningfulSegment.isNullOrBlank()) {
                val humanized = meaningfulSegment
                    .substringBeforeLast('.')
                    .replace('-', ' ')
                    .replace('_', ' ')
                    .replace('+', ' ')
                    .trim()
                if (humanized.isNotBlank()) {
                    "$platformName • ${humanized.replaceFirstChar { it.uppercase() }.take(70)}"
                } else {
                    "$platformName Link ($domain)"
                }
            } else {
                "$platformName Link ($domain)"
            }
        } catch (_: Exception) {
            if (platformName.isNotBlank()) "$platformName Link" else "Shared Link"
        }
    }
}
