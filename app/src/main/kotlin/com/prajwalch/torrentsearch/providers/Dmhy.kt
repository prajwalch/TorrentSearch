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

class Dmhy : SearchProvider, TorrentDetailsProvider, LatestTorrentsProvider, TopTorrentsProvider {
    override val id = "dmhy"
    override val name = "Dmhy"
    override val url = "https://share.dmhy.org"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Books,
        Category.Games,
        Category.Music,
        Category.Series,
        Category.Other,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = DmhyResultsPageParser(providerName = name)
    private val categoryMap = mapOf(
        Category.All to 0,
        Category.Anime to 2,
        Category.Books to 3,
        Category.Games to 9,
        Category.Music to 4,
        Category.Series to 6,
        Category.Other to 1,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/topics")
            append("/list")

            val categoryId = categoryMap[context.category] ?: categoryMap[Category.All]!!
            append("?keyword=$query")
            append("&sort_id=$categoryId")
            append("&team_id=0")
            append("&order=date-desc")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return DmhyDetailsPageParser.parse(responseHtml)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val categoryId = categoryMap[category] ?: categoryMap[Category.All]!!
        val requestUrl = "$url/topics/list/sort_id/$categoryId"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        return getLastestTorrents(category)
    }
}

private class DmhyResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
        val seeders = listItem.selectFirst(SEEDERS)?.text()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.text()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.ownText()
            ?.let { TorrentDateParser.parse(date = it, format = "yyyy/MM/dd HH:mm") }
        val category = listItem.selectFirst(CATEGORY)?.className()
            ?.removePrefix("sort-")
            ?.let(::getCategoryFromId)
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
            descriptionPageUrl = detailsPageUrl ?: "",
        )
    }

    private companion object {
        private const val LIST_ITEM = "table#topic_list > tbody > tr"
        private const val TORRENT_NAME = "td.title > a"
        private const val SIZE = "td:nth-child(5)"
        private const val SEEDERS = "td:nth-child(6)"
        private const val PEERS = "td:nth-child(7)"
        private const val UPLOAD_DATE = "td:nth-child(1) > span"
        private const val CATEGORY = "td:nth-child(2) > a"
        private const val MAGNET_URI = "td:nth-child(4) > a:nth-child(1)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object DmhyDetailsPageParser {
    private const val TORRENT_NAME = "div.topic-title > h3"
    private const val SIZE = "div.topic-title > div.info > ul > li:nth-child(6) > span"
    private const val UPLOAD_DATE = "div.topic-title > div.info > ul > li:nth-child(2) > span"
    private const val CATEGORY = "div.topic-title > div.info > ul > li:nth-child(1) > span > a"
    private const val DESCRIPTION = "div.topic-nfo"
    private const val MAGNET_URI = "div#resource-tabs > div#tabs-1 > p:nth-child(2) > a#a_magnet"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val torrentName = html.selectFirst(TORRENT_NAME)?.text() ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val size = html.selectFirst(SIZE)?.text()?.let(FileSizeUtils::normalizeSize)
        val uploadDate = html.selectFirst(UPLOAD_DATE)
            ?.text()
            ?.takeWhile { !it.isWhitespace() }
            ?.let { TorrentDateParser.parse(date = it, format = "yyyy/MM/dd") }
        val category = html.selectFirst(CATEGORY)
            ?.attr("href")
            ?.removePrefix("/topics/list/sort_id/")
            ?.let(::getCategoryFromId)
        val description = html.selectFirst(DESCRIPTION)
            ?.apply { select("> *:lt(2)").remove() }
            ?.html()
            ?.let(TorrentUtils.HtmlToMarkdownConverter::convert)

        TorrentDetails(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            uploadDate = uploadDate,
            category = category,
            magnetUri = magnetUri,
            description = description,
        )
    }
}

private fun getCategoryFromId(id: String) = when (id) {
    "2", "7", "31" -> Category.Anime
    "3" -> Category.Books
    "41", "42" -> Category.Series
    "4", "43", "44", "15" -> Category.Music
    "6" -> Category.Series
    "9", "17", "18", "19", "20", "21" -> Category.Games
    else -> Category.Other
}