package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient

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
class LimeTorrents : SearchProvider {
    override val info = SearchProviderInfo(
        id = "limetorrents",
        name = "LimeTorrents",
        url = "https://limetorrents.lol",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Unsafe(
            reason = R.string.limetorrents_unsafe_reason,
        ),
        enabledByDefault = false,
    )

    private val resultsPageParser = LimeTorrentsResultsPageParser(info.name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val categoryString = getCategoryString(context.category)
        val requestUrl = "${info.url}/search/$categoryString/$query/date/1/"

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return LimeTorrentsDetailsPageParser.parse(responseHtml)
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
}

private class LimeTorrentsResultsPageParser(
    private val providerName: String,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            val rows = Jsoup.parse(html, pageUrl).select(".table2 > tbody > tr[bgcolor]")
            rows.mapNotNull(::parseRowCatching)
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
        val descriptionPageUrl = nameAnchor.attr("abs:href")

        val infoHash = extractInfoHash(row) ?: return null
        val uploadDate = extractUploadDate(row)
        val size = row.selectFirst("td:nth-child(3)")?.text() ?: return null
        val seeders = row.selectFirst(".tdseed")?.text()?.toUIntOrNull() ?: 0u
        val peers = row.selectFirst(".tdleech")?.text()?.toUIntOrNull() ?: 0u
        val category = extractCategory(row)

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = descriptionPageUrl,
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
        private val INFO_HASH_REGEX = Regex("""/torrent/([A-Fa-f0-9]{40})\.torrent""")
    }
}

private object LimeTorrentsDetailsPageParser {
    private const val INFO_HASH =
        "#content > div:nth-child(6) > div:nth-child(1) > div > table > tbody > tr:nth-child(1) > td:nth-child(2)"
    private const val NAME = "#content > h1"
    private const val SIZE =
        "#content > div:nth-child(6) > div:nth-child(1) > div > table > tbody > tr:nth-child(3) > td:nth-child(2)"
    private const val SEEDERS = "#content > span.greenish"
    private const val PEERS = "#content > span.reddish"
    private const val UPLOAD_DATE_AND_CATEGORY =
        "#content > div:nth-child(6) > div:nth-child(1) > div > table > tbody > tr:nth-child(2) > td:nth-child(2)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK =
        "#content > div:nth-child(6) > div:nth-child(1) > div > div:nth-child(7) > div > a"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val infoHash = html.selectFirst(INFO_HASH)?.ownText()?.lowercase()
            ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
        val size = html.selectFirst(SIZE)?.ownText()
        val seeders = html.selectFirst(SEEDERS)
            ?.ownText()
            ?.removePrefix("Seeders : ")
            ?.trim()
            ?.toUIntOrNull()
        val peers = html.selectFirst(PEERS)
            ?.ownText()
            ?.removePrefix("Leechers : ")
            ?.trim()
            ?.toUIntOrNull()
        val (uploadDate, category) = html.selectFirst(UPLOAD_DATE_AND_CATEGORY)
            ?.text()
            ?.split("in", limit = 2)
            ?: listOf(null, null)
        val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("href")

        TorrentDetails(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate?.trim(),
            category = category?.trim(),
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }
}