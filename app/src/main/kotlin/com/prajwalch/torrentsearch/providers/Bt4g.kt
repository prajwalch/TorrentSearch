package com.prajwalch.torrentsearch.providers

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
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Bt4g : SearchProvider, LatestTorrentsProvider, TopTorrentsProvider, TorrentDetailsProvider {
    override val id = "bt4g"
    override val name = "BT4G"
    override val url = "https://bt4gprx.com"
    override val cloudflareSolverUrl = "$url/search?q=ubuntu"
    override val supportedCategories = setOf(
        Category.Apps,
        Category.Books,
        Category.Movies,
        Category.Music,
        Category.Other,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val isCloudflareProtected = true
    override val enabledByDefault = false

    private val categoryMap = mapOf(
        Category.All to "all",
        Category.Apps to "app",
        Category.Books to "doc",
        Category.Movies to "movie",
        Category.Music to "audio",
        Category.Other to "other",
    )
    private val resultsPageParser = Bt4gResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val categoryName = categoryMap[context.category] ?: categoryMap[Category.All]!!
        val requestUrl = "$url/search?q=$query&category=$categoryName&orderby=seeders&p=1"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return Bt4gDetailsPageParser.parse(responseHtml)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/new"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/week"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }
}

private class Bt4gResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href") ?: return null
        val infoHash = Bt4gDetailsPageParser.getInfoHash(detailsPageUrl) ?: return null

        val torrentName = listItem.selectFirst(TORRENT_NAME)?.text() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.removePrefix("Creation Time: ")
            ?.trim()
            ?.let { rawDate ->
                runCatching {
                    TorrentDateParser.parse(date = rawDate, format = "yyyy-MM-dd")
                }.recoverCatching {
                    TorrentDateParser.tryParseRelative(rawDate)
                }.getOrNull()
            }
        val category = listItem.selectFirst(CATEGORY)?.ownText()?.let(::getCategoryFromRaw)

        return Torrent(
            infoHash = infoHash,
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

    private companion object {
        private const val LIST_ITEM = "div.notion-list-item"
        private const val TORRENT_NAME = "div.notion-list-item-title > a"
        private const val SIZE = "div.notion-list-item-meta > span:nth-child(5) > b"
        private const val SEEDERS = "div.notion-list-item-meta span#seeders"
        private const val PEERS = "div.notion-list-item-meta span#leechers"
        private const val UPLOAD_DATE = "div.notion-list-item-meta > span:nth-child(2)"
        private const val CATEGORY = "div.notion-list-item-meta span.notion-tag"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object Bt4gDetailsPageParser {
    private const val TORRENT_NAME = "h1.notion-detail-title"
    private const val SIZE = "span.notion-property-label:containsOwn(File Size)"
    private const val SEEDERS = "span#seeders"
    private const val PEERS = "span#leechers"
    private const val UPLOAD_DATE = "span.notion-property-label:containsOwn(Creation Time)"
    private const val CATEGORY = "span.notion-property-label:containsOwn(File Type)"
    private const val LAST_CHECKED = "span.notion-property-label:containsOwn(Updated)"
    private const val MAGNET_LINK_BTN = """a[href^="//downloadtorrentfile.com/hash/"]"""

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val infoHash = extractInfoHash(html) ?: return@withContext null
        val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
        val size = html.selectFirst(SIZE)
            ?.nextElementSibling()
            ?.ownText()
            ?.let(FileSizeUtils::normalizeSize)
        val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = html.selectFirst(UPLOAD_DATE)
            ?.nextElementSibling()
            ?.ownText()
            ?.let { TorrentDateParser.parse(date = it, format = "yyyy-MM-dd") }
        val category = html.selectFirst(CATEGORY)
            ?.nextElementSibling()
            ?.ownText()
            ?.let(::getCategoryFromRaw)
        val lastChecked = html.selectFirst(LAST_CHECKED)
            ?.nextElementSibling()
            ?.ownText()
            ?.let {
                runCatching { TorrentDateParser.tryParseRelative(it) }.getOrNull()
            }

        TorrentDetails(
            infoHash = infoHash,
            magnetUri = TorrentUtils.createMagnetUri(infoHash),
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            lastChecked = lastChecked,
        )
    }

    suspend fun getInfoHash(detailsPageUrl: String): String? {
        val detailsPageHtml = withContext(Dispatchers.Default) {
            HttpClient.get(detailsPageUrl)
        }

        return withContext(Dispatchers.Default) {
            extractInfoHash(Jsoup.parse(detailsPageHtml))
        }
    }

    private fun extractInfoHash(html: Document): String? {
        return html.selectFirst(MAGNET_LINK_BTN)
            ?.attr("href")
            ?.removePrefix("//downloadtorrentfile.com/hash/")
            ?.takeWhile { it != '?' }
    }
}

private fun getCategoryFromRaw(raw: String): Category = when (raw) {
    "Video", "Movie" -> Category.Movies
    "Audio" -> Category.Music
    "Doc" -> Category.Books
    "App", "Application" -> Category.Apps
    "Other" -> Category.Other
    else -> Category.Other
}