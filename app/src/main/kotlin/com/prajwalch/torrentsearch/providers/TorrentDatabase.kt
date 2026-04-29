package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TorrentDatabase : SearchProvider {
    override val info = SearchProviderInfo(
        id = "torrentdatabase",
        name = "TorrentDatabase",
        url = "https://developify.ca",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    private val categoryMap = mapOf(
        Category.All to "",
        Category.Anime to "",
        Category.Other to "",
        Category.Apps to "software",
        Category.Books to "e-books",
        Category.Games to "games",
        Category.Movies to "movies",
        Category.Music to "music",
        Category.Porn to "porn",
        Category.Series to "tv",
    )

    private val resultsPageParser = TdResultsPageParser(providerName = info.name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/newest")
            append("?q=$query")
            append("&category=${categoryMap[context.category]}")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): GetTorrentDetailsResponse {
        val responseHtml = HttpClient.get(detailsPageUrl)

        return TdDetailsPageParser.parse(responseHtml)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.DetailsNotFound
    }
}

private class TdResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select(RESULT_LIST_ITEM)
                .mapNotNull { parseListItem(it) }
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentNameElm = listItem.selectFirst(TORRENT_NAME) ?: return null
        val torrentName = torrentNameElm.ownText()
        val infoHash = torrentNameElm.attr("href")
            .removePrefix("/track/magnet/")
            .takeWhile { it != '?' }
        val magnetUri = TorrentUtils.createMagnetUri(infoHash).let {
            "$it&tr=$TORRENT_DATABASE_TRACKER_URL"
        }
        val descriptionPageUrl = listItem.selectFirst(DESCRIPTION_PAGE_URL)?.attr("abs:href")
        val category = listItem.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)
        val size = listItem.selectFirst(SIZE)?.ownText()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.takeWhile { !it.isWhitespace() }
            ?.let(DateUtils::formatYearMonthDay)
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()
        val peers = listItem.selectFirst(PEERS)?.ownText()

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders?.toUIntOrNull() ?: 0U,
            peers = peers?.toUIntOrNull() ?: 0U,
            uploadDate = uploadDate ?: "0 min ago",
            category = category,
            providerName = providerName,
            descriptionPageUrl = descriptionPageUrl ?: "",
            magnetUri = magnetUri,
        )
    }

    private companion object {
        private const val TORRENT_DATABASE_TRACKER_URL = "https%3A%2F%2Fdevelopify.ca%2Fannounce"

        // Selectors
        private const val RESULT_LIST_ITEM = "table.torrent-table > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(1) > a:nth-child(2)"
        private const val SIZE = "td.size-cell"
        private const val SEEDERS = "td:nth-child(5) > div > span:nth-child(1)"
        private const val PEERS = "td:nth-child(5) > div > span:nth-child(2)"
        private const val UPLOAD_DATE = "td.date-cell"
        private const val CATEGORY = "td:nth-child(2) > span.category-bubble"
        private const val DESCRIPTION_PAGE_URL = "td:nth-child(1) > a:nth-child(1)"
    }
}

private object TdDetailsPageParser {
    private const val TORRENT_NAME =
        "div.torrent-detail-card > div.card-header.torrent-cat-header > h4"
    private const val SIZE =
        "div.torrent-detail-card > div.card-body ul.torrent-info-list > li:nth-child(2) > strong.db-value"
    private const val SEEDERS =
        "div.torrent-detail-card > div.card-body ul.torrent-stats-list > li:nth-child(1) > strong.text-success"
    private const val PEERS =
        "div.torrent-detail-card > div.card-body ul.torrent-stats-list > li:nth-child(2) > strong.text-danger"
    private const val UPLOAD_DATE =
        "div.torrent-detail-card > div.card-body ul.torrent-info-list > li:nth-child(3) > strong.db-value"
    private const val CATEGORY = ".cat-badge"
    private const val UPLOADER =
        "div.torrent-detail-card > div.card-body ul.torrent-info-list > li:nth-child(4) > a"
    private const val LAST_CHECKED =
        "div.torrent-detail-card > div.card-body ul.torrent-info-list li:nth-child(5) > strong.db-value"
    private const val DESCRIPTION = "div.torrent-info-card > div.torrent-info-content"
    private const val MAGNET_URI = "#downloadMagnetBtn"

    suspend fun parse(html: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html)

            val name = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

            val size = html.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()
            val category = html.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)
            val uploader = html.selectFirst(UPLOADER)?.ownText()
            val lastChecked = html.selectFirst(LAST_CHECKED)?.ownText()
            val description = html.selectFirst(DESCRIPTION)
                ?.let(TorrentUtils.HtmlToMarkdownConverter::convert)

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                uploader = uploader,
                lastChecked = lastChecked,
                magnetUri = magnetUri,
                description = description,
            )
        }
}

private fun categoryFromRawString(raw: String) = when (raw) {
    "Software" -> Category.Apps
    "E-Books", "AudioBooks" -> Category.Books
    "Games" -> Category.Games
    "Movies" -> Category.Movies
    "Music" -> Category.Music
    "Porn" -> Category.Porn
    "TV" -> Category.Series
    else -> Category.Other
}