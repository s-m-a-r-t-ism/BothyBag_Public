package com.bothybag.app.data

import android.content.Context
import android.util.Xml
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class NewsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("news_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val RSS_URL = "https://www.mountainbothies.org.uk/feed/"
        private const val KEY_NEWS = "cached_news"
        private const val KEY_TIMESTAMP = "cached_timestamp"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Get news items, using cache if fresh enough, otherwise fetching from RSS.
     * [forceRefresh] bypasses the cache age check.
     */
    suspend fun getNews(
        bothyRepo: BothyRepository,
        forceRefresh: Boolean = false
    ): Result<List<NewsItem>> {
        // Check cache first
        if (!forceRefresh && isCacheFresh()) {
            val cached = getCachedNews()
            if (cached.isNotEmpty()) return Result.success(cached)
        }

        // Fetch from RSS
        return try {
            val items = fetchRss()
            val matched = matchBothies(items, bothyRepo)
            cacheNews(matched)
            Result.success(matched)
        } catch (e: Exception) {
            // Fall back to stale cache if available
            val cached = getCachedNews()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun isCacheFresh(): Boolean {
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
        return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS
    }

    private fun getCachedNews(): List<NewsItem> {
        val json = prefs.getString(KEY_NEWS, null) ?: return emptyList()
        val type = object : TypeToken<List<NewsItem>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun cacheNews(items: List<NewsItem>) {
        prefs.edit()
            .putString(KEY_NEWS, gson.toJson(items))
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    private suspend fun fetchRss(): List<NewsItem> = withContext(Dispatchers.IO) {
        val connection = URL(RSS_URL).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "BothyBag/1.0")
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        try {
            val xml = connection.inputStream.bufferedReader().use { it.readText() }
            parseRss(xml)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRss(xml: String): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var description = ""
        var currentTag = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        inItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                        description = ""
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "title" -> title += text
                            "link" -> link += text
                            "pubDate" -> pubDate += text
                            "description" -> description += text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        inItem = false
                        items.add(
                            NewsItem(
                                title = title.trim(),
                                link = link.trim(),
                                pubDate = formatDate(pubDate.trim()),
                                description = stripHtml(description.trim()),
                            )
                        )
                    }
                    currentTag = ""
                }
            }
            parser.next()
        }

        return items
    }

    /**
     * Match news items to bothies by checking if any bothy name appears in the title.
     * Strips common suffixes like "Bothy" for broader matching.
     */
    private fun matchBothies(
        items: List<NewsItem>,
        bothyRepo: BothyRepository
    ): List<NewsItem> {
        val bothies = bothyRepo.loadBothies()

        // Build lookup: list of (normalized name variants, bothy) pairs
        val bothyLookup = bothies.flatMap { bothy ->
            val names = mutableListOf(bothy.name.lowercase())
            // Also try without "Bothy" suffix
            val stripped = bothy.name
                .replace(Regex("\\s+bothy$", RegexOption.IGNORE_CASE), "")
                .lowercase()
            if (stripped != names[0]) names.add(stripped)
            names.map { it to bothy }
        }
        // Sort by name length descending so longer names match first (avoids partial matches)
        val sortedLookup = bothyLookup.sortedByDescending { it.first.length }

        return items.map { item ->
            val titleLower = item.title.lowercase()
            val match = sortedLookup.firstOrNull { (name, _) ->
                titleLower.contains(name)
            }
            if (match != null) {
                item.copy(
                    matchedBothyId = match.second.id,
                    matchedBothyName = match.second.name
                )
            } else {
                item
            }
        }
    }

    /** Convert RSS date like "Mon, 16 Mar 2026 10:01:58 +0000" to "16 Mar 2026" */
    private fun formatDate(rssDate: String): String {
        return try {
            val input = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val output = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
            val date = input.parse(rssDate) ?: return rssDate
            output.format(date)
        } catch (e: Exception) {
            rssDate
        }
    }

    /** Strip HTML tags from RSS description */
    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#8217;", "'")
            .replace("&#8216;", "'")
            .replace("&#8211;", "-")
            .replace("&#8230;", "...")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** Get news items matching a specific bothy ID (from cache) */
    fun getNewsForBothy(bothyId: String): List<NewsItem> {
        return getCachedNews().filter { it.matchedBothyId == bothyId }
    }
}
