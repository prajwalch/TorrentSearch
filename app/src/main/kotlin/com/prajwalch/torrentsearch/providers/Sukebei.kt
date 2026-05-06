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

class Sukebei : SearchProvider, TorrentDetailsProvider {
    override val id = "sukebeinyaa"
    override val name = "Sukebei"
    override val url = "https://sukebei.nyaa.si"
    override val specializedCategory = Category.Porn
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = SukebeiResultsPageParser(
        providerName = name,
        providerSpecializedCategory = specializedCategory,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append("$url/")
            // Filter = No filter (0)
            append("?f=0")
            // Category = All categories (0_0)
            append("&c=0_0")
            append("&q=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return SukebeiDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class SukebeiResultsPageParser(
    private val providerName: String,
    private val providerSpecializedCategory: Category,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(NAME)?.ownText() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        val size = listItem.selectFirst(SIZE)?.ownText()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.attr("data-timestamp")
            ?.toLongOrNull()
            ?.let(TorrentDateParser::epochSecondToInstant)
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders ?: 0u,
            peers = peers ?: 0u,
            providerName = providerName,
            uploadDate = uploadDate,
            category = providerSpecializedCategory,
            descriptionPageUrl = detailsPageUrl ?: "",
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table.torrent-list > tbody > tr"
        private const val NAME = "td:nth-child(2) > a:not(.comments)"
        private const val SIZE = "td:nth-child(4)"
        private const val SEEDERS = "td:nth-child(6)"
        private const val PEERS = "td:nth-child(7)"
        private const val UPLOAD_DATE = "td:nth-child(5)"
        private const val MAGNET_URI = "td:nth-child(3) > a:nth-child(2)"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(3) > a:nth-child(1)"
        private const val DETAILS_PAGE_URL = NAME
    }
}

private object SukebeiDetailsPageParser {
    private const val TORRENT_NAME = "body > div > div:nth-child(7) > div.panel-heading > h3"
    private const val SIZE =
        "body > div > div:nth-child(7) > div.panel-body > div:nth-child(4) > div:nth-child(2)"
    private const val SEEDERS =
        "body > div > div:nth-child(7) > div.panel-body > div:nth-child(2) > div:nth-child(4)"
    private const val PEERS =
        "body > div > div:nth-child(7) > div.panel-body > div:nth-child(3) > div:nth-child(4)"
    private const val UPLOAD_DATE =
        "body > div > div:nth-child(7) > div.panel-body > div:nth-child(1) > div:nth-child(4)"

    //    private const val CATEGORY =
//        "body > div > div:nth-child(7) > div.panel-body > div:nth-child(1) > div:nth-child(2)"
    private const val UPLOADER =
        "body > div > div:nth-child(7) > div.panel-body > div:nth-child(2) > div:nth-child(2)"
    private const val DESCRIPTION = "#torrent-description"
    private const val MAGNET_URI = """a[href^="magnet:"]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="/download"]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val name = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.text()?.trim()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.text()?.trim()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.attr("data-timestamp")
                ?.toLongOrNull()
                ?.let(TorrentDateParser::epochSecondToInstant)
            val uploader = html.selectFirst(UPLOADER)?.text()?.trim()
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val description = html.selectFirst(DESCRIPTION)?.html()

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Porn,
                uploader = uploader,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
            )
        }
}