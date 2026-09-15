package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUriState
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.MagnetUriProvider
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.provider.TorrentDetailsProvider
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ZeroMagnet(private val networkClient: NetworkClient) :
    SearchProvider,
    MagnetUriProvider,
    TorrentDetailsProvider {
    override val id = "0magnet"
    override val name = "0Magnet"
    override val url = "https://9mag.net"
    override val supportedCategories = setOf(Category.Porn)
    override val safety = SearchProviderSafety.Safe
    override val enabledByDefault = false

    private val resultsPageParser = ZeroMagnetResultsPageParser(id, name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        // https://9mag.net/search?q=tight
        val requestUrl = "$url/search?q=$query"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getMagnetUri(sourceUrl: String): String {
        val detailsPageHtml = networkClient.getText(sourceUrl)
        return ZeroMagnetDetailsPageParser.extractMagnetUri(detailsPageHtml)
            ?: error("Failed to retrieve magnet URI from '$sourceUrl'")
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = networkClient.getText(detailsPageUrl)
        return ZeroMagnetDetailsPageParser.parse(responseHtml)
    }
}

private class ZeroMagnetResultsPageParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
) {
    private companion object {
        private const val LIST_ITEM = "table.file-list > tbody > tr"
        private const val TORRENT_NAME = "td.result-title > a"
        private const val SIZE = "td.result-meta > div:nth-child(1)"
        private const val UPLOAD_DATE = "td.result-meta > div.result-date"
        private const val DETAILS_PAGE_URL = "td:nth-child(1) > a"
    }

    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val name = listItem.selectFirst(TORRENT_NAME)?.text() ?: return null
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)
            ?.attr("abs:href") ?: return null
        val torrentId = TorrentUtils.createTorrentId(providerId, detailsPageUrl)
        val size = listItem.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.ownText()?.let {
            TorrentDateParser.parse(date = it, format = "yyyy-MM-dd")
        }

        return Torrent(
            id = torrentId,
            name = name,
            size = size,
            uploadDate = uploadDate,
            providerName = providerName,
            category = Category.Porn,
            magnetUriState = MagnetUriState.FetchRequired(detailsPageUrl),
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

    suspend fun extractMagnetUri(detailsPageHtml: String): String? =
        withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml).selectFirst(MAGNET_URI)?.attr("value")
        }
}