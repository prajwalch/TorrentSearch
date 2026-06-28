package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ZeroMagnet : SearchProvider, TorrentDetailsProvider {
    override val id = "0magnet"
    override val name = "0Magnet"
    override val url = "https://9mag.net"
    override val supportedCategories = setOf(Category.Porn)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = ZeroMagnetResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        // https://9mag.net/search?q=tight
        val requestUrl = "$url/search?q=$query"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return ZeroMagnetDetailsPageParser.parse(responseHtml)
    }
}

private class ZeroMagnetResultsPageParser(private val providerName: String) {
    private companion object {
        private const val LIST_ITEM = "table.file-list > tbody > tr"
        private const val DETAILS_PAGE_URL = "td:nth-child(1) > a"
    }

    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    private suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)
            ?.attr("abs:href") ?: return null
        val detailsPageHtml = HttpClient.get(detailsPageUrl)
        val torrentDetails = ZeroMagnetDetailsPageParser.parse(detailsPageHtml) ?: return null

        return Torrent(
            infoHash = torrentDetails.infoHash,
            name = torrentDetails.name,
            size = torrentDetails.size ?: "0 KB",
            seeders = 1U,
            peers = 1U,
            uploadDate = torrentDetails.uploadDate,
            providerName = providerName,
            category = torrentDetails.category,
            descriptionPageUrl = detailsPageUrl,
        )
    }
}

private object ZeroMagnetDetailsPageParser {
    private const val TORRENT_NAME = "h2.magnet-title"
    private const val SIZE = "dl.torrent-info > dd:nth-child(4)"
    private const val UPLOAD_DATE = "dl.torrent-info > dd:nth-child(6)"
    private const val MAGNET_URI = "input#input-magnet"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)
        val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("value") ?: return@withContext null
        val size = html.selectFirst(SIZE)?.ownText()
        val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()?.let {
            TorrentDateParser.parse(date = it, format = "yyyy-MM-dd HH:mm:ss")
        }

        TorrentDetails(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            uploadDate = uploadDate,
            magnetUri = magnetUri,
            category = Category.Porn,
        )
    }
}