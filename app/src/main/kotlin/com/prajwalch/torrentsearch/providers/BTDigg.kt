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

class BTDigg : SearchProvider, TorrentDetailsProvider {
    override val id = "btdigg"
    override val name = "BTDigg"
    override val url = "https://btdig.com"
    override val supportedCategories = setOf(Category.Other)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = BTDiggResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "https://btdig.com/search?q=$query"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return BTDiggDetailsPageParser.parse(responseHtml)
    }
}

private class BTDiggResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.text() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.removePrefix("found ")
            ?.let(TorrentDateParser::tryParseRelative)
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            uploadDate = uploadDate,
            descriptionPageUrl = detailsPageUrl,
            providerName = providerName,
        )
    }

    private companion object {
        private const val LIST_ITEM = "div.one_result > div"
        private const val TORRENT_NAME = "div.torrent_name > a"
        private const val SIZE = "span.torrent_size"
        private const val UPLOAD_DATE = "span.torrent_age"
        private const val MAGNET_URI = "div.torrent_magnet > div.fa-magnet > a"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object BTDiggDetailsPageParser {
    private const val TORRENT_NAME = "td:containsOwn(Name:)"
    private const val SIZE = "td:containsOwn(Size:)"
    private const val UPLOAD_DATE = "td:containsOwn(Age:)"
    private const val MAGNET_URI = """a[href^="magnet:?xt="]"""

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)
        val torrentName = html.selectFirst(TORRENT_NAME)
            ?.nextElementSibling()
            ?.ownText()
            ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val size = html.selectFirst(SIZE)?.nextElementSibling()?.ownText()
        val uploadDate = html.selectFirst(UPLOAD_DATE)
            ?.nextElementSibling()
            ?.ownText()
            ?.let { "$it ago" }
            ?.let(TorrentDateParser::tryParseRelative)

        TorrentDetails(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            uploadDate = uploadDate,
            magnetUri = magnetUri,
        )
    }
}