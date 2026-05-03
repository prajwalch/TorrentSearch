package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Dmhy : SearchProvider {
    override val id = "dmhy"
    override val name = "Dmhy"
    override val url = "https://share.dmhy.org"
    override val specializedCategory = Category.All
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = DmhyResultsPageParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/topics")
            append("/list")
            append("?keyword=$query")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }
}

private class DmhyResultsPageParser(
    private val providerName: String,
) {
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
            ?.split(' ')
            ?.firstOrNull()
            ?.replace("/", "-")
            ?.let(DateUtils::formatYearMonthDay)
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
            uploadDate = uploadDate ?: "0 min ago",
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
        private const val UPLOAD_DATE = "td:nth-child(1)"
        private const val CATEGORY = "td:nth-child(2) > a"
        private const val MAGNET_URI = "td:nth-child(4) > a:nth-child(1)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
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