package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class AniRena : SearchProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider,
    TorrentDetailsProvider {
    override val id = "anirena"
    override val name = "AniRena"
    override val url = "https://anirena.com"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Books,
        Category.Music,
        Category.Porn,
        Category.Series,
        Category.Other,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val categoryMap = mapOf(
        Category.Anime to "anime",
        Category.Apps to "software",
        Category.Books to "manga",
        Category.Music to "audio",
        Category.Porn to "hentai",
        Category.Series to "live",
        Category.Other to "other"
    )
    private val resultsPageParser = AniRenaResultsPageParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("?q=$query")
            append("&page=1")

            if (context.category != Category.All) {
                categoryMap[context.category]?.let { append("&cat=$it") }
            }
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val responseHtml = HttpClient.get(url)
        return resultsPageParser.parse(html = responseHtml, pageUrl = url)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        return getLastestTorrents(category)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return AniRenaDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class AniRenaResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull { parseListItem(it) }
        }

    private suspend fun parseListItem(listItem: Element): Torrent? {
        val magnetUriSourceLink = listItem.selectFirst(MAGNET_URI)?.attr("abs:href") ?: return null
        val magnetUri = getMagnetUri(magnetUriSourceLink) ?: return null

        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.attr("data-created-ts")
            .takeIf { it.isNotBlank() }
            ?.toLongOrNull()
            ?.let(TorrentDateParser::epochSecondToInstant)
        val category = listItem.selectFirst(CATEGORY)
            ?.attr("title")
            ?.takeIf { it.isNotBlank() }
            ?.takeWhile { it != '/' }
            ?.trim()
            ?.let(::getCategoryFromRawString)
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders ?: 0U,
            peers = peers ?: 0U,
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl ?: "",
        )
    }

    private companion object {
        private const val LIST_ITEM = "table.tl-table > tbody > tr"
        private const val TORRENT_NAME = "td.col-name > div.tl-name-wrap > a.tl-torrent-name"
        private const val SIZE = "td.col-size"
        private const val SEEDERS = "td.col-se > span.tl-se"
        private const val PEERS = "td.col-le > span.tl-le"
        private const val CATEGORY = "td.col-cat"
        private const val MAGNET_URI = "td.col-actions > div.tl-actions > a:nth-child(1)"
        private const val FILE_DOWNLOAD_LINK = "td.col-actions > div.tl-actions > a:nth-child(2)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object AniRenaDetailsPageParser {
    private const val INFO_HASH = "code.td-ov-stat-hash"
    private const val TORRENT_NAME = "h1.td-title"
    private const val SIZE = "div.td-ov-stats > div.td-ov-stat:nth-child(4) > div.td-ov-stat-num"
    private const val SEEDERS = "div.td-ov-stats > div.td-ov-stat--se > div.td-ov-stat-num"
    private const val PEERS = "div.td-ov-stats > div.td-ov-stat--le > div.td-ov-stat-num"
    private const val UPLOAD_DATE = "div.td-ov-meta > div.td-ov-meta-val:nth-child(6) > span"
    private const val CATEGORY = "div.td-ov-meta-val:nth-child(2) a.td-cat-name.td-cat-link"
    private const val UPLOADER = "div.td-ov-meta > div.td-ov-meta-val:nth-child(4)"
    private const val MAGNET_URI = "div.td-actions > a:nth-child(1)"
    private const val FILE_DOWNLOAD_LINK = "div.td-actions > a:nth-child(2)"
    private const val DESCRIPTION = "script#td-description-raw"
    private const val POSTER_URL = "div.td-anime-poster > img"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val infoHash = html.selectFirst(INFO_HASH)
                ?.ownText()
                ?.lowercase()
                ?: return@withContext null
            val torrentName = html.selectFirst(TORRENT_NAME)?.text() ?: return@withContext null
            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.attr("data-utc")
                ?.let { TorrentDateParser.parse(date = it, format = "yyyy-MM-dd HH:mm") }
            val category = html.selectFirst(CATEGORY)
                ?.ownText()
                ?.let(::getCategoryFromRawString)
            val uploader = html.selectFirst(UPLOADER)?.ownText()
            val magnetUri = html.selectFirst(MAGNET_URI)
                ?.attr("abs:href")
                ?.let { getMagnetUri(it) }
                ?: TorrentUtils.createMagnetUri(infoHash)
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val description = html.selectFirst(DESCRIPTION)?.data()?.removeSurrounding("\"")
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("abs:src")

            TorrentDetails(
                infoHash = infoHash,
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                uploader = uploader,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }
}

private suspend fun getMagnetUri(sourceUrl: String): String? {
    return HttpClient.getResponse(sourceUrl).let { it.headers["Location"] }
}

private fun getCategoryFromRawString(raw: String) = when (raw) {
    "Anime" -> Category.Anime
    "Manga" -> Category.Books
    "Audio" -> Category.Music
    "Literature" -> Category.Books
    "Live Action" -> Category.Series
    "Software" -> Category.Apps
    "Hentai" -> Category.Porn
    "Other" -> Category.Other
    else -> Category.Other
}