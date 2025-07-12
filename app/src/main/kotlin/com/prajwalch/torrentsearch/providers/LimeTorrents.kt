package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Provider implementation for [LimeTorrents](https://www.limetorrents.lol).
 *
 * Extracts torrent results from the HTML search page.
 * This provider uses InfoHash, not Magnet URIs.
 */
class LimeTorrents(override val id: SearchProviderId) : SearchProvider {
    override fun specializedCategory(): Category = Category.All

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val categoryString = getCategoryString(context.category)
        val requestUrl = "$BASE_URL/search/$categoryString/$query/date/1/"

        val responseHtml = context.httpClient.get(url = requestUrl)

        return withContext(Dispatchers.Default) {
            parseSearchResults(html = responseHtml)
        }
    }

    /** Returns the category string used by it. */
    private fun getCategoryString(category: Category): String = when (category) {
        Category.All, Category.Books, Category.Porn -> "all"
        Category.Anime -> "anime"
        Category.Apps -> "applications"
        Category.Games -> "games"
        Category.Movies -> "movies"
        Category.Music -> "music"
        Category.Series -> "tv"
        Category.Other -> "other"
    }

    /** Parses all torrent rows from the result HTML page. */
    private fun parseSearchResults(html: String): List<Torrent> {
        val rows = Jsoup.parse(html).select(".table2 > tbody > tr[bgcolor]")
        return rows.mapNotNull(::parseRowCatching)
    }

    /** Attempts to parse a single row; returns null if it fails. */
    private fun parseRowCatching(row: Element): Torrent? {
        return try {
            parseRow(row)
        } catch (_: Exception) {
            null
        }
    }

    /** Parses a valid torrent row into a [Torrent] object. */
    private fun parseRow(row: Element): Torrent? {
        val nameAnchor = row.selectFirst("div.tt-name > a[href^=/]") ?: return null
        val name = nameAnchor.text()
        val descriptionPageUrl = BASE_URL + nameAnchor.attr("href")

        val infoHash = extractInfoHash(row) ?: return null
        val uploadDate = extractUploadDate(row)
        val size = row.selectFirst("td:nth-child(3)")?.text() ?: return null
        val seeds = row.selectFirst(".tdseed")?.text()?.toUIntOrNull() ?: 0u
        val peers = row.selectFirst(".tdleech")?.text()?.toUIntOrNull() ?: 0u
        val category = extractCategory(row)

        return Torrent(
            name = name,
            size = size,
            seeds = seeds,
            peers = peers,
            providerId = id,
            providerName = PROVIDER_NAME,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.InfoHash(infoHash),
        )
    }

    /** Extracts the InfoHash from a magnet link element. */
    private fun extractInfoHash(row: Element): String? {
        val link = row.select("a[class^=csprite][href^=http]")
            .mapNotNull { it.attr("href") }
            .firstOrNull { it.contains("/torrent/") } ?: return null

        return INFO_HASH_REGEX.find(link)?.groupValues?.get(1)
    }

    /**
     * Extracts the upload date from the second column, trimming off " - in Movies".
     * e.g. "27 days ago - in Movies" → "27 days ago"
     */
    private fun extractUploadDate(row: Element): String {
        // TODO: The upload date here is not in [dd MMM yyyy] format.
        //  We are currently using the raw relative format from the site (e.g., "27 days ago").
        //  Consider parsing and converting it to a consistent date format if needed later.
        return row.selectFirst("td:nth-child(2)")
            ?.text()
            ?.substringBefore(" -")
            ?.trim()
            .orEmpty()
    }

    /** Detects category heuristically from the second column's text. */
    private fun extractCategory(row: Element): Category {
        val rawText = row.selectFirst("td:nth-child(2)")?.text().orEmpty()

        return when {
            rawText.contains("TV", ignoreCase = true) -> Category.Series
            rawText.contains("Movie", ignoreCase = true) -> Category.Movies
            rawText.contains("Music", ignoreCase = true) -> Category.Music
            rawText.contains("App", ignoreCase = true) -> Category.Apps
            rawText.contains("E-book", ignoreCase = true) -> Category.Books
            rawText.contains("Anime", ignoreCase = true) -> Category.Anime
            rawText.contains("Games", ignoreCase = true) -> Category.Games
            else -> Category.Other
        }
    }

    private companion object {
        private const val BASE_URL = "https://www.limetorrents.lol"
        private const val PROVIDER_NAME = "limetorrents.lol"
        private val INFO_HASH_REGEX = Regex("""/torrent/([A-Fa-f0-9]{40})\.torrent""")
    }
}