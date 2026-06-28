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

class TorrentKitty : SearchProvider, TorrentDetailsProvider {
    override val id = "torrentkitty"
    override val name = "TorrentKitty"
    override val url = "https://torrentkitty.tv"
    override val supportedCategories = setOf(Category.Other)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = TorrentKittyResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/search/$query"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return TorrentKittyDetailsPageParser.parse(responseHtml)
    }
}

private class TorrentKittyResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .drop(1)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()?.uppercase()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.ownText()?.let {
            TorrentDateParser.parse(date = it, format = UPLOAD_DATE_FORMAT)
        }
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            uploadDate = uploadDate,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl,
            providerName = providerName,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table#archiveResult > tbody > tr"
        private const val TORRENT_NAME = "td.name"
        private const val SIZE = "td.size"
        private const val UPLOAD_DATE = "td.date"
        private const val MAGNET_URI = "td.action > a:nth-child(2)"
        private const val FILE_DOWNLOAD_LINK = "td.action > a:nth-child(3)"
        private const val DETAILS_PAGE_URL = "td.action > a:nth-child(1)"
        private const val UPLOAD_DATE_FORMAT = "yyyy-MM-dd"
    }
}

private object TorrentKittyDetailsPageParser {
    private const val TORRENT_NAME = "h2"
    private const val SIZE = "table.detailSummary > tbody > tr:nth-child(4) > td"
    private const val UPLOAD_DATE = "table.detailSummary > tbody > tr:nth-child(5) > td"
    private const val MAGNET_URI = "p.action > a:nth-child(2)"
    private const val FILE_DOWNLOAD_LINK = "p.action > a:nth-child(1)"
    private const val UPLOAD_DATE_FORMAT = "yyyy-MM-dd"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)
        val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val size = html.selectFirst(SIZE)?.ownText()?.uppercase()
        val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()?.let {
            TorrentDateParser.parse(date = it, format = UPLOAD_DATE_FORMAT)
        }
        val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

        TorrentDetails(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            uploadDate = uploadDate,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }
}