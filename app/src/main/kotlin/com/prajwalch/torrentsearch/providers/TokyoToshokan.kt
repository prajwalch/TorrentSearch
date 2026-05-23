package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TokyoToshokan : SearchProvider, TorrentDetailsProvider, LatestTorrentsProvider,
    TopTorrentsProvider {
    override val id = "tokyotoshokan"
    override val name = "TokyoToshokan"
    override val url = "https://tokyotosho.info"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Books,
        Category.Music,
        Category.Porn,
        Category.Other,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = true

    private val categoryMap = mapOf(
        Category.All to 0,
        Category.Anime to 1,
        Category.Books to 3,
        Category.Music to 2,
        Category.Porn to 15,
        Category.Other to 5,
    )
    private val resultsPageParser = TokyoToshokanResultsPageParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/search.php")
            append("?terms=$query")
            // Type = Anime (1)
            val categoryId = categoryMap[context.category] ?: categoryMap[Category.All]!!
            append("&type=$categoryId")
            // Match query with torrent name.
            append("&searchName=true")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return TokyoToshokanDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/index.php")

            if (category != Category.All) {
                categoryMap[category]?.let { append("?cat=$it") }
            }
        }
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        return getLastestTorrents(category)
    }
}

private class TokyoToshokanResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select(LIST_ITEM)
                .zipWithNext()
                .mapNotNull { (tr1, tr2) -> parseListItem(tr1, tr2) }
        }

    private fun parseListItem(tr1: Element, tr2: Element): Torrent? {
        val torrentName = tr1.selectFirst(NAME)?.ownText() ?: return null
        val magnetUri = tr1.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val fileDownloadLink = tr1.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = tr1.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")
        val category = tr1.selectFirst(CATEGORY)
            ?.attr("href")
            ?.removePrefix("/?cat=")
            ?.let(::categoryFromId)

        val (rawSize, rawUploadDate) = tr2.selectFirst(SIZE_AND_UPLOAD_DATE)
            ?.ownText()
            ?.split('|')
            ?.drop(1)
            ?.map { it.trim().dropWhile { ch -> !ch.isWhitespace() }.trim() }
            ?: listOf(null, null)
        val size = rawSize?.let(FileSizeUtils::normalizeSize)
        val uploadDate = rawUploadDate?.let {
            TorrentDateParser.parse(date = it, format = "yyyy-MM-dd HH:mm z")
        }
        val seeders = tr2.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = tr2.selectFirst(PEERS)?.ownText()?.toUIntOrNull()

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders ?: 0u,
            peers = peers ?: 0u,
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = detailsPageUrl ?: "",
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table.listing > tbody > tr:nth-child(n+2)"
        private const val NAME = "td.desc-top > a:nth-child(2)"
        private const val SIZE_AND_UPLOAD_DATE = "td.desc-bot"
        private const val SEEDERS = "td.stats > span:nth-child(1)"
        private const val PEERS = "td.stats > span:nth-child(2)"
        private const val CATEGORY = "td:nth-child(1) > a"
        private const val MAGNET_URI = "td.desc-top > a:nth-child(1)"
        private const val FILE_DOWNLOAD_LINK = NAME
        private const val DETAILS_PAGE_URL = "td.web > a:last-child"
    }
}

private object TokyoToshokanDetailsPageParser {
    private const val INFO_HASH = "#main > div.details > ul > li:nth-child(18)"
    private const val NAME = "#main > div.details > ul > li:nth-child(6) > a"
    private const val SIZE = "#main > div.details > ul > li:nth-child(10)"
    private const val SEEDERS = "#main > div.details > ul > li:nth-child(20)"
    private const val PEERS = "#main > div.details > ul > li:nth-child(22)"
    private const val UPLOAD_DATE = "#main > div.details > ul > li:nth-child(8)"
    private const val CATEGORY = "#main > div.details > ul > li:nth-child(2) > a"
    private const val UPLOADER = "#main > div.details > ul > li:nth-child(28)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val nameAnchor = html.selectFirst(NAME) ?: return@withContext null
            val name = nameAnchor.text()
            val fileDownloadLink = if (nameAnchor.attr("type") == "application/x-bittorrent") {
                nameAnchor.attr("abs:href")
            } else {
                null
            }
            val infoHash = html.selectFirst(INFO_HASH)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)
                ?.attr("href")
                ?: TorrentUtils.createMagnetUri(infoHash)
            val size = html.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.ownText()
                ?.let { TorrentDateParser.parse(date = it, format = "yyyy-MM-dd hh:mm z") }
            val category = html.selectFirst(CATEGORY)
                ?.attr("href")
                ?.removePrefix("index.php?cat=")
                ?.let(::categoryFromId)
            val uploader = html.selectFirst(UPLOADER)?.ownText()

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
                fileDownloadLink = fileDownloadLink,
            )
        }
}

private fun categoryFromId(id: String): Category = when (id) {
    "1", "7", "8", "10", "11" -> Category.Anime
    "3" -> Category.Books
    "2", "9" -> Category.Music
    "4", "12", "13", "14", "15" -> Category.Porn
    "5" -> Category.Other
    else -> Category.Other
}