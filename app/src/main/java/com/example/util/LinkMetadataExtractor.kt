package com.example.util

import android.util.Patterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
    val isYouTube: Boolean = false
)

object LinkMetadataExtractor {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val YOUTUBE_REGEX = Regex(
        "(?:youtu\\.be/|youtube\\.com/(?:embed/|v/|shorts/|watch\\?v=|watch\\?.+&v=))([\\w-]{11})",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extracts the first valid HTTP/HTTPS URL from any shared text.
     */
    fun extractUrl(text: String): String? {
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            var candidate = matcher.group()
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
            host.removePrefix("www.")
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
     * Resolves metadata for the given URL or shared text.
     * Supports:
     * - YouTube Shorts & Videos (Deterministic high-res thumbnail + oEmbed title/author)
     * - Instagram (Posts & Reels preview image heuristics + metadata)
     * - Facebook & Pinterest
     * - Universal OpenGraph / Twitter Cards / HTML meta tags
     * - Microlink public fallback API for JavaScript-rendered & social pages
     */
    suspend fun resolve(sharedText: String, hintSubject: String? = null): LinkMetadata = withContext(Dispatchers.IO) {
        val extractedUrl = extractUrl(sharedText) ?: sharedText.trim()
        val domain = extractDomain(extractedUrl).ifBlank { "link" }
        val ytVideoId = extractYouTubeId(extractedUrl)

        // For YouTube, thumbnail URL is deterministic and available completely offline
        val ytThumbnail = if (ytVideoId != null) {
            "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg"
        } else ""

        val fallbackTitle = when {
            !hintSubject.isNullOrBlank() -> hintSubject.trim()
            else -> generateFallbackTitle(sharedText, extractedUrl, domain)
        }

        var metadata = LinkMetadata(
            url = extractedUrl,
            title = fallbackTitle,
            description = "",
            imageUrl = ytThumbnail,
            domain = domain,
            isYouTube = ytVideoId != null
        )

        // 1. YouTube & YouTube Shorts (Fast oEmbed + HQ thumbnail)
        if (ytVideoId != null) {
            try {
                val oEmbedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$ytVideoId&format=json"
                val request = Request.Builder()
                    .url(oEmbedUrl)
                    .header("User-Agent", "Mozilla/5.0")
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
                                url = extractedUrl,
                                title = title,
                                description = if (author.isNotBlank()) "Channel: $author" else "",
                                imageUrl = thumb.ifBlank { ytThumbnail },
                                domain = "youtube.com",
                                isYouTube = true
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Offline or timeout; use fallback metadata with offline YouTube thumbnail!
            }
            return@withContext metadata
        }

        // 2. Platform-specific image & metadata heuristics
        var platformImage = ""
        var platformDesc = ""
        var platformTitle = ""

        if (domain.contains("instagram.com")) {
            val isReel = extractedUrl.contains("/reel/") || extractedUrl.contains("/reels/")
            platformTitle = if (isReel) "Instagram Reel" else "Instagram Post"
            platformDesc = "Shared from Instagram: $extractedUrl"
            val cleanMedia = extractedUrl.substringBefore("?").trimEnd('/')
            platformImage = "$cleanMedia/media/?size=l"
        } else if (domain.contains("pinterest.com") || domain.contains("pin.it")) {
            platformTitle = "Pinterest Pin"
            platformDesc = "Saved from Pinterest: $extractedUrl"
            // For pin URLs like /pin/12345/, try to extract pin ID
            val pinMatch = Regex("/pin/(\\d+)").find(extractedUrl)
            if (pinMatch != null) {
                val pinId = pinMatch.groupValues[1]
                platformDesc = "Pinterest Pin #$pinId"
            }
        } else if (domain.contains("facebook.com") || domain.contains("fb.watch") || domain.contains("fb.com")) {
            val isWatch = extractedUrl.contains("watch") || extractedUrl.contains("/reel")
            platformTitle = if (isWatch) "Facebook Video" else "Facebook Post"
            platformDesc = "Shared from Facebook: $extractedUrl"
        } else if (domain.contains("twitter.com") || domain.contains("x.com")) {
            platformTitle = "X / Twitter Post"
            platformDesc = "Post shared from X ($extractedUrl)"
        } else if (domain.contains("tiktok.com")) {
            platformTitle = "TikTok Video"
            platformDesc = "Shared from TikTok: $extractedUrl"
        } else if (domain.contains("reddit.com") || domain.contains("redd.it")) {
            platformTitle = "Reddit Post"
            platformDesc = "Shared from Reddit: $extractedUrl"
        }

        // 3. Attempt direct HTML / OpenGraph parsing with browser headers & follow redirects
        try {
            val request = Request.Builder()
                .url(extractedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        metadata = parseHtmlMetadata(body, extractedUrl, domain, fallbackTitle)
                    }
                }
            }
        } catch (_: Exception) {
            // Direct request failure or blocked by bot protection
        }

        // 4. If image or title is still missing, call Microlink Public Metadata API
        val currentImage = metadata.imageUrl.ifBlank { platformImage }
        val currentTitle = if (metadata.title != fallbackTitle && metadata.title.isNotBlank()) metadata.title else platformTitle.ifBlank { fallbackTitle }
        val currentDesc = metadata.description.ifBlank { platformDesc }

        if (currentImage.isBlank() || currentTitle == fallbackTitle) {
            try {
                val encodedUrl = URLEncoder.encode(extractedUrl, "UTF-8")
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
                                    val apiTitle = data.optString("title", "")
                                    val apiDesc = data.optString("description", "")
                                    val apiImgObj = data.optJSONObject("image") ?: data.optJSONObject("logo")
                                    val apiImgUrl = apiImgObj?.optString("url", "") ?: ""

                                    return@withContext LinkMetadata(
                                        url = extractedUrl,
                                        title = if (apiTitle.isNotBlank()) apiTitle else currentTitle,
                                        description = if (apiDesc.isNotBlank()) apiDesc else currentDesc,
                                        imageUrl = if (apiImgUrl.isNotBlank()) apiImgUrl else currentImage,
                                        domain = domain,
                                        isYouTube = false
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore API failure, proceed with platform heuristics
            }
        }

        // Return combined metadata with platform heuristics filled in
        LinkMetadata(
            url = extractedUrl,
            title = currentTitle,
            description = currentDesc,
            imageUrl = currentImage,
            domain = domain,
            isYouTube = false
        )
    }

    private fun parseHtmlMetadata(html: String, url: String, domain: String, fallbackTitle: String): LinkMetadata {
        // Find title: og:title -> twitter:title -> <title>
        val ogTitle = findMetaContent(html, "property", "og:title")
            ?: findMetaContent(html, "name", "twitter:title")
            ?: findTagContent(html, "title")

        // Find description: og:description -> twitter:description -> meta description
        val ogDesc = findMetaContent(html, "property", "og:description")
            ?: findMetaContent(html, "name", "twitter:description")
            ?: findMetaContent(html, "name", "description")

        // Find image: og:image -> twitter:image -> link rel=image_src -> link rel=apple-touch-icon / icon
        var ogImage = findMetaContent(html, "property", "og:image")
            ?: findMetaContent(html, "property", "og:image:url")
            ?: findMetaContent(html, "property", "og:image:secure_url")
            ?: findMetaContent(html, "name", "twitter:image")
            ?: findMetaContent(html, "name", "twitter:image:src")
            ?: findLinkHref(html, "image_src")
            ?: findLinkHref(html, "apple-touch-icon")
            ?: findLinkHref(html, "icon")

        if (ogImage != null && !ogImage.startsWith("http://") && !ogImage.startsWith("https://")) {
            ogImage = resolveRelativeUrl(url, ogImage)
        }

        val cleanedTitle = cleanHtmlEntities(ogTitle?.trim() ?: fallbackTitle)
        val cleanedDesc = cleanHtmlEntities(ogDesc?.trim() ?: "")

        return LinkMetadata(
            url = url,
            title = cleanedTitle.ifBlank { fallbackTitle },
            description = cleanedDesc,
            imageUrl = ogImage?.trim() ?: "",
            domain = domain,
            isYouTube = false
        )
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
            .replace("&nbsp;", " ")
            .trim()
    }

    private fun generateFallbackTitle(sharedText: String, url: String, domain: String): String {
        val remaining = sharedText.replace(url, "").trim().trim('-', ':', '|')
        if (remaining.isNotBlank() && remaining.length > 3) {
            return remaining.take(80)
        }
        return try {
            val uri = URI(url)
            val path = uri.path?.trim('/') ?: ""
            if (path.isNotBlank()) {
                val lastSegment = path.substringAfterLast('/').replace('-', ' ').replace('_', ' ')
                if (lastSegment.isNotBlank()) {
                    "$domain - ${lastSegment.replaceFirstChar { it.uppercase() }}"
                } else {
                    domain
                }
            } else {
                domain.replaceFirstChar { it.uppercase() }
            }
        } catch (_: Exception) {
            if (domain.isNotBlank()) domain else "Shared Link"
        }
    }
}
