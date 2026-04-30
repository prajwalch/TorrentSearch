package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Sukebei : SearchProvider {
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
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl).orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): GetTorrentDetailsResponse {
        val responseHtml = HttpClient.get(detailsPageUrl)

        return SukebeiDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.DetailsNotFound
    }
}

private class SukebeiResultsPageParser(
    private val providerName: String,
    private val providerSpecializedCategory: Category,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent>? =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .selectFirst("table.torrent-list > tbody")
                ?.children()
                ?.mapNotNull { tr -> parseTableRow(tr = tr) }
        }

    private fun parseTableRow(tr: Element): Torrent? {
        val nameAnchorElem = tr.selectFirst("td:nth-child(2)")?.selectFirst("a") ?: return null

        val torrentName = nameAnchorElem.ownText()
        val descriptionPageUrl = nameAnchorElem.attr("abs:href")

        val downloadLinks = tr.selectFirst("td:nth-child(3)") ?: return null
        val fileDownloadLink = downloadLinks.selectFirst("a:nth-child(1)")?.attr("abs:href")
        val magnetUri = downloadLinks
            .selectFirst("a:nth-child(2)")
            ?.attr("href")
            ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        val size = tr.selectFirst("td:nth-child(4)")?.ownText() ?: return null
        val uploadDate = tr
            .selectFirst("td:nth-child(5)")
            ?.attr("data-timestamp")
            ?.let { DateUtils.formatEpochSecond(it.toLong()) }
            ?: return null
        val seeders = tr.selectFirst("td:nth-child(6)")?.ownText() ?: return null
        val peers = tr.selectFirst("td:nth-child(7)")?.ownText() ?: return null

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerName = providerName,
            uploadDate = uploadDate,
            category = providerSpecializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
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
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()
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