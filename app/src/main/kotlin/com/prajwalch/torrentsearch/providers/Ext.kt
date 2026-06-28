package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.ExtUtils.getCategoryFromRaw
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

import java.security.MessageDigest

class Ext : SearchProvider, TorrentDetailsProvider {
    override val id = "extdotto"
    override val name = "Ext"
    override val url = "https://ext.to"
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Other,
        Category.Porn,
        Category.Series,
    )
    override val enabledByDefault = false
    override val isCloudflareProtected = true

    private val categoryMap = mapOf(
        Category.Anime to 7,
        Category.Apps to 5,
        Category.Books to 6,
        Category.Games to 4,
        Category.Movies to 1,
        Category.Music to 3,
        Category.Other to 8,
        Category.Porn to 10,
        Category.Series to 2,
    )
    private val resultsPageParser = ExtResultsPageParser(name)

    override suspend fun search(
        query: String,
        context: SearchContext,
    ): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/browse/?")

            categoryMap[context.category]?.let {
                append("cat=$it&")
            }
            append("q=$query")
        }
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return ExtDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class ExtResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)
            val sessionId = html.selectFirst(SESSION_ID)?.attr("content")
            val pageToken = extractSearchPageToken(html)

            html.select(LIST_ITEM)
                .mapNotNull {
                    async {
                        parseListItem(
                            listItem = it,
                            sessionId = sessionId,
                            pageToken = pageToken
                        )
                    }
                }
                .awaitAll()
                .filterNotNull()
        }

    private fun extractSearchPageToken(html: Document): String? {
        return html.select("script")
            .map { it.data().trim() }
            .filter { it.isNotBlank() }
            .find { it.startsWith("window.searchPageToken") }
            ?.removeSuffix("';")
            ?.takeLastWhile { it != '\'' }
    }

    private suspend fun parseListItem(
        listItem: Element,
        sessionId: String?,
        pageToken: String?,
    ): Torrent? {
        val torrentId = listItem.selectFirst(TORRENT_ID)?.attr("data-id") ?: return null
        val magnetUri = getMagnetUri(torrentId, sessionId, pageToken) ?: return null
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.text() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.attr("title")
            ?.let { TorrentDateParser.parse(date = it, format = "dd MMMM yyyy") }
        val category = listItem.selectFirst(CATEGORY)
            ?.attr("href")
            ?.removeSurrounding("/", "/")
            ?.let(::getCategoryFromRaw)
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private suspend fun getMagnetUri(
        torrentId: String,
        sessionId: String?,
        pageToken: String?,
    ): String? {
        if (pageToken == null || sessionId == null) {
            return null
        }

        val timestamp = System.currentTimeMillis() / 1000
        val hmacToken = ExtUtils.computeHMAC(torrentId, timestamp, pageToken)
        return HttpClient.submitForm(
            url = "https://ext.to/ajax/getSearchMagnet.php",
            formData = mapOf(
                "torrent_id" to torrentId,
                "hash" to "",
                "name" to "",
                "timestamp" to timestamp.toString(),
                "hmac" to hmacToken,
                "sessid" to sessionId,
            ),
        )
            ?.let(Json::parseToJsonElement)
            ?.asObject()
            ?.getString("url")
    }

    private companion object {
        private const val SESSION_ID = """meta[name="csrf-token"]"""
        private const val LIST_ITEM = "table.search-table > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(1) > div:nth-child(1) > a.torrent-title-link"
        private const val SIZE = "td:nth-child(2) > div > span:nth-child(2)"
        private const val SEEDERS = "td:nth-child(5) > div > span:nth-child(2)"
        private const val PEERS = "td:nth-child(6) > div > span:nth-child(3)"
        private const val UPLOAD_DATE = "td:nth-child(4) > div > span:nth-child(2)"
        private const val CATEGORY =
            "td:nth-child(1) > div:nth-child(1) > div.related-posted > a:nth-child(2)"
        private const val TORRENT_ID = "td:nth-child(1) > div:nth-child(2) > a.search-magnet-btn"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object ExtDetailsPageParser {
    private const val SESSION_ID = """meta[name="csrf-token"]"""
    private const val TORRENT_ID = "a.detail-magnet-link.download-btn-magnet"
    private const val DETAILS_PAGE_CARD = "div.detal-page-left-block > div.card > div.card-body"
    private const val TORRENT_NAME = "$DETAILS_PAGE_CARD .card-title"
    private const val SIZE = "$DETAILS_PAGE_CARD .content-size"
    private const val SEEDERS = "$DETAILS_PAGE_CARD #seed-counter"
    private const val PEERS = "$DETAILS_PAGE_CARD #leech-counter"
    private const val UPLOAD_DATE =
        "$DETAILS_PAGE_CARD div.detail-torrent-poster-info > span:nth-child(1)"
    private const val CATEGORY =
        "$DETAILS_PAGE_CARD div.detail-torrent-poster-info > a:nth-child(3)"
    private const val UPLOADER =
        "$DETAILS_PAGE_CARD div.detail-torrent-poster-info span.external-user"
    private const val LAST_CHECKED = "$DETAILS_PAGE_CARD span.detail-update-date > strong"
    private const val TORRENT_SLUG = "$DETAILS_PAGE_CARD a.js-dscr"
    private const val MOVIE_PLOT = "$DETAILS_PAGE_CARD div.movie-info > div.plot-block"
    private const val SERIES_PLOT = "$DETAILS_PAGE_CARD div.plot-info > div.block-plot-tv"
    private const val POSTER_URL = "$DETAILS_PAGE_CARD div.poster-block > a > img"
    private const val POSTER_URL_1 = "$DETAILS_PAGE_CARD img.detail-torrent-image"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val torrentId = html.selectFirst(TORRENT_ID)?.attr("data-id") ?: return@withContext null
            val sessionId = html.selectFirst(SESSION_ID)?.attr("content") ?: return@withContext null
            val pageToken = extractPageToken(html) ?: return@withContext null
            val magnetUri = getMagnetUri(
                torrentId = torrentId,
                sessionId = sessionId,
                pageToken = pageToken,
            ) ?: return@withContext null

            val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val size = html.selectFirst(SIZE)?.ownText()?.removePrefix("Size: ")
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.attr("title")
                ?.let { TorrentDateParser.parse(date = it, format = "dd MMMM yyyy") }
            val category = html.selectFirst(CATEGORY)
                ?.attr("href")
                ?.removeSurrounding("/")
                ?.let(::getCategoryFromRaw)
            val uploader = html.selectFirst(UPLOADER)?.ownText()
            val lastChecked = html.selectFirst(LAST_CHECKED)
                ?.ownText()
                ?.let(TorrentDateParser::tryParseRelative)

            val description = when (category) {
                Category.Movies -> html.selectFirst(MOVIE_PLOT)?.ownText()
                Category.Series -> html.selectFirst(SERIES_PLOT)?.ownText()
                else -> {
                    html.selectFirst(TORRENT_SLUG)
                        ?.attr("data-code")
                        ?.let { getDescription(torrentId = torrentId, torrentSlug = it) }
                }
            }
            val posterUrl = (html.selectFirst(POSTER_URL) ?: html.selectFirst(POSTER_URL_1))
                ?.attr("abs:src")
                ?.takeIf { !it.endsWith("no-torrent-image.png") }

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                magnetUri = magnetUri,
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                uploader = uploader,
                lastChecked = lastChecked,
                description = description,
                posterUrl = posterUrl,
            )
        }

    private fun extractPageToken(html: Document): String? {
        return html.select("script")
            .mapNotNull { it.data().trim().lines().firstOrNull() }
            .find { it.startsWith("window.pageToken") }
            ?.removeSuffix("';")
            ?.takeLastWhile { it != '\'' }
    }

    private suspend fun getMagnetUri(
        torrentId: String,
        sessionId: String,
        pageToken: String,
    ): String? {
        val timestamp = System.currentTimeMillis() / 1000
        val hmacToken = ExtUtils.computeHMAC(torrentId, timestamp, pageToken)

        return HttpClient.submitForm(
            url = "https://ext.to/ajax/getTorrentMagnet.php",
            formData = mapOf(
                "torrent_id" to torrentId,
                "download_type" to "magnet",
                "timestamp" to timestamp.toString(),
                "hmac" to hmacToken,
                "sessid" to sessionId,
            ),
        )
            ?.let(Json::parseToJsonElement)
            ?.asObject()
            ?.getString("url")
    }

    private suspend fun getDescription(torrentId: String, torrentSlug: String): String? {
        return HttpClient.getJson(
            url = "https://ext.to/ajax/torrentDescription.php?id=$torrentId&code=$torrentSlug",
            headers = mapOf("x-requested-with" to "XMLHttpRequest"),
        )
            ?.also { println(it) }
            ?.asObject()
            ?.getString("data")
    }
}

private object ExtUtils {
    fun computeHMAC(torrentId: String, timestamp: Long, pageToken: String): String {
        val data = "$torrentId|$timestamp|$pageToken".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)

        return hashBytes.toHexString()
    }

    fun getCategoryFromRaw(raw: String) = when (raw) {
        "anime" -> Category.Anime
        "applications" -> Category.Apps
        "books" -> Category.Books
        "games" -> Category.Games
        "movies" -> Category.Movies
        "music" -> Category.Music
        "other" -> Category.Other
        "tv" -> Category.Series
        "xxx" -> Category.Porn
        else -> Category.Other
    }
}