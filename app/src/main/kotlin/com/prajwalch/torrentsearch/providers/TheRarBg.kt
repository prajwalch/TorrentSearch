package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TheRarBg : SearchProvider, TorrentDetailsProvider {
    override val id = "therarbag"
    override val name = "TheRarBg"
    override val url = "https://therarbg.com"
    override val specializedCategory = Category.All
    override val safetyStatus = SearchProviderSafetyStatus.Unsafe(
        reason = R.string.therarbg_unsafe_reason
    )
    override val enabledByDefault = false

    private val resultsPageParser = TheRarBgResultsPageParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/get-posts")
            append("/keywords:$query")

            if (context.category != Category.All) {
                val category = categoryName(raw = context.category)
                append(":category:$category")
            }
        }
        val resultPageHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = resultPageHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return TheRarBgDetailsPageParser.parse(responseHtml)
    }

    /** Returns the compatible category string. */
    private fun categoryName(raw: Category): String = when (raw) {
        Category.All -> ""
        Category.Anime -> "Anime"
        Category.Apps -> "Apps"
        Category.Books -> "Books"
        Category.Games -> "Games"
        Category.Movies -> "Movies"
        Category.Music -> "Music"
        Category.Porn -> "XXX"
        Category.Series -> "Tv"
        Category.Other -> "Other"
    }
}

private class TheRarBgResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")
            ?: return null
        println(detailsPageUrl)
        val detailsPageHtml = HttpClient.get(detailsPageUrl)
        val torrentDetails = TheRarBgDetailsPageParser.parse(detailsPageHtml) ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(torrentDetails.magnetUri)

        val name = listItem.selectFirst(NAME)?.ownText() ?: return null
        val size = listItem.selectFirst(SIZE)?.attr("data-order")?.let(FileSizeUtils::formatBytes)
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.attr("data-order")
            ?.toLongOrNull()
            ?.let(TorrentDateParser::epochSecondToInstant)
        val category = listItem.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size ?: "0 KB",
            seeders = seeders ?: 0U,
            peers = peers ?: 0U,
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            magnetUri = torrentDetails.magnetUri,
            fileDownloadLink = torrentDetails.fileDownloadLink,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table > tbody > tr.list-entry"
        private const val NAME = "td.cellName > div > a"
        private const val SIZE = "td.sizeCell"
        private const val SEEDERS = "td:nth-child(7)"
        private const val PEERS = "td:nth-child(8)"
        private const val UPLOAD_DATE = "td:nth-child(4)"
        private const val CATEGORY = "td:nth-child(3) > a"
        private const val DETAILS_PAGE_URL = NAME
    }
}

private object TheRarBgDetailsPageParser {
    private const val NAME = "div.postContL > h4:has(+ div.table-responsive)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

        val detailRows = html.select("table.detailTable > tbody > tr")
            .mapNotNull { tr ->
                val label = tr.selectFirst("th")?.ownText() ?: return@mapNotNull null
                val value = tr.selectFirst("td") ?: return@mapNotNull null
                label to value
            }
            .toMap()
        val size = detailRows["Size:"]?.ownText()
        val seedersPeers = detailRows["Peers:"]?.ownText()?.trim()?.split(',')
        val seeders = seedersPeers?.firstOrNull()?.removePrefix("Seeders: ")?.toUIntOrNull()
        val peers = seedersPeers?.lastOrNull()
            ?.trim()
            ?.removePrefix("Leechers: ")
            ?.toUIntOrNull()
        val uploadDate = detailRows["Added:"]?.ownText()
            ?.let(::normalizeUploadDate)
            ?.let { TorrentDateParser.parse(date = it, format = "MMM d, yyyy, h:mm a") }
        val category = detailRows["Category:"]?.text()?.let(::categoryFromRawString)
        val uploader = detailRows["Uploader:"]?.text()
        val description = detailRows["Description:"]?.wholeText()

        TorrentDetails(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            uploader = uploader,
            magnetUri = magnetUri,
            description = description,
        )
    }

    private fun normalizeUploadDate(date: String): String =
        date
            .replace("a.m.", "AM")
            .replace("p.m.", "PM")
            .split(' ', limit = 5)
            .let { (month, day, year, time, amPm) ->
                val fixedTime = if (time.contains(':')) time else "$time:00"
                "$month $day $year $fixedTime $amPm"
            }
}

/** Returns the [Category] that matches the string extracted from page. */
private fun categoryFromRawString(raw: String): Category = when (raw) {
    "Anime" -> Category.Anime
    "Apps" -> Category.Apps
    "Books" -> Category.Books
    "Games" -> Category.Games
    "Movies" -> Category.Movies
    "Music" -> Category.Music
    "XXX" -> Category.Porn
    "Tv" -> Category.Series
    "Other" -> Category.Other
    else -> Category.Other
}